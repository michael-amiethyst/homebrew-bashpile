package com.bashpile.engine;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bashpile.Asserts;
import com.bashpile.BashpileParser;
import com.bashpile.Strings;
import com.bashpile.engine.bast.ListOfTranslation;
import com.bashpile.engine.bast.Translation;
import com.bashpile.engine.strongtypes.FunctionTypeInfo;
import com.bashpile.engine.strongtypes.ParameterInfo;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.engine.strongtypes.TypeStack;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.TypeError;
import com.google.common.collect.Streams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.bashpile.Asserts.assertTypesCoerce;
import static com.bashpile.engine.BashTranslationHelper.*;
import static com.bashpile.engine.bast.Translation.*;
import static com.bashpile.engine.strongtypes.TranslationMetadata.*;
import static com.bashpile.engine.strongtypes.Type.*;
import static java.util.Objects.requireNonNull;

/**
 * Translates to Bash5 with four spaces as a tab.
 */
public class BashTranslationEngine implements TranslationEngine {

    // static variables

    /** Four spaces */
    public static final String TAB = "    ";

    private static final Map<String, String> binaryPrimaryTranslations = Map.of(
            "===", "==",
            "!==", "!=");

    private static final Logger LOG = LogManager.getLogger(BashTranslationEngine.class);

    // instance variables

    /** This is how we enforce type checking at compile time.  Mutable. */
    private final TypeStack typeStack = new TypeStack();

    /** All the functions hoisted so far, so we can ensure we don't emit them twice */
    private final Set<String> foundForwardDeclarations = new HashSet<>();

    /**
     * When an expression needs a statement inserted before the expression.
     * <p>Replaces preambles.</p>
     */
    private List<Translation> expressionSetups = new ArrayList<>();

    /** The Bashpile script input file or literal text */
    private final @Nonnull String origin;

    /** We need to name the anonymous blocks, anon0, anon1, anon2, etc.  We keep that counter here. */
    private int anonBlockCounter = 0;

    /** Should be set immediately after creation with {@link #setVisitor(BashpileVisitor)} */
    private @Nullable BashpileVisitor visitor;

    private @Nullable BashTranslationEngineDelegate kotlinDelegate;

    // instance methods

    public BashTranslationEngine(@Nonnull final String origin) {
        // escape newlines -- origin may be multi-line script
        this.origin = StringEscapeUtils.escapeJava(origin);
        TypecastUtils.engine = this;
    }

    @Override
    public void setVisitor(@Nonnull final BashpileVisitor visitor) {
        this.visitor = visitor;
        kotlinDelegate = new BashTranslationEngineDelegate(visitor);
    }

    public void addExpressionSetup(@Nonnull final Translation setup) {
        expressionSetups.add(setup);
    }

    @Nonnull
    @Override
    public Translation getExpressionSetup() {
        try {
            return expressionSetups.stream().reduce(Translation::add).orElse(EMPTY_TRANSLATION);
        } finally {
            // the list is drained, reset
            expressionSetups = new ArrayList<>();
        }
    }

    // header translations

    @Override
    public @Nonnull Translation originHeader() {
        final ZonedDateTime now = ZonedDateTime.now();
        return toStringTranslation("""
                #
                # Generated from %s on %s (timestamp %d)
                #
                """.formatted(origin, now, now.toInstant().toEpochMilli()));
    }

    /**
     * Set Bash options for scripts.
     * <br>
     * <ul><li>set -e: exit on failed command</li>
     * <li>set -E: subshells and command substitutions inherit our ERR trap</li>
     * <li>set -u: exit on undefined variable --
     *  we don't need this for Bashpile generated code but there may be `source`d code.</li>
     * <li>set -o pipefail: exit immediately when a command in a pipeline fails.</li>
     * <li>set -o posix: Posix mode -- we need this so that all subshells inherit the -eu options.</li></ul>
     *
     * @see <a href=https://unix.stackexchange.com/a/23099>Q & A</a>
     * @see <a href=http://redsymbol.net/articles/unofficial-bash-strict-mode/>Unofficial Bash Strict Mode</a>
     * @see <a href=https://disconnected.systems/blog/another-bash-strict-mode/>Another Bash Strict Mode</a>
     * @return The Strict Mode header
     */
    @Override
    public @Nonnull Translation strictModeHeader() {
        // we need to declare s to avoid a false positive for a shellcheck warning
        final String strictMode = """
                set -eEuo pipefail -o posix
                declare -x IFS=$'\\n\\t'
                declare -i s
                trap 's=$?; echo "Error (exit code $s) found on line $LINENO.  Command was: $BASH_COMMAND"; exit $s' ERR
                """;
        return toStringTranslation("# strict mode header\n%s".formatted(strictMode));
    }

    // statement translations

    @Override
    public Translation importStatement(BashpileParser.ImportStatementContext ctx) {
        final String libraryName = Strings.unquote(ctx.StringValues().getText());
        // try to source 3 locations, develop, OSX location, Linux location
        // 'source ... || source ...' does not work
        // dot ('.') is more portable than 'source'
        final String trText = """
                declare stdlibPath
                stdlibPath="${BASHPILE_HOME:=.}/target/%s"
                if ! [ -e "${stdlibPath}" ]; then
                  stdlibPath="${BASHPILE_HOME:=.}/%s"
                fi
                if ! [ -e "${stdlibPath}" ]; then
                  stdlibPath="/usr/local/bin/%s"
                fi
                if ! [ -e "${stdlibPath}" ]; then
                  stdlibPath="/opt/homebrew/bin/%s"
                fi
                # To fix shellcheck SC1090
                # shellcheck source=/dev/null
                . "$stdlibPath"
                """.formatted(libraryName, libraryName, libraryName, libraryName);

        final Translation comment = createCommentTranslation("import statement", lineNumber(ctx));
        final Translation importTranslation = new Translation(trText);
        return comment.add(importTranslation);
    }

    @Override
    public Translation whileStatement(BashpileParser.WhileStatementContext ctx) {
        LOG.trace("In whileStatement");
        final Translation comment = createCommentTranslation("while statement", lineNumber(ctx));
        final Translation gate = requireNonNull(visitor).visit(ctx.expression());
        final Translation bodyStatements = ctx.indentedStatements().statement().stream()
                .map(visitor::visit).reduce(Translation::add).orElseThrow().lambdaBodyLines(x -> "    " + x);
        final Translation whileTranslation = Translation.toStringTranslation("""
                while %s; do
                %sdone
                """.formatted(gate.body(), bodyStatements.body()));
        return comment.add(whileTranslation);
    }

    @Override
    public @Nonnull Translation functionForwardDeclarationStatement(
            @Nonnull final BashpileParser.FunctionForwardDeclarationStatementContext ctx) {
        LOG.trace("In functionForwardDeclarationStatement");
        // create translations
        final Translation comment = createCommentTranslation("function forward declaration", lineNumber(ctx));
        final ParserRuleContext functionDeclCtx = getFunctionDeclCtx(requireNonNull(visitor), ctx);
        final Translation hoistedFunction = visitor.visit(functionDeclCtx).lambdaBody(String::stripTrailing);

        // register that this forward declaration has been handled
        foundForwardDeclarations.add(ctx.Id().getText());

        // add translations
        return comment.add(hoistedFunction);
    }

    @Override
    public @Nonnull Translation functionDeclarationStatement(
            @Nonnull final BashpileParser.FunctionDeclarationStatementContext ctx) {
        return requireNonNull(kotlinDelegate).functionDeclarationStatement(ctx, foundForwardDeclarations, typeStack);
    }

    @Override
    public @Nonnull Translation anonymousBlockStatement(
            @Nonnull final BashpileParser.AnonymousBlockStatementContext ctx) {
        LOG.trace("In anonymousBlockStatement");
        try (var ignored2 = typeStack.pushFrame()) {
            final Translation comment = createCommentTranslation("anonymous block", lineNumber(ctx));
            // behind the scenes we need to name the anonymous function
            final String anonymousFunctionName = "anon" + anonBlockCounter++;
            // map of x to x needed for upcasting to parent type
            final Translation blockStatements = visitBodyStatements(
                    ctx.functionBlock().statement(), requireNonNull(visitor));
            // define function and then call immediately with no arguments
            final Translation selfCallingAnonymousFunction = toStringTranslation("%s () {\n%s}; %s\n"
                    .formatted(anonymousFunctionName, blockStatements.body(), anonymousFunctionName));
            return comment.add(selfCallingAnonymousFunction);
        }
    }

    @Override
    public @Nonnull Translation conditionalStatement(BashpileParser.ConditionalStatementContext ctx) {
        LOG.trace("In conditionalStatement");
        // handle initial if
        Translation guard = visitGuardingExpression(requireNonNull(visitor).visit(ctx.expression()));
        Translation ifBlockStatements;
        try (var ignored = typeStack.pushFrame()) {
            ifBlockStatements = visitBodyStatements(ctx.indentedStatements(0).statement(), visitor);
        }

        // handle else ifs
        final AtomicReference<String> elseIfBlock = new AtomicReference<>();
        elseIfBlock.set("");
        ctx.elseIfClauses().forEach(elseIfCtx -> {
            Translation guard2 = visitGuardingExpression(visitor.visit(elseIfCtx.expression()));
            Translation ifBlockStatements2;
            try (var ignored = typeStack.pushFrame()) {
                ifBlockStatements2 = visitBodyStatements(elseIfCtx.indentedStatements().statement(), visitor);
            }
            final String prev = elseIfBlock.get();
            elseIfBlock.set(prev + """

                    elif %s; then
                    %s""".formatted(guard2, ifBlockStatements2).stripTrailing());
        });

        // handle else
        String elseBlock = "";
        if (ctx.Else() != null) {
            Translation elseBlockStatements;
            try (var ignored = typeStack.pushFrame()) {
                final int lastIndex = ctx.indentedStatements().size() - 1;
                elseBlockStatements = visitBodyStatements(ctx.indentedStatements(lastIndex).statement(), visitor);
            }
            elseBlock = """
                    
                    else
                    %s""".formatted(elseBlockStatements).stripTrailing();
        }
        final String ifBlock = ifBlockStatements.body().stripTrailing();
        final String conditional = """
                if %s; then
                %s%s%s
                fi
                """.formatted(guard.body(), ifBlock, elseIfBlock, elseBlock);
        return toStringTranslation(conditional);
    }

    @SuppressWarnings("UnstableApiUsage") // for Streams.zip
    @Override
    public Translation switchStatement(@Nonnull BashpileParser.SwitchStatementContext ctx) {
        LOG.trace("In switchStatement");
        final Translation expressionTranslation = requireNonNull(visitor).visit(ctx.expression(0));
        final Stream<Translation> patterns = ctx.expression().stream().skip(1)
                .map(visitor::visit)
                // change "or" to single pipe for Bash case syntax
                .map(x -> x.lambdaBody(str -> str.replace("||", "|")))
                .map(tr -> {
                    // unquote a catchAll
                    if (tr.body().equals("\"*\"")) {
                        tr = tr.unquoteBody();
                    }
                    return tr;
                });
        final Stream<List<Translation>> statementsLists = ctx.indentedStatements().stream()
                .map(BashpileParser.IndentedStatementsContext::statement)
                .map(x -> x.stream().map(visitor::visit).toList());
        final Translation cases = Streams.zip(patterns, statementsLists, Pair::of)
                .map(BashTranslationHelper::toCase)
                .reduce(Translation::add)
                .orElseThrow();
        final String template = """
                case %s in
                %sesac
                """.formatted(expressionTranslation.body(), cases.body());
        final Translation comment = createCommentTranslation("switch statement", lineNumber(ctx));
        return comment.add(toStringTranslation(template));
    }

    @Override
    public @Nonnull Translation assignmentStatement(@Nonnull final BashpileParser.AssignmentStatementContext ctx) {
        LOG.trace("In assignmentStatement");
        // add this variable for the Left Hand Side to the type map
        final String lhsVariableName = ctx.typedId().Id().getText();
        final int lineNumber = lineNumber(ctx);
        final Type lhsType = getLhsType(ctx);
        typeStack.putVariableType(lhsVariableName, lhsType, lineNumber);

        // visit the Right Hand Side expression
        final boolean rhsExprExists = ctx.expression() != null;
        Translation rhsExprTranslation = UNKNOWN_TRANSLATION;
        if (rhsExprExists) {
            rhsExprTranslation = requireNonNull(visitor).visit(ctx.expression());
            if (rhsExprTranslation.metadata().contains(CONDITIONAL)) {
                rhsExprTranslation = rhsExprTranslation
                        .lambdaBody("$(if %s; then echo true; else echo false; fi)"::formatted)
                        .metadata(INLINE);
            }
            // add quotes if needed
            if (rhsExprTranslation.isStr() && rhsExprTranslation.metadata().contains(NORMAL)) {
                // TODO call quoteBody and have quoteBody escape quotes
                rhsExprTranslation = rhsExprTranslation.lambdaBody(str -> {
                    str = StringUtils.prependIfMissing(str, "\"");
                    return StringUtils.appendIfMissing(str, "\"");
                });
            }
            rhsExprTranslation = rhsExprTranslation.inlineAsNeeded();
        }
        assertTypesCoerce(lhsType, rhsExprTranslation.type(), ctx.typedId().Id().getText(), lineNumber);

        // create translations
        final Translation comment = createCommentTranslation("assign statement", lineNumber);
        // 'readonly' not enforced
        Translation modifiers = visitModifiers(ctx.typedId().modifier());
        final String ctxTypeString = ctx.typedId().complexType().types(0).getText();
        final boolean isList = ctxTypeString.equalsIgnoreCase(LIST_TYPE.mainTypeName().name());
        if (isList) {
            // make the declaration for a Bash non-associative array
            final Translation arrayOption = toStringTranslation("a").metadata(OPTION);
            modifiers = modifiers.add(arrayOption);
        }
        final Translation variableDeclaration =
                toStringTranslation("declare %s %s\n".formatted(modifiers.body(), lhsVariableName));

        final boolean isListAssignment = lhsType.isList() && rhsExprTranslation.isList();
        if (isListAssignment && !rhsExprTranslation.isListOf()) {
            // only quote body when we're assigning a list reference (not 'listOf')
            rhsExprTranslation = rhsExprTranslation.quoteBody().parenthesizeBody().toTrueArray();
        }
        // merge expr into the assignment
        final String assignmentBody = rhsExprExists ? "%s=%s\n".formatted(lhsVariableName, rhsExprTranslation.body()) : "";
        final Translation assignment = toStringTranslation(assignmentBody);

        // order is comment, variable declaration, assignment
        return comment.add(getExpressionSetup()).add(variableDeclaration).add(assignment)
                .type(NA_TYPE).metadata(NORMAL);
    }

    @Override
    public @Nonnull Translation reassignmentStatement(@Nonnull final BashpileParser.ReassignmentStatementContext ctx) {
        LOG.trace("In reassignmentStatement");
        // get name and type
        final String lhsVariableName = ContextUtils.getIdText(ctx);
        final Type lhsExpectedType = typeStack.getVariableType(lhsVariableName);
        if (lhsExpectedType.isNotFound()) {
            throw new TypeError(lhsVariableName + " has not been declared", lineNumber(ctx));
        }

        // get expression and it's type
        Translation rhsExprTranslation;
        rhsExprTranslation = requireNonNull(visitor).visit(ctx.expression());
        if (rhsExprTranslation.metadata().contains(CONDITIONAL)) {
            rhsExprTranslation = rhsExprTranslation
                    .lambdaBody("$(if %s; then echo true; else echo false; fi)"::formatted)
                    .metadata(INLINE);
        }
        rhsExprTranslation = rhsExprTranslation.inlineAsNeeded();
        final Type rhsActualType = rhsExprTranslation.type();
        if (!rhsActualType.isEmpty()) {
            Asserts.assertTypesCoerce(lhsExpectedType, rhsActualType, lhsVariableName, lineNumber(ctx));
        } // TODO import impl - rhs is empty on `source`d function calls

        // create translations
        final Translation comment = createCommentTranslation("reassign statement", lineNumber(ctx));
        final String assignOperator = ctx.assignmentOperator().getText();
        String listAccessor = "";
        if (lhsExpectedType.isList()) {
            final String indexText = ContextUtils.getListAccessorIndexText(ctx);
            if (StringUtils.isBlank(indexText)) {
                // no indexing, so we are adding to a list
                final boolean addingListToList = rhsExprTranslation.isList();
                if (addingListToList && !rhsExprTranslation.isListOf()) {
                    rhsExprTranslation = rhsExprTranslation.quoteBody().parenthesizeBody().toTrueArray();
                } else {
                    rhsExprTranslation = rhsExprTranslation.parenthesizeBody();
                }
            } else /* indexing */ {
                listAccessor = "[%s]".formatted(indexText);
            }
        }
        // merge rhsExprTranslation into reassignment
        final String reassignmentBody = "%s%s%s%s\n"
                .formatted(lhsVariableName, listAccessor, assignOperator, rhsExprTranslation.body());
        final Translation reassignment = toStringTranslation(reassignmentBody);

        return comment.add(reassignment).assertParagraphBody().type(NA_TYPE).metadata(NORMAL);
    }

    @Override
    public @Nonnull Translation printStatement(@Nonnull final BashpileParser.PrintStatementContext ctx) {
        return requireNonNull(kotlinDelegate).printStatement(ctx);
    }

    @Override
    public @Nonnull Translation expressionStatement(@Nonnull final BashpileParser.ExpressionStatementContext ctx) {
        LOG.trace("In expressionStatement");
        Translation expr = requireNonNull(visitor).visit(ctx.expression());
        // change $(( )) to _=$(( )) to avoid executing a number.  Fixes ShellCheck error SC2084
        expr = expr.lambdaBody(body -> !body.startsWith("$((") ? body : "_=" + body).add(NEWLINE);
        final Translation comment = createCommentTranslation("expression statement", lineNumber(ctx));
        return comment.add(expr).type(expr.type()).metadata(expr.metadata());
    }

    @Override
    public @Nonnull Translation returnPsudoStatement(@Nonnull final BashpileParser.ReturnPsudoStatementContext ctx) {
        return requireNonNull(kotlinDelegate).returnPsudoStatement(ctx, typeStack);
    }

    // expressions

    @Override
    public Translation unaryPostCrementExpression(BashpileParser.UnaryPostCrementExpressionContext ctx) {
        final Translation expressionTranslation = requireNonNull(visitor).visit(ctx.expression());
        final String opText = ctx.op.getText();
        final int lineNumber = ctx.start.getLine();
        if (expressionTranslation.isInt()) {
            // arithmetic built-in when possible
            final String body = "$((%s%s))".formatted(expressionTranslation.removeVariableBrackets(), opText);
            return new Translation(body, INT_TYPE, List.of(CALCULATION));
        } else if (expressionTranslation.isNumeric()) {
            // bc tool can't assign to shell variables, only bc variables.
            // bc variables can't have uppercase, and to "export" them back to the shell we would need a whole
            // concept of a post-amble and who uses ++ on floats anyway???
            final String message = "++/-- operators only allowed on explicit ints.  Try casting with ': int'.";
            throw new TypeError(message, lineNumber);
        } else {
            throw new TypeError("Post increment and post decrement only allowed on numeric vars", lineNumber);
        }
    }

    /**
     * True/False cast to 1/0.  Any number besides 0 casts to true.  "true" and "false" (any case) cast to BOOLs.
     * Quoted numbers (numbers in STRs) cast to BOOLs like numbers.  Anything cast to an STR gets quotes around it.
     */
    @Override
    public @Nonnull Translation typecastExpression(BashpileParser.TypecastExpressionContext ctx) {
        LOG.trace("In typecastExpression");
        final Type castTo = Type.valueOf(ctx.complexType());
        Translation expression = requireNonNull(visitor).visit(ctx.expression());
        final int lineNumber = lineNumber(ctx);
        final TypeError typecastError = new TypeError(
                "Casting %s to %s is not supported".formatted(expression.type(), castTo), lineNumber);
        switch (expression.type().mainTypeName()) {
            case EMPTY -> expression = expression.type(castTo); // usually for `source`d method returns
            case BOOL -> expression = TypecastUtils.typecastFromBool(expression, castTo, typecastError);
            case NUMBER -> expression = TypecastUtils.typecastFromNumber(expression, castTo, lineNumber, typecastError);
            case INT -> expression = TypecastUtils.typecastFromInt(expression, castTo, lineNumber, typecastError);
            case FLOAT -> expression = TypecastUtils.typecastFromFloat(expression, castTo, lineNumber, typecastError);
            case STR -> expression = TypecastUtils.typecastFromStr(expression, castTo, lineNumber, typecastError);
            case LIST -> expression = TypecastUtils.typecastFromList(expression, castTo, typecastError);
            case UNKNOWN ->
                    expression = TypecastUtils.typecastFromUnknown(expression, castTo, lineNumber, typecastError);
            case NOT_FOUND -> {
                // for specifying a type for a variable assigned by a command, e.g. getopts creates OPTARG
            }
            default -> throw typecastError;
        }
        expression = expression.type(castTo);
        return expression;
    }

    /**
     * {@inheritDoc}
     * @see BashTranslationEngineDelegate#functionDeclarationStatement(BashpileParser.FunctionDeclarationStatementContext, Set, TypeStack)
     */
    @Override
    public @Nonnull Translation functionCallExpression(
            @Nonnull final BashpileParser.FunctionCallExpressionContext ctx) {
        LOG.trace("In functionCallExpression");
        final String functionName = ctx.Id().getText();

        // check arg types

        // get non-defaulted argumentTranslations
        List<Translation> argumentTranslationsList = ctx.argumentList() != null
                ? ctx.argumentList().expression().stream()
                        .map(requireNonNull(visitor)::visit)
                        .map(Translation::inlineAsNeeded)
                        .toList()
                : List.of();
        argumentTranslationsList = new ArrayList<>(argumentTranslationsList); // make mutable

        // add defaulted arguments
        final List<ParameterInfo> parameterInfos = typeStack.getFunctionTypes(functionName).parameterInfos();
        final int firstDefaultedIndex = Math.min(argumentTranslationsList.size(), parameterInfos.size());
        final List<ParameterInfo> neededDefaults = parameterInfos.subList(firstDefaultedIndex, parameterInfos.size());
        for (ParameterInfo info : neededDefaults) {
            argumentTranslationsList.add(new Translation(info.defaultValue(), info.type(), NORMAL));
        }

        // check types
        final FunctionTypeInfo expectedTypes = typeStack.getFunctionTypes(functionName);
        final List<Type> actualTypes = argumentTranslationsList.stream().map(Translation::type).toList();
        if (!expectedTypes.isEmpty()) {
            Asserts.assertTypesCoerce(expectedTypes.parameterTypes(), actualTypes, functionName, lineNumber(ctx));
        } // TODO "imports" impl - ensure expectedTypes interfaces with the planned import system

        // collapse argumentTranslationsList to a single translation
        Translation argumentTranslations = UNKNOWN_TRANSLATION;
        if (!argumentTranslationsList.isEmpty()) {
            argumentTranslations = toStringTranslation(" ").add(argumentTranslationsList.stream()
                    // only add quotes if needed
                    .map(tr -> !(tr.body().startsWith("\"") && tr.body().endsWith("\"")) ? tr.quoteBody() : tr)
                    .reduce((left, right) -> new Translation(
                            left.body() + " " + right.body(),
                            right.type(),
                            right.metadata()))
                    .orElseThrow());
        }

        // lookup return type of this function
        final Type retType = expectedTypes.returnType();

        Translation ret = new Translation(functionName + argumentTranslations.body(), retType, List.of(NORMAL));
        // suppress output if we are printing to output as part of a work-around to return a string
        // this covers the case of calling a function without using the return
        if (retType.isStr()) {
            ret = ret.lambdaBody("%s >/dev/null"::formatted);
        }
        ret = ret.metadata(NEEDS_INLINING_OFTEN);
        return ret;
    }

    @Override
    public @Nonnull Translation parenthesisExpression(@Nonnull final BashpileParser.ParenthesisExpressionContext ctx) {
        return requireNonNull(kotlinDelegate).parenthesisExpression(ctx);
    }

    @Override
    public @Nonnull Translation calculationExpression(@Nonnull final BashpileParser.CalculationExpressionContext ctx) {
        return requireNonNull(kotlinDelegate).calculationExpression(ctx);
    }

    @Override
    public @Nonnull Translation unaryPrimaryExpression(BashpileParser.UnaryPrimaryExpressionContext ctx) {
        return requireNonNull(kotlinDelegate).unaryPrimaryExpression(ctx);
    }

    @Override
    public @Nonnull Translation binaryPrimaryExpression(BashpileParser.BinaryPrimaryExpressionContext ctx) {
        LOG.trace("In binaryPrimaryExpression");
        Asserts.assertEquals(3, ctx.getChildCount(), "Should be 3 parts");
        String primary = ctx.binaryPrimary().getText();
        final Translation firstTranslation =
                requireNonNull(visitor).visit(ctx.getChild(0)).inlineAsNeeded();
        final Translation secondTranslation =
                visitor.visit(ctx.getChild(2)).inlineAsNeeded();

        // we do some checks for strict equals and strict not equals
        final boolean noTypeMatch = !(firstTranslation.type().equals(secondTranslation.type()));
        if (ctx.binaryPrimary().IsStrictlyEqual() != null && noTypeMatch) {
            return toStringTranslation("false");
        } else if (ctx.binaryPrimary().InNotStrictlyEqual() != null && noTypeMatch) {
            return toStringTranslation("true");
        } // else make a non-trivial string or numeric primary

        String body;
        primary = binaryPrimaryTranslations.getOrDefault(primary, primary);
        String not = "";
        final boolean numeric = firstTranslation.type().isNumeric() && secondTranslation.type().isNumeric();
        if (numeric) {
            // use bc to handle floats and avoid silly Bash operators (e.g. `-eq`) entirely
            body = "[ \"$(bc <<< \"%s%s %s %s\")\" -eq 1 ]";
        } else {
            // string
            body = "[ %s\"%s\" %s \"%s\" ]";
            // <= and >= not supported, so need to do ! > and ! <
            // all < and > must be escaped, so they will not be interpreted as redirects
            switch (primary) {
                case "<=" -> {
                    not = "! ";
                    primary = "\\>";
                }
                case ">=" -> {
                    not = "! ";
                    primary = "\\<";
                }
                case "<", ">" -> primary = "\\" + primary;
            }
        }
        body = body.formatted(not, firstTranslation.unquoteBody().body(), primary,
                secondTranslation.unquoteBody().body());
        return new Translation(body, BOOL_TYPE, CONDITIONAL);
    }

    @Override
    public Translation combiningExpression(BashpileParser.CombiningExpressionContext ctx) {
        return requireNonNull(kotlinDelegate).combiningExpression(ctx);
    }

    @Override
    public Translation argumentsBuiltinExpression(BashpileParser.ArgumentsBuiltinExpressionContext ctx) {
        LOG.trace("In argumentsBuiltinExpression");
        if (ctx.argumentsBuiltin().NumberValues() != null) {
            return toStringTranslation("$" + ctx.argumentsBuiltin().NumberValues().getText());
        } else {
            // arguments[all]
            return toStringTranslation("$@");
        }
    }

    @Override
    public Translation listOfBuiltinExpression(BashpileParser.ListOfBuiltinExpressionContext ctx) {
        LOG.trace("In listOfBuiltinExpression");
        // guard, empty list
        if (ctx.expression().isEmpty()) {
            return new ListOfTranslation(UNKNOWN_TYPE);
        }

        // guard, checks types
        final List<Pair<Integer, Translation>> translations = IntStream.range(0, ctx.expression().size())
                .mapToObj(it -> Pair.of(it, requireNonNull(visitor).visit(ctx.expression(it)))).toList();
        final Type listType = translations.get(0).getRight().type();
        final AtomicReference<List<BashpileUncheckedException>> errors = new AtomicReference<>();
        errors.set(List.of());
        translations.stream().skip(1).forEachOrdered(tr -> {
            if (!tr.getRight().type().equals(listType)) {
                String message = "Bad type %s in %s list at index %s"
                        .formatted(tr.getRight().type(), listType, tr.getLeft());
                errors.get().add(new BashpileUncheckedException(message));
            }
        });
        if (!errors.get().isEmpty()) {
            throw errors.get().get(0);
        }

        // body
        return ListOfTranslation.of(translations.stream().map(Pair::getRight).toList());
    }

    @Override
    public @Nonnull Translation idExpression(BashpileParser.IdExpressionContext ctx) {
        LOG.trace("In idExpression");
        final String variableName = ctx.Id().getText();
        final Type type = typeStack.getVariableType(variableName);
        // list syntax is `listName[*]`
        // see https://www.masteringunixshell.net/qa35/bash-how-to-print-array.html
        final String allIndexes = type.isBasic() ? "" : /* list */ "[*]";
        // use ${var} syntax instead of $var for string concatenations, e.g. `${var}someText`
        return new Translation("${%s%s}".formatted(ctx.getText(), allIndexes), type, NORMAL);
    }

    @Override
    public Translation listIndexExpression(BashpileParser.ListAccessExpressionContext ctx) {
        LOG.trace("In listIndexExpression");
        final String variableName = ContextUtils.getIdText(ctx);
        final Type type = typeStack.getVariableType(requireNonNull(variableName));
        // use ${var} syntax instead of $var for string concatenations, e.g. `${var}someText`
        Integer index = ContextUtils.getListAccessorIndex(ctx);
        if (index != null) {
            return new Translation(
                    "${%s[%d]}".formatted(variableName, index), type.asContentsType().orElseThrow(), NORMAL);
        } else {
            return new Translation("${%s[@]}".formatted(variableName), type.asContentsType().orElseThrow(), NORMAL);
        }

    }

    // expression helper rules

    @Override
    public @Nonnull Translation shellString(@Nonnull final BashpileParser.ShellStringContext ctx) {
        LOG.trace("In shellString helper");
        Translation contentsTranslation = ctx.shellStringContents().stream()
                .map(requireNonNull(visitor)::visit)
                .map(Translation::inlineAsNeeded)
                .reduce(Translation::add)
                .map(x -> x.lambdaBody(Strings::dedent))
                .map(BashTranslationHelper::joinEscapedNewlines)
                .orElseThrow()
                .type(UNKNOWN_TYPE);

        // a subshell does NOT need inlining often, see conditionalStatement
        if (!Strings.inParentheses(contentsTranslation.body())) {
            contentsTranslation = contentsTranslation.metadata(NEEDS_INLINING_OFTEN);
        }

        return contentsTranslation.unescapeBody();
    }
}
