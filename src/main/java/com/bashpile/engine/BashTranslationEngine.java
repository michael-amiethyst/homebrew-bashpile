package com.bashpile.engine;

import com.bashpile.Asserts;
import com.bashpile.BashpileParser;
import com.bashpile.Strings;
import com.bashpile.engine.strongtypes.FunctionTypeInfo;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.engine.strongtypes.TypeStack;
import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bashpile.Asserts.*;
import static com.bashpile.Strings.lambdaLastLine;
import static com.bashpile.engine.BashTranslationHelper.*;
import static com.bashpile.engine.LevelCounter.BLOCK_LABEL;
import static com.bashpile.engine.Translation.*;
import static com.bashpile.engine.strongtypes.TranslationMetadata.NEEDS_INLINING_OFTEN;
import static com.bashpile.engine.strongtypes.TranslationMetadata.NORMAL;
import static com.bashpile.engine.strongtypes.Type.*;
import static com.google.common.collect.Iterables.getLast;

/**
 * Translates to Bash5 with four spaces as a tab.
 */
public class BashTranslationEngine implements TranslationEngine {

    // static variables

    /** Four spaces */
    public static final String TAB = "    ";

    private static final Map<String, String> primaryTranslations = Map.of("unset", "-z", "isEmpty", "-z",
            "isNotEmpty", "-n");

    // instance variables

    /** This is how we enforce type checking at compile time.  Mutable. */
    private final TypeStack typeStack = new TypeStack();

    /** The Bashpile script input file or literal text */
    @Nonnull
    private final String origin;

    /** All the functions hoisted so far, so we can ensure we don't emit them twice */
    private final Set<String> foundForwardDeclarations = new HashSet<>();

    /** The current create statement filenames for using in a trap command */
    private final Stack<String> createFilenamesStack = new Stack<>();

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
                export IFS=$'\\n\\t'
                declare s
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

        // handle the initial variable declaration and type, if applicable
        String variableName;
        if (ctx.typedId() != null) {
            variableName = ctx.typedId().Id().getText();
            if (fileNameIsId) {
                Asserts.assertEquals(variableName, ctx.Id().getText(),
                        "Create Statements must have matching Ids at the start and end.");
            }
            // add this variable to the type map
            final Type type = Type.valueOf(ctx.typedId().Type().getText().toUpperCase());
            typeStack.putVariableType(variableName, type, lineNumber(ctx));
        }

        // create child translations and other variables
        Translation shellString;
        final boolean addingCommandSubstitution = ctx.typedId() != null;
        shellString = visitor.visit(ctx.shellString());
        if (addingCommandSubstitution) {
            shellString = unwindAll(shellString);
        }
        final TerminalNode filenameNode = fileNameIsId ? ctx.Id() : ctx.String();
        String filename =  visitor.visit(filenameNode).unquoteBody().body();
        // convert ID to "$ID"
        filename = fileNameIsId ? "\"$%s\"".formatted(filename) : filename;

        // create our final translation and pop the stack
        createFilenamesStack.push(filename);
        try {
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
    public @Nonnull Translation functionForwardDeclarationStatement(
            @Nonnull final BashpileParser.FunctionForwardDeclarationStatementContext ctx) {
        // create translations
        final Translation comment = createCommentTranslation("function forward declaration", lineNumber(ctx));
        final ParserRuleContext functionDeclCtx = getFunctionDeclCtx(visitor, ctx);
        final Translation hoistedFunction = visitor.visit(functionDeclCtx).lambdaBody(String::stripTrailing);

        // register that this forward declaration has been handled
        foundForwardDeclarations.add(ctx.typedId().Id().getText());

        // add translations
        return comment.add(hoistedFunction.assertEmptyPreamble());
    }

    @Override
    public @Nonnull Translation functionDeclarationStatement(
            @Nonnull final BashpileParser.FunctionDeclarationStatementContext ctx) {
        // avoid translating twice if was part of a forward declaration
        final String functionName = ctx.typedId().Id().getText();
        if (foundForwardDeclarations.contains(functionName)) {
            return EMPTY_TRANSLATION;
        }

        // check for double declaration
        if (typeStack.containsFunction(functionName)) {
            throw new UserError(
                    functionName + " was declared twice (function overloading is not supported)", lineNumber(ctx));
        }

        // regular processing -- no forward declaration

        // register function param types and return type
        final List<Type> typeList = ctx.paramaters().typedId()
                .stream().map(Type::valueOf).collect(Collectors.toList());
        final Type retType = Type.valueOf(ctx.typedId().Type().getText().toUpperCase());
        typeStack.putFunctionTypes(functionName, new FunctionTypeInfo(typeList, retType));

        try (var ignored = new LevelCounter(BLOCK_LABEL); var ignored2 = typeStack.pushFrame()) {

            // register local variable types
            ctx.paramaters().typedId().forEach(
                    x -> typeStack.putVariableType(
                            x.Id().getText(), Type.valueOf(x.Type().getText().toUpperCase()), lineNumber(ctx)));

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
                        .map(Translation::body)
                        .map(str -> "local %s=$%s;".formatted(str, i.getAndIncrement()))
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
        try (var ignored = new LevelCounter(BLOCK_LABEL); var ignored2 = typeStack.pushFrame()) {
            final Translation comment = createCommentTranslation("anonymous block", lineNumber(ctx));
            // behind the scenes we need to name the anonymous function
            final String anonymousFunctionName = "anon" + anonBlockCounter++;
            // map of x to x needed for upcasting to parent type
            final Translation blockStatements = visitBodyStatements(ctx.statement(), visitor);
            // define function and then call immediately with no arguments
            final Translation selfCallingAnonymousFunction = toParagraphTranslation("%s () {\n%s}; %s\n"
                    .formatted(anonymousFunctionName, blockStatements.body(), anonymousFunctionName));
            return comment.add(selfCallingAnonymousFunction);
        }
    }

    @Override
    public @Nonnull Translation conditionalStatement(BashpileParser.ConditionalStatementContext ctx) {
        final Translation guard;
        final Translation not = ctx.Not() != null ? new Translation("! ", STR, NORMAL) : EMPTY_TRANSLATION;
        Translation expressionTranslation = visitor.visit(ctx.expression());
        expressionTranslation = unwindAll(expressionTranslation);
        if (expressionTranslation.type().isNumeric()) {
            // to handle floats we use bc, but bc uses C style bools (1 for true, 0 for false) so we need to convert
            expressionTranslation = expressionTranslation
                    .inlineAsNeeded(BashTranslationHelper::unwindAll)
                    .lambdaBody("[ \"$(bc <<< \"%s == 0\")\" -eq 1 ]"::formatted);
        }
        guard = not.add(expressionTranslation);

        final Translation ifBlockStatements = visitBodyStatements(ctx.statement(), visitor);
        String elseBlock = "";
        if (ctx.elseBody() != null) {
            final Translation elseBlockStatements = visitBodyStatements(ctx.elseBody().statement(), visitor);
            elseBlock = """
                    
                    else
                    %s""".formatted(elseBlockStatements).stripTrailing();
        }
        final String conditional = """
                if %s; then
                %s%s
                fi
                """.formatted(guard.body(), ifBlockStatements.mergePreamble().body().stripTrailing(), elseBlock);
        return toParagraphTranslation(guard.preamble() + conditional);
    }

    @Override
    public @Nonnull Translation assignmentStatement(@Nonnull final BashpileParser.AssignmentStatementContext ctx) {
        // add this variable to the type map
        final String variableName = ctx.typedId().Id().getText();
        final Type type = Type.valueOf(ctx.typedId().Type().getText().toUpperCase());
        typeStack.putVariableType(variableName, type, lineNumber(ctx));

        // visit the right hand expression
        final boolean exprExists = ctx.expression() != null;
        Translation exprTranslation;
        exprTranslation = exprExists
                ? visitor.visit(ctx.expression()).inlineAsNeeded(BashTranslationHelper::unwindNested)
                : EMPTY_TRANSLATION;
        exprTranslation = unwindNested(exprTranslation);
        assertTypesCoerce(type, exprTranslation.type(), ctx.typedId().Id().getText(), lineNumber(ctx));

        // create translations
        final Translation comment = createCommentTranslation("assign statement", lineNumber(ctx));
        final Translation subcomment =
                subcommentTranslationOrDefault(exprTranslation.hasPreamble(), "assign statement body");
        final Translation variableDeclaration = toLineTranslation(getLocalText() + variableName + "\n");
        // merge expr into the assignment
        final String assignmentBody = exprExists ? "%s=%s\n".formatted(variableName, exprTranslation.body()) : "";
        final Translation assignment =
                toParagraphTranslation(assignmentBody).addPreamble(exprTranslation.preamble());

        // order is comment, preamble, subcomment, variable declaration, assignment
        final Translation subcommentToAssignment = subcomment.add(variableDeclaration).add(assignment);
        return comment.add(subcommentToAssignment.mergePreamble()).type(NA).metadata(NORMAL);
    }

    @Override
    public @Nonnull Translation reassignmentStatement(@Nonnull final BashpileParser.ReassignmentStatementContext ctx) {
        // get name and type
        final String variableName = ctx.Id().getText();
        final Type expectedType = typeStack.getVariableType(variableName);
        if (expectedType.equals(NOT_FOUND)) {
            throw new TypeError(variableName + " has not been declared", lineNumber(ctx));
        }

        // get expression and it's type
        Translation exprTranslation;
        exprTranslation = visitor.visit(ctx.expression()).inlineAsNeeded(BashTranslationHelper::unwindNested);
        exprTranslation = unwindNested(exprTranslation);
        final Type actualType = exprTranslation.type();
        Asserts.assertTypesCoerce(expectedType, actualType, variableName, lineNumber(ctx));

        // create translations
        final Translation comment = createCommentTranslation("reassign statement", lineNumber(ctx));
        final Translation subcomment =
                subcommentTranslationOrDefault(exprTranslation.hasPreamble(), "reassignment statement body");
        // merge exprTranslation into reassignment
        final String reassignmentBody = "%s%s=%s\n".formatted(
                getLocalText(true), variableName, exprTranslation.body());
        final Translation reassignment =
                toLineTranslation(reassignmentBody).addPreamble(exprTranslation.preamble());

        // order is: comment, preamble, subcomment, reassignment
        final Translation preambleToReassignment = subcomment.add(reassignment).mergePreamble();
        return comment.add(preambleToReassignment).assertParagraphBody().type(NA).metadata(NORMAL);
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
                .map(tr -> tr.body("""
                        printf "%s\\n"
                        """.formatted(tr.unquoteBody().body())))
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
        final String functionName = enclosingFunction.typedId().Id().getText();
        final FunctionTypeInfo functionTypes = typeStack.getFunctionTypes(functionName);
        Translation exprTranslation =
                exprExists ? visitor.visit(ctx.expression()) : Translation.EMPTY_TYPE;
        assertTypesCoerce(functionTypes.returnType(), exprTranslation.type(), functionName, lineNumber(ctx));

        if (!exprExists) {
            return EMPTY_TRANSLATION;
        }

        final Translation comment = createCommentTranslation("return statement", lineNumber(ctx));
        final Function<String, String> returnLineLambda = str -> {
            if (functionTypes.returnType().equals(STR)
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
        final Type castTo = Type.valueOf(ctx.Type().getText().toUpperCase());
        Translation expression = visitor.visit(ctx.expression());
        final int lineNumber = lineNumber(ctx);
        final TypeError typecastError = new TypeError(
                "Casting %s to %s is not supported".formatted(expression.type(), castTo), lineNumber);
        switch (expression.type()) {
            case BOOL -> expression = typecastBool(castTo, expression, typecastError);
            case INT -> expression = typecastInt(castTo, expression, lineNumber, typecastError);
            case FLOAT -> expression = typecastFloat(castTo, expression, lineNumber, typecastError);
            case STR -> expression = typecastStr(castTo, expression, lineNumber, typecastError);
            case UNKNOWN -> typecastUnknown(castTo, typecastError);
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
        final String functionName = ctx.Id().getText();
        final boolean hasArgs = ctx.argumentList() != null;
        final List<Translation> argumentTranslationsList = hasArgs
                ? ctx.argumentList().expression().stream()
                        .map(visitor::visit)
                        .map(tr -> tr.inlineAsNeeded(BashTranslationHelper::unwindNested))
                        .map(BashTranslationHelper::unwindNested)
                        .toList()
                : List.of();

        // check types
        final FunctionTypeInfo expectedTypes = typeStack.getFunctionTypes(functionName);
        final List<Type> actualTypes = argumentTranslationsList.stream().map(Translation::type).toList();
        Asserts.assertTypesCoerce(expectedTypes.parameterTypes(), actualTypes, functionName, lineNumber(ctx));

        // extract argText and preambles from argumentTranslations
        // empty list or ' arg1Text arg2Text etc.'
        Translation argumentTranslations = EMPTY_TRANSLATION;
        if (hasArgs) {
            argumentTranslations = new Translation(" ", STR, NORMAL).add(argumentTranslationsList.stream()
                    .map(Translation::quoteBody)
                    .reduce((left, right) -> new Translation(
                            left.preamble() + right.preamble(),
                            left.body() + " " + right.body(),
                            right.type(),
                            right.metadata()))
                    .orElseThrow());
        }

        // lookup return type of this function
        final Type retType = typeStack.getFunctionTypes(id).returnType();

        // suppress output if we are a top-level statement
        // this covers the case of calling a function without using the return
        Translation ret = new Translation(
                argumentTranslations.preamble(), id + argumentTranslations.body(), retType, List.of(NORMAL));
        ret = ret.lambdaBody("%s >/dev/null"::formatted).metadata(NEEDS_INLINING_OFTEN).mergePreamble();
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
            return toTranslation(childTranslations.stream(), Type.NUMBER, NORMAL);
        } else if (areNumericExpressions(first, second)) {
            final String translationsString = childTranslations.stream()
                    .map(Translation::body).collect(Collectors.joining(" "));
            return toTranslation(childTranslations.stream(), Type.NUMBER, NEEDS_INLINING_OFTEN)
                    .body("bc <<< \"%s\"".formatted(translationsString));
        } else if (areStringExpressions(first, second)) {
            final String op = ctx.op.getText();
            Asserts.assertEquals("+", op, "Only addition is allowed on Strings, but got " + op);
            return toTranslation(Stream.of(first, second)
                    .map(Translation::unquoteBody)
                    .map(tr -> tr.lambdaBody(Strings::unparenthesize)));
        // found no matching types -- error section
        } else if (first.type().equals(Type.NOT_FOUND) || second.type().equals(Type.NOT_FOUND)) {
            throw new UserError("`%s` or `%s` are undefined".formatted(
                    first.body(), second.body()), lineNumber(ctx));
        } else {
            // throw type error for all others
            throw new TypeError("Incompatible types in calc: %s and %s".formatted(
                    first.type(), second.type()), lineNumber(ctx));
        }
    }

    @Override
    public @Nonnull Translation primaryExpression(BashpileParser.PrimaryExpressionContext ctx) {
        final String primary = ctx.primary().getText();
        Translation valueBeingTested;
        // right now all implemented primaries are string tests
        valueBeingTested = visitor.visit(ctx.expression()).inlineAsNeeded(BashTranslationHelper::unwindNested);

        if (ctx.expression() instanceof BashpileParser.ArgumentsBuiltinExpressionContext argumentsCtx) {
            // for unset (-z) '+default' will evaluate to nothing if unset, and 'default' if set
            // see https://stackoverflow.com/questions/3601515/how-to-check-if-a-variable-is-set-in-bash for details
            final String parameterExpansion = primary.equals("unset") ? "+default" : "";
            valueBeingTested = valueBeingTested.body("${%s%s}".formatted(
                    argumentsCtx.argumentsBuiltin().Number().getText(), parameterExpansion));
        }
        final String body = "[ %s \"%s\" ]".formatted(
                primaryTranslations.get(primary), valueBeingTested.unquoteBody().body());
        return new Translation(valueBeingTested.preamble(), body, STR, List.of(NORMAL));
    }

    @Override
    public @Nonnull Translation idExpression(BashpileParser.IdExpressionContext ctx) {
        final String variableName = ctx.Id().getText();
        final Type type = typeStack.getVariableType(variableName);
        // use ${var} syntax instead of $var for string concatenations, e.g. `${var}someText`
        return new Translation("${%s}".formatted(ctx.getText()), type, NORMAL);
    }

    // expression helper rules

    @Override
    public @Nonnull Translation shellString(@Nonnull final BashpileParser.ShellStringContext ctx) {
        Translation contentsTranslation;
        final Stream<Translation> contentsStream = ctx.shellStringContents().stream()
                .map(visitor::visit).map(tr -> tr.inlineAsNeeded(BashTranslationHelper::unwindNested));
        contentsTranslation = toTranslation(contentsStream, UNKNOWN, NORMAL).lambdaBody(Strings::dedent);
        if (!Strings.PARENTHESIS.matcher(contentsTranslation.body()).matches()) {
            contentsTranslation = contentsTranslation.metadata(NEEDS_INLINING_OFTEN);
            // a subshell does NOT need inlining often, see conditionalStatement
        }

        contentsTranslation = unwindNested(contentsTranslation);
        return contentsTranslation.unescapeBody();
    }
}
