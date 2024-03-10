package com.bashpile.engine;

import com.bashpile.Asserts;
import com.bashpile.BashpileParser;
import com.bashpile.Strings;
import com.bashpile.engine.strongtypes.FunctionTypeInfo;
import com.bashpile.engine.strongtypes.SimpleType;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.engine.strongtypes.TypeStack;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import com.google.common.collect.Streams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.bashpile.Asserts.*;
import static com.bashpile.Strings.lambdaLastLine;
import static com.bashpile.engine.BashTranslationHelper.*;
import static com.bashpile.engine.Translation.*;
import static com.bashpile.engine.strongtypes.SimpleType.LIST;
import static com.bashpile.engine.strongtypes.TranslationMetadata.*;
import static com.bashpile.engine.strongtypes.Type.*;
import static com.google.common.collect.Iterables.getLast;

/**
 * Translates to Bash5 with four spaces as a tab.
 */
public class BashTranslationEngine implements TranslationEngine {

    // static variables

    /** Four spaces */
    public static final String TAB = "    ";

    private static final Map<String, String> primaryTranslations = Map.of(
            "unset", "-z",
            "isset", "-n",
            "isEmpty", "-z",
            "isNotEmpty", "-n",
            "fileExists", "-e",
            "===", "==",
            "!==", "!=");

    // instance variables

    /** This is how we enforce type checking at compile time.  Mutable. */
    private final TypeStack typeStack = new TypeStack();

    /** All the functions hoisted so far, so we can ensure we don't emit them twice */
    private final Set<String> foundForwardDeclarations = new HashSet<>();

    /** The current create statement filenames for using in a trap command */
    private final Stack<String> createFilenamesStack = new Stack<>();

    /** The Bashpile script input file or literal text */
    private final @Nonnull String origin;

    /** We need to name the anonymous blocks, anon0, anon1, anon2, etc.  We keep that counter here. */
    private int anonBlockCounter = 0;

    /** Should be set immediately after creation with {@link #setVisitor(BashpileVisitor)} */
    private BashpileVisitor visitor;

    // instance methods

    public BashTranslationEngine(@Nonnull final String origin) {
        // escape newlines -- origin may be multi-line script
        this.origin = StringEscapeUtils.escapeJava(origin);
    }

    @Override
    public void setVisitor(@Nonnull final BashpileVisitor visitor) {
        this.visitor = visitor;
    }

    // header translations

    @Override
    public @Nonnull Translation originHeader() {
        final ZonedDateTime now = ZonedDateTime.now();
        return toParagraphTranslation("""
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
        return toParagraphTranslation("# strict mode header\n%s".formatted(strictMode));
    }

    @Override
    public @Nonnull Translation importsHeaders() {
        // stub
        String text = "";
        return toParagraphTranslation(text);
    }

    // statement translations

    /**
     * See "Setting Traps" and "Race Conditions" at
     * <a href="https://www.davidpashley.com/articles/writing-robust-shell-scripts/">Writing Robust Shell Scripts</a>
     */
    @Override
    public @Nonnull Translation createsStatement(BashpileParser.CreatesStatementContext ctx) {
        final boolean fileNameIsId = ctx.String() == null;

        // create child translations and other variables
        Translation shellString;
        shellString = visitor.visit(ctx.shellString());
        final TerminalNode filenameNode = fileNameIsId ? ctx.Id() : ctx.String();
        String filename =  visitor.visit(filenameNode).unquoteBody().body();
        // convert ID to "$ID"
        filename = fileNameIsId ? "\"$%s\"".formatted(filename) : filename;

        // create our final translation and pop the stack
        createFilenamesStack.push(filename);
        try (var ignored = typeStack.pushFrame()){
            // create other translations
            final Translation comment = createCommentTranslation("creates statement", lineNumber(ctx));
            final Translation subcomment =
                    subcommentTranslationOrDefault(shellString.hasPreamble(), "creates statement body");

            // create a large if-else block with traps
            final String body = getBodyStringForCreatesStatement(ctx, shellString, filename, visitor, createFilenamesStack);
            final Translation bodyTranslation = toParagraphTranslation(body);

            // merge translations and preambles
            return comment.add(subcomment.add(bodyTranslation).mergePreamble());
        } finally {
            createFilenamesStack.pop();
        }
    }

    @Override
    public Translation whileStatement(BashpileParser.WhileStatementContext ctx) {
        final Translation comment = createCommentTranslation("while statement", lineNumber(ctx));
        final Translation gate = visitor.visit(ctx.expression());
        final Translation bodyStatements = ctx.indentedStatements().statement().stream()
                .map(visitor::visit).reduce(Translation::add).orElseThrow().lambdaBodyLines(x -> "    " + x);
        final Translation whileTranslation = Translation.toParagraphTranslation("""
                while %s; do
                %sdone
                """.formatted(gate.body(), bodyStatements.body()))
                .addPreamble(gate.preamble()).addPreamble(bodyStatements.preamble());
        return comment.add(whileTranslation);
    }

    @Override
    public @Nonnull Translation functionForwardDeclarationStatement(
            @Nonnull final BashpileParser.FunctionForwardDeclarationStatementContext ctx) {
        // create translations
        final Translation comment = createCommentTranslation("function forward declaration", lineNumber(ctx));
        final ParserRuleContext functionDeclCtx = getFunctionDeclCtx(visitor, ctx);
        final Translation hoistedFunction = visitor.visit(functionDeclCtx).lambdaBody(String::stripTrailing);

        // register that this forward declaration has been handled
        foundForwardDeclarations.add(ctx.Id().getText());

        // add translations
        return comment.add(hoistedFunction.assertEmptyPreamble());
    }

    @Override
    public @Nonnull Translation functionDeclarationStatement(
            @Nonnull final BashpileParser.FunctionDeclarationStatementContext ctx) {
        // avoid translating twice if was part of a forward declaration
        final String functionName = ctx.Id().getText();
        if (foundForwardDeclarations.contains(functionName)) {
            return UNKNOWN_TRANSLATION;
        }

        // check for double declaration
        if (typeStack.containsFunction(functionName)) {
            throw new UserError(
                    functionName + " was declared twice (function overloading is not supported)", lineNumber(ctx));
        }

        // regular processing -- no forward declaration

        // register function param types and return type
        final List<Type> typeList = ctx.paramaters().typedId()
                .stream().map(SimpleType::valueOf).map(Type::of).collect(Collectors.toList());
        final Type retType = ctx.type() != null ? Type.valueOf(ctx.type()) : NA_TYPE;
        typeStack.putFunctionTypes(functionName, new FunctionTypeInfo(typeList, retType));

        try (var ignored2 = typeStack.pushFrame()) {

            // register local variable types
            ctx.paramaters().typedId().forEach(x -> {
                final Type mainType = Type.valueOf(x.type());
                typeStack.putVariableType(x.Id().getText(), mainType, lineNumber(ctx));
            });

            // create Translations
            final Translation comment = createCommentTranslation("function declaration", lineNumber(ctx));
            final AtomicInteger i = new AtomicInteger(1);
            // the empty string or ...
            String namedParams = "";
            if (!ctx.paramaters().typedId().isEmpty()) {
                // local var1=$1; local var2=$2; etc
                final String paramDeclarations = ctx.paramaters().typedId().stream()
                        .map(BashpileParser.TypedIdContext::Id)
                        .map(visitor::visit)
                        .map(x -> {
                            String opts = "-r";
                            if (x.type().equals(INT_TYPE)) {
                                opts += "i";
                            }
                            return "declare %s %s=$%s;".formatted(opts, x.body(), i.getAndIncrement());
                        })
                        .collect(Collectors.joining(" "));
                namedParams = TAB + paramDeclarations + "\n";
            }
            final Translation blockStatements = streamContexts(
                    ctx.functionBlock().statement(), ctx.functionBlock().returnPsudoStatement())
                    .map(visitor::visit)
                    .map(tr -> tr.lambdaBodyLines(str -> TAB + str))
                    .reduce(Translation::add)
                    .orElseThrow()
                    .assertEmptyPreamble();
            final Translation functionDeclaration = toParagraphTranslation("%s () {\n%s%s}\n"
                    .formatted(functionName, assertIsLine(namedParams), assertIsParagraph(blockStatements.body())));
            return comment.add(functionDeclaration);
        }
    }

    @Override
    public @Nonnull Translation anonymousBlockStatement(
            @Nonnull final BashpileParser.AnonymousBlockStatementContext ctx) {
        try (var ignored2 = typeStack.pushFrame()) {
            final Translation comment = createCommentTranslation("anonymous block", lineNumber(ctx));
            // behind the scenes we need to name the anonymous function
            final String anonymousFunctionName = "anon" + anonBlockCounter++;
            // map of x to x needed for upcasting to parent type
            final Translation blockStatements = visitBodyStatements(ctx.functionBlock().statement(), visitor);
            // define function and then call immediately with no arguments
            final Translation selfCallingAnonymousFunction = toParagraphTranslation("%s () {\n%s}; %s\n"
                    .formatted(anonymousFunctionName, blockStatements.body(), anonymousFunctionName));
            return comment.add(selfCallingAnonymousFunction);
        }
    }

    @Override
    public @Nonnull Translation conditionalStatement(BashpileParser.ConditionalStatementContext ctx) {
        // handle initial if
        Translation guard = visitGuardingExpression(ctx.Not(), visitor.visit(ctx.expression()));
        Translation ifBlockStatements;
        try (var ignored = typeStack.pushFrame()) {
            ifBlockStatements = visitBodyStatements(ctx.indentedStatements(0).statement(), visitor);
        }

        // handle else ifs
        final AtomicReference<String> elseIfBlock = new AtomicReference<>();
        elseIfBlock.set("");
        ctx.elseIfClauses().forEach(elseIfCtx -> {
            Translation guard2 = visitGuardingExpression(elseIfCtx.Not(), visitor.visit(elseIfCtx.expression()));
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
        final String ifBlock = ifBlockStatements.mergePreamble().body().stripTrailing();
        final String conditional = """
                if %s; then
                %s%s%s
                fi
                """.formatted(guard.body(), ifBlock, elseIfBlock, elseBlock);
        return toParagraphTranslation(guard.preamble() + conditional);
    }

    @Override
    public Translation switchStatement(BashpileParser.SwitchStatementContext ctx) {
        final Translation expressionTranslation = visitor.visit(ctx.expression(0));
        final Stream<Translation> patterns = ctx.expression().stream().skip(1)
                .map(visitor::visit)
                // change "or" to single pipe for Bash case syntax
                .map(x -> x.lambdaBody(str -> str.replace("||", "|")))
                .map(tr -> {
                    // unquote a catchAll
                    // TODO replace with regex when it's implemented
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
        return comment.add(toParagraphTranslation(template))
                .addPreamble(expressionTranslation.preamble()).addPreamble(cases.preamble());
    }

    @Override
    public @Nonnull Translation assignmentStatement(@Nonnull final BashpileParser.AssignmentStatementContext ctx) {
        // add this variable for the Left Hand Side to the type map
        final String lhsVariableName = ctx.typedId().Id().getText();
        final Type lhsType = Type.valueOf(ctx.typedId());
        typeStack.putVariableType(lhsVariableName, lhsType, lineNumber(ctx));

        // visit the Right Hand Side expression
        final boolean rhsExprExists = ctx.expression() != null;
        Translation rhsExprTranslation = UNKNOWN_TRANSLATION;
        if (rhsExprExists) {
            rhsExprTranslation = visitor.visit(ctx.expression());
            if (rhsExprTranslation.metadata().contains(CONDITIONAL)) {
                rhsExprTranslation = rhsExprTranslation
                        .lambdaBody("$(if %s; then echo true; else echo false; fi)"::formatted)
                        .metadata(INLINE);
            }
            rhsExprTranslation = rhsExprTranslation.inlineAsNeeded(BashTranslationHelper::unwindNested);
            rhsExprTranslation = unwindNested(rhsExprTranslation);
        }
        assertTypesCoerce(lhsType, rhsExprTranslation.type(), ctx.typedId().Id().getText(), lineNumber(ctx));

        // create translations
        final Translation comment = createCommentTranslation("assign statement", lineNumber(ctx));
        final Translation subcomment =
                subcommentTranslationOrDefault(rhsExprTranslation.hasPreamble(), "assign statement body");
        // 'readonly' not enforced
        Translation modifiers = visitModifiers(ctx.typedId().modifier());
        final boolean isList = ctx.typedId().type().Type(0).getText().toUpperCase().equals(LIST.name());
        if (isList) {
            modifiers = modifiers.body().isEmpty() ? toStringTranslation(" ") : modifiers;
            // make the declaration for a Bash non-associative array
            modifiers = modifiers.addOption("a");
        }
        final Translation variableDeclaration =
                toLineTranslation("declare %s%s\n".formatted(modifiers.body(), lhsVariableName));

        final boolean isListAssignment = lhsType.isList() && rhsExprTranslation.isList();
        if (isListAssignment && !rhsExprTranslation.isListOf()) {
            // only quote body when we're assigning a list reference (not 'listOf')
            rhsExprTranslation = rhsExprTranslation.quoteBody().parenthesizeBody().toTrueArray();
        }
        // merge expr into the assignment
        final String assignmentBody = rhsExprExists ? "%s=%s\n".formatted(lhsVariableName, rhsExprTranslation.body()) : "";
        final Translation assignment =
                toParagraphTranslation(assignmentBody).addPreamble(rhsExprTranslation.preamble());

        // order is comment, preamble, subcomment, variable declaration, assignment
        final Translation subcommentToAssignment = subcomment.add(variableDeclaration).add(assignment);
        return comment.add(subcommentToAssignment.mergePreamble()).type(NA_TYPE).metadata(NORMAL);
    }

    @Override
    public @Nonnull Translation reassignmentStatement(@Nonnull final BashpileParser.ReassignmentStatementContext ctx) {
        // get name and type
        final String lhsVariableName = ContextUtils.getIdText(ctx);
        final Type lhsExpectedType = typeStack.getVariableType(lhsVariableName);
        if (lhsExpectedType.equals(NOT_FOUND_TYPE)) {
            throw new TypeError(lhsVariableName + " has not been declared", lineNumber(ctx));
        }

        // get expression and it's type
        Translation rhsExprTranslation;
        rhsExprTranslation = visitor.visit(ctx.expression());
        if (rhsExprTranslation.metadata().contains(CONDITIONAL)) {
            rhsExprTranslation = rhsExprTranslation
                    .lambdaBody("$(if %s; then echo true; else echo false; fi)"::formatted)
                    .metadata(INLINE);
        }
        rhsExprTranslation = rhsExprTranslation.inlineAsNeeded(BashTranslationHelper::unwindNested);
        rhsExprTranslation = unwindNested(rhsExprTranslation);
        final Type rhsActualType = rhsExprTranslation.type();
        Asserts.assertTypesCoerce(lhsExpectedType, rhsActualType, lhsVariableName, lineNumber(ctx));

        // create translations
        final Translation comment = createCommentTranslation("reassign statement", lineNumber(ctx));
        final Translation subcomment =
                subcommentTranslationOrDefault(rhsExprTranslation.hasPreamble(), "reassignment statement body");
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
        final Translation reassignment =
                toLineTranslation(reassignmentBody).addPreamble(rhsExprTranslation.preamble());

        // order is: comment, preamble, subcomment, reassignment
        final Translation preambleToReassignment = subcomment.add(reassignment).mergePreamble();
        return comment.add(preambleToReassignment).assertParagraphBody().type(NA_TYPE).metadata(NORMAL);
    }

    @Override
    public @Nonnull Translation printStatement(@Nonnull final BashpileParser.PrintStatementContext ctx) {
        // guard
        final BashpileParser.ArgumentListContext argList = ctx.argumentList();
        if (argList == null) {
            return toLineTranslation("printf \"\\n\"\n");
        }

        // body
        final Translation comment = createCommentTranslation("print statement", lineNumber(ctx));
        final Translation arguments = argList.expression().stream()
                .map(visitor::visit)
                .map(tr -> tr.inlineAsNeeded(BashTranslationHelper::unwindNested))
                .map(BashTranslationHelper::unwindNested)
                .map(tr -> {
                    if (tr.isBasicType()) {
                        return tr.body("""
                                printf "%s\\n"
                                """.formatted(tr.unquoteBody().body()));
                    } else {
                        // list.  Change the Internal Field Separator to a space just for this subshell (parens)
                        return tr.body("""
                                (declare -x IFS=$' '; printf "%%s\\n" "%s")
                                """.formatted(tr.unquoteBody().body()));
                    }
                })
                .reduce(Translation::add)
                .orElseThrow();
        final Translation subcomment =
                subcommentTranslationOrDefault(arguments.hasPreamble(), "print statement body");
        return comment.add(subcomment.add(arguments).mergePreamble());
    }

    @Override
    public @Nonnull Translation expressionStatement(@Nonnull final BashpileParser.ExpressionStatementContext ctx) {
        final Translation expr = visitor.visit(ctx.expression()).add(NEWLINE);
        final Translation comment = createCommentTranslation("expression statement", lineNumber(ctx));
        final Translation subcomment =
                subcommentTranslationOrDefault(expr.hasPreamble(), "expression statement body");
        // order is: comment, preamble, subcomment, expr
        final Translation exprStatement = subcomment.add(expr).mergePreamble();
        return comment.add(exprStatement).type(expr.type()).metadata(expr.metadata());
    }

    @Override
    public @Nonnull Translation returnPsudoStatement(@Nonnull final BashpileParser.ReturnPsudoStatementContext ctx) {
        final boolean exprExists = ctx.expression() != null;

        // check return matches with function declaration
        final BashpileParser.FunctionDeclarationStatementContext enclosingFunction =
                (BashpileParser.FunctionDeclarationStatementContext) ctx.parent.parent;
        final String functionName = enclosingFunction.Id().getText();
        final FunctionTypeInfo functionTypes = typeStack.getFunctionTypes(functionName);
        Translation exprTranslation =
                exprExists ? visitor.visit(ctx.expression()) : EMPTY_TRANSLATION;
        assertTypesCoerce(functionTypes.returnType(), exprTranslation.type(), functionName, lineNumber(ctx));

        if (!exprExists) {
            return UNKNOWN_TRANSLATION;
        }

        final Translation comment = createCommentTranslation("return statement", lineNumber(ctx));
        final Function<String, String> returnLineLambda = str -> {
            if (functionTypes.returnType().equals(STR_TYPE)
                    || ctx.expression() instanceof BashpileParser.NumberExpressionContext) {
                return "printf \"%s\"\n".formatted(Strings.unquote(str));
            } // else
            return str + "\n";
        };
        exprTranslation = exprTranslation.body(lambdaLastLine(exprTranslation.body(), returnLineLambda));
        return comment.add(exprTranslation.mergePreamble());
    }

    // expressions

    /**
     * True/False cast to 1/0.  Any number besides 0 casts to true.  "true" and "false" (any case) cast to BOOLs.
     * Quoted numbers (numbers in STRs) cast to BOOLs like numbers.  Anything cast to an STR gets quotes around it.
     */
    @Override
    public @Nonnull Translation typecastExpression(BashpileParser.TypecastExpressionContext ctx) {
        final Type castTo = Type.valueOf(ctx.type());
        Translation expression = visitor.visit(ctx.expression());
        final int lineNumber = lineNumber(ctx);
        final TypeError typecastError = new TypeError(
                "Casting %s to %s is not supported".formatted(expression.type(), castTo), lineNumber);
        switch (expression.type().mainType()) {
            case BOOL -> expression = typecastToBool(castTo.mainType(), expression, typecastError);
            case INT -> expression = typecastToInt(castTo.mainType(), expression, lineNumber, typecastError);
            case FLOAT -> expression = typecastToFloat(castTo.mainType(), expression, lineNumber, typecastError);
            case STR -> expression = typecastStr(castTo.mainType(), expression, lineNumber, typecastError);
            case LIST -> expression = typecastToList(castTo, expression, typecastError);
            case UNKNOWN -> typecastToUnknown(castTo.mainType(), typecastError);
            case NOT_FOUND -> {
                // for specifying a type for a variable assigned by a command, e.g. getopts creates OPTARG
            }
            default -> throw typecastError;
        }
        expression = expression.type(castTo);
        return expression;
    }

    @Override
    public @Nonnull Translation functionCallExpression(
            @Nonnull final BashpileParser.FunctionCallExpressionContext ctx) {
        final String id = ctx.Id().getText();

        // check arg types

        // get functionName and the argumentTranslations
        final boolean hasArgs = ctx.argumentList() != null;
        final List<Translation> argumentTranslationsList = hasArgs
                ? ctx.argumentList().expression().stream()
                        .map(visitor::visit)
                        .map(tr -> tr.inlineAsNeeded(BashTranslationHelper::unwindNested))
                        .map(BashTranslationHelper::unwindNested)
                        .toList()
                : List.of();

        // check types
        final FunctionTypeInfo expectedTypes = typeStack.getFunctionTypes(id);
        final List<Type> actualTypes = argumentTranslationsList.stream().map(Translation::type).toList();
        Asserts.assertTypesCoerce(expectedTypes.parameterTypes(), actualTypes, id, lineNumber(ctx));

        // extract argText and preambles from argumentTranslations
        // empty list or ' arg1Text arg2Text etc.'
        Translation argumentTranslations = UNKNOWN_TRANSLATION;
        if (hasArgs) {
            argumentTranslations = toStringTranslation(" ").add(argumentTranslationsList.stream()
                    .map(Translation::quoteBody)
                    .reduce((left, right) -> new Translation(
                            left.preamble() + right.preamble(),
                            left.body() + " " + right.body(),
                            right.type(),
                            right.metadata()))
                    .orElseThrow());
        }

        // lookup return type of this function
        final Type retType = expectedTypes.returnType();

        Translation ret = new Translation(
                argumentTranslations.preamble(), id + argumentTranslations.body(), retType, List.of(NORMAL));
        // suppress output if we are printing to output as part of a work-around to return a string
        // this covers the case of calling a function without using the return
        if (retType.equals(STR_TYPE)) {
            ret = ret.lambdaBody("%s >/dev/null"::formatted);
        }
        ret = ret.metadata(NEEDS_INLINING_OFTEN).mergePreamble();
        return ret;
    }

    @Override
    public @Nonnull Translation parenthesisExpression(@Nonnull final BashpileParser.ParenthesisExpressionContext ctx) {
        // drop parenthesis
        Translation ret = visitor.visit(ctx.expression());

        // only keep parenthesis for necessary operations (e.g. "(((5)))" becomes "5" outside of a calc)
        if (ret.type().isPossiblyNumeric() && inCalc(ctx)) {
            ret = ret.parenthesizeBody();
        }
        return ret;
    }

    @Override
    public @Nonnull Translation calculationExpression(@Nonnull final BashpileParser.CalculationExpressionContext ctx) {
        // get the child translations
        List<Translation> childTranslations;
        childTranslations = ctx.children.stream()
                .map(visitor::visit)
                .map(tr -> tr.inlineAsNeeded(BashTranslationHelper::unwindNested))
                .toList();

        // child translations in the format of 'expr operator expr', so we are only interested in the first and last
        final Translation first = childTranslations.get(0);
        final Translation second = getLast(childTranslations);
        // types section
        if (areNumericExpressions(first, second) && inCalc(ctx)) {
            return toTranslation(childTranslations.stream(), NUMBER_TYPE, NORMAL);
        } else if (areNumericExpressions(first, second)) {
            final String translationsString = childTranslations.stream()
                    .map(Translation::body).collect(Collectors.joining(" "));
            return toTranslation(childTranslations.stream(), NUMBER_TYPE, NEEDS_INLINING_OFTEN)
                    .body("bc <<< \"%s\"".formatted(translationsString));
        } else if (areStringExpressions(first, second)) {
            final String op = ctx.op.getText();
            Asserts.assertEquals("+", op, "Only addition is allowed on Strings, but got " + op);
            return toTranslation(Stream.of(first, second)
                    .map(Translation::unquoteBody)
                    .map(tr -> tr.lambdaBody(Strings::unparenthesize)));
        // found no matching types -- error section
        } else if (first.type().equals(NOT_FOUND_TYPE) || second.type().equals(NOT_FOUND_TYPE)) {
            throw new UserError("`%s` or `%s` are undefined".formatted(
                    first.body(), second.body()), lineNumber(ctx));
        } else {
            // throw type error for all others
            throw new TypeError("Incompatible types in calc: %s and %s".formatted(
                    first.type(), second.type()), lineNumber(ctx));
        }
    }

    @Override
    public @Nonnull Translation unaryPrimaryExpression(BashpileParser.UnaryPrimaryExpressionContext ctx) {
        final String primary = ctx.unaryPrimary().getText();
        Translation valueBeingTested;
        // right now all implemented primaries are string tests
        valueBeingTested = visitor.visit(ctx.expression()).inlineAsNeeded(BashTranslationHelper::unwindNested);

        if (ctx.expression() instanceof BashpileParser.ArgumentsBuiltinExpressionContext argumentsCtx) {
            // for isset (-n) and unset (-z) '+default' will evaluate to nothing if unset, and 'default' if set
            // see https://stackoverflow.com/questions/3601515/how-to-check-if-a-variable-is-set-in-bash for details
            final String parameterExpansion = List.of("isset", "unset").contains(primary) ? "+default" : "";
            final String argNumber = argumentsCtx.argumentsBuiltin().Number().getText();
            valueBeingTested = valueBeingTested.body("${%s%s}".formatted(argNumber, parameterExpansion));
        }
        final String body = "[ %s \"%s\" ]".formatted(
                primaryTranslations.getOrDefault(primary, primary), valueBeingTested.unquoteBody().body());
        return new Translation(valueBeingTested.preamble(), body, STR_TYPE, List.of(NORMAL));
    }

    @Override
    public @Nonnull Translation binaryPrimaryExpression(BashpileParser.BinaryPrimaryExpressionContext ctx) {
        Asserts.assertEquals(3, ctx.getChildCount(), "Should be 3 parts");
        String primary = ctx.binaryPrimary().getText();
        final Translation firstTranslation =
                visitor.visit(ctx.getChild(0)).inlineAsNeeded(BashTranslationHelper::unwindNested);
        final Translation secondTranslation =
                visitor.visit(ctx.getChild(2)).inlineAsNeeded(BashTranslationHelper::unwindNested);

        // we do some checks for strict equals and strict not equals
        final boolean noTypeMatch = !(firstTranslation.type().equals(secondTranslation.type()));
        if (ctx.binaryPrimary().IsStrictlyEqual() != null && noTypeMatch) {
            return toStringTranslation("false");
        } else if (ctx.binaryPrimary().InNotStrictlyEqual() != null && noTypeMatch) {
            return toStringTranslation("true");
        } // else make a non-trivial string or numeric primary

        String body;
        primary = primaryTranslations.getOrDefault(primary, primary);
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
        return new Translation(body, BOOL_TYPE, CONDITIONAL).addPreamble(firstTranslation.preamble())
                .addPreamble(secondTranslation.preamble());
    }

    @Override
    public Translation combiningExpression(BashpileParser.CombiningExpressionContext ctx) {
        final String operator = ctx.combiningOperator().getText().equals("and") ? "&&" : "||";
        final Translation firstTranslation =
                visitor.visit(ctx.getChild(0)).inlineAsNeeded(BashTranslationHelper::unwindNested);
        final Translation secondTranslation =
                visitor.visit(ctx.getChild(2)).inlineAsNeeded(BashTranslationHelper::unwindNested);

        final String body = "%s %s %s".formatted(firstTranslation.unquoteBody().body(), operator,
                secondTranslation.unquoteBody().body());
        return toStringTranslation(body).addPreamble(firstTranslation.preamble())
                .addPreamble(secondTranslation.preamble());
    }

    @Override
    public Translation listOfBuiltinExpression(BashpileParser.ListOfBuiltinExpressionContext ctx) {
        // guard, empty list
        if (ctx.expression().isEmpty()) {
            return new ListOfTranslation(UNKNOWN_TYPE);
        }

        // guard, checks types
        final List<Pair<Integer, Translation>> translations = IntStream.range(0, ctx.expression().size())
                .mapToObj(it -> Pair.of(it, visitor.visit(ctx.expression(it)))).toList();
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
        final String variableName = ctx.Id().getText();
        final Type type = typeStack.getVariableType(variableName);
        // list syntax is `listName[*]`
        // see https://www.masteringunixshell.net/qa35/bash-how-to-print-array.html
        final String allIndexes = type.mainType().isBasic() ? "" : /* list */ "[*]";
        // use ${var} syntax instead of $var for string concatenations, e.g. `${var}someText`
        return new Translation("${%s%s}".formatted(ctx.getText(), allIndexes), type, NORMAL);
    }

    @Override
    public Translation listIndexExpression(BashpileParser.ListAccessExpressionContext ctx) {
        final String variableName = ContextUtils.getIdText(ctx);
        final Type type = typeStack.getVariableType(Objects.requireNonNull(variableName));
        // use ${var} syntax instead of $var for string concatenations, e.g. `${var}someText`
        Integer index = ContextUtils.getListAccessorIndex(ctx);
        return new Translation("${%s[%d]}".formatted(variableName, index), type.asContentsType(), NORMAL);
    }

    // expression helper rules

    @Override
    public @Nonnull Translation shellString(@Nonnull final BashpileParser.ShellStringContext ctx) {
        Translation contentsTranslation = ctx.shellStringContents().stream()
                .map(visitor::visit)
                .map(tr -> tr.inlineAsNeeded(BashTranslationHelper::unwindNested))
                .reduce(Translation::add)
                .map(x -> x.lambdaBody(Strings::dedent))
                .map(BashTranslationHelper::joinEscapedNewlines)
                .orElseThrow()
                .type(UNKNOWN_TYPE);

        // a subshell does NOT need inlining often, see conditionalStatement
        if (!Strings.inParentheses(contentsTranslation.body())) {
            contentsTranslation = contentsTranslation.metadata(NEEDS_INLINING_OFTEN);
        }

        contentsTranslation = unwindNested(contentsTranslation);
        return contentsTranslation.unescapeBody();
    }
}
