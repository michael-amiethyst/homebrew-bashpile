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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bashpile.AntlrUtils.*;
import static com.bashpile.Asserts.*;
import static com.bashpile.Strings.*;
import static com.bashpile.engine.LevelCounter.*;
import static com.bashpile.engine.Translation.*;
import static com.bashpile.engine.strongtypes.Type.*;
import static com.bashpile.engine.strongtypes.TypeMetadata.INLINE;
import static com.bashpile.engine.strongtypes.TypeMetadata.NORMAL;
import static com.google.common.collect.Iterables.getLast;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Translates to Bash5 with four spaces as a tab.
 */
public class BashTranslationEngine implements TranslationEngine {

    // static variables

    public static final String TAB = "    ";

    private static final Pattern GENERATED_VARIABLE_NAME = Pattern.compile("^\\$\\{__bp.*");

    // instance variables

    /** This is how we enforce type checking at compile time.  Mutable. */
    private final TypeStack typeStack = new TypeStack();

    /** Should be set immediately after creation with {@link #setVisitor(BashpileVisitor)} */
    private BashpileVisitor visitor;

    @Nonnull
    private final String origin;

    /** We need to name the anonymous blocks, anon0, anon1, anon2, etc.  We keep that counter here. */
    private int anonBlockCounter = 0;

    /** Used to ensure variable names are unique */
    private int subshellWorkaroundCounter = 0;

    /** All the functions hoisted so far, so we can ensure we don't emit them twice */
    private final Set<String> foundForwardDeclarations = new HashSet<>();

    /** The current create statement filenames for using in a trap command */
    private final Stack<String> createFilenamesStack = new Stack<>();

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
    public Translation originHeader() {
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
    public Translation createsStatement(BashpileParser.CreatesStatementContext ctx) {
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
        final Translation shellString = visitor.visit(ctx.shellString());
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
            final String body = getBodyString(ctx, shellString, filename);
            final Translation bodyTranslation = toParagraphTranslation(body);

            // merge translations and preambles
            return comment.add(
                    subcomment.add(bodyTranslation)
                            .addPreamble(shellString.preamble())
                            .mergePreamble());
        } finally {
            createFilenamesStack.pop();
        }
    }

    /** Helper to {@link #createsStatement(BashpileParser.CreatesStatementContext)} */
    private String getBodyString(
            final @Nonnull BashpileParser.CreatesStatementContext ctx,
            final @Nonnull Translation shellString,
            final @Nonnull String filename) {
        final String check = String.join("; ", shellString.body().trim().split("\n"));

        // set noclobber avoids some race conditions
        String ifGuard;
        String variableName = null;
        if (ctx.typedId() != null) {
            variableName = ctx.typedId().Id().getText();
            ifGuard = "%s %s\nif %s=$(set -o noclobber; %s) 2> /dev/null; then".formatted(
                    getLocalText(), variableName, variableName, check);
        } else {
            ifGuard = "if (set -o noclobber; %s) 2> /dev/null; then".formatted(check);
        }

        // create our statements translation
        final Translation statements = ctx.statement().stream()
                .map(visitor::visit)
                .reduce(Translation::add)
                .orElseThrow()
                .assertParagraphBody()
                .assertNoBlankLinesInBody();
        // create an ifBody to put into the bodyTranslation
        // only one trap can be in effect at a time, so we keep a stack of all current filenames to delete
        String ifBody = """
                trap 'rm -f %s; exit 10' INT TERM EXIT
                ## wrapped body of creates statement
                %s
                ## end of wrapped body of creates statement
                rm -f %s
                trap - INT TERM EXIT""".formatted(
                String.join(" ", createFilenamesStack), statements.body(), filename);
        ifBody = lambdaAllLines(ifBody, str -> TAB + str);
        ifBody = lambdaFirstLine(ifBody, String::stripLeading);

        // `return` in an if statement doesn't work, so we need to `exit` if we're not in a function or subshell
        final String exitOrReturn = isTopLevelShell() && !in(BLOCK_LABEL) ? "exit" : "return";
        final String plainFilename = STRING_QUOTES.matcher(filename).replaceAll("").substring(1);
        final String errorDetails = variableName != null ? "  Output from attempted creation:\\n$" + variableName : "";
        String elseBody = """
                printf "Failed to create %s correctly.%s"
                rm -f %s
                %s 1""".formatted(plainFilename, errorDetails, filename, exitOrReturn);
        elseBody = lambdaAllLines(elseBody, str -> TAB + str);
        elseBody = lambdaFirstLine(elseBody, String::stripLeading);
        return """
                %s
                    %s
                else
                    %s
                fi
                declare -i __bp_exitCode=$?
                if [ "$__bp_exitCode" -ne 0 ]; then exit "$__bp_exitCode"; fi
                """.formatted(ifGuard, ifBody, elseBody);
    }

    @Override
    public @Nonnull Translation functionForwardDeclarationStatement(
            @Nonnull final BashpileParser.FunctionForwardDeclarationStatementContext ctx) {
        final ParserRuleContext functionDeclCtx = getFunctionDeclCtx(visitor, ctx);
        try (var ignored = new LevelCounter(FORWARD_DECL_LABEL)) {
            // create translations
            final Translation comment = createCommentTranslation("function forward declaration", lineNumber(ctx));
            // remove trailing newline
            final Translation hoistedFunction = visitor.visit(functionDeclCtx).lambdaBody(String::stripTrailing);
            // register that this forward declaration has been handled
            foundForwardDeclarations.add(ctx.typedId().Id().getText());
            // add translations
            return comment.add(hoistedFunction.assertEmptyPreamble());
        }
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
            final Translation comment = createHoistedCommentTranslation("function declaration", lineNumber(ctx));
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
            final Translation comment = createHoistedCommentTranslation("anonymous block", lineNumber(ctx));
            // behind the scenes we need to name the anonymous function
            final String anonymousFunctionName = "anon" + anonBlockCounter++;
            // map of x to x needed for upcasting to parent type
            final Translation blockStatements = ctx.statement().stream()
                    .map(visitor::visit)
                    .map(tr -> tr.lambdaBodyLines(str -> TAB + str))
                    .reduce(Translation::add)
                    .orElseThrow()
                    .assertEmptyPreamble();
            // define function and then call immediately with no arguments
            final Translation selfCallingAnonymousFunction = toParagraphTranslation("%s () {\n%s}; %s\n"
                    .formatted(anonymousFunctionName, blockStatements.body(), anonymousFunctionName));
            return comment.add(selfCallingAnonymousFunction);
        }
    }

    @Override
    public Translation conditionalStatement(BashpileParser.ConditionalStatementContext ctx) {
        final Translation predicate = visitor.visit(ctx.expression());
        final Translation ifBlockStatements = ctx.statement().stream()
                .map(visitor::visit)
                .map(tr -> tr.lambdaBodyLines(str -> TAB + str))
                .reduce(Translation::add)
                .orElseThrow();
        final String conditional = """
                if [ %s ]; then
                %s
                fi
                """.formatted(predicate.body(), ifBlockStatements.mergePreamble().body().stripTrailing());
        return toParagraphTranslation(predicate.preamble() + conditional);
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
        try (var ignored = new LevelCounter(ASSIGNMENT_LABEL)) {
            exprTranslation = exprExists ? visitor.visit(ctx.expression()) : EMPTY_TRANSLATION;
        }
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
        return comment.add(subcommentToAssignment.mergePreamble()).type(NA).typeMetadata(NORMAL);
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
        try (var ignored = new LevelCounter(ASSIGNMENT_LABEL)) {
            exprTranslation = visitor.visit(ctx.expression());
        }
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
        return comment.add(preambleToReassignment).assertParagraphBody().type(NA).typeMetadata(NORMAL);
    }

    @Override
    public @Nonnull Translation printStatement(@Nonnull final BashpileParser.PrintStatementContext ctx) {
        // guard
        final BashpileParser.ArgumentListContext argList = ctx.argumentList();
        if (argList == null) {
            return toLineTranslation("printf \"\\n\"\n");
        }

        // body
        try (final var ignored = new LevelCounter(PRINT_LABEL)) {
            final Translation comment = createCommentTranslation("print statement", lineNumber(ctx));
            final Translation arguments = argList.expression().stream()
                    .map(visitor::visit)
                    .map(tr -> tr.isInlineOrSubshell() && inCommandSubstitution() ? unnest(tr) : tr)
                    .map(tr -> tr.body("""
                            printf "%s\\n"
                            """.formatted(tr.unquoteBody().body())))
                    .reduce(Translation::add)
                    .orElseThrow();
            final Translation subcomment =
                    subcommentTranslationOrDefault(arguments.hasPreamble(), "print statement body");
            return comment.add(subcomment.add(arguments).mergePreamble());
        }
    }

    @Override
    public Translation expressionStatement(@Nonnull final BashpileParser.ExpressionStatementContext ctx) {
        final Translation expr = visitor.visit(ctx.expression()).add(NEWLINE);
        final Translation comment = createCommentTranslation("expression statement", lineNumber(ctx));
        final Translation subcomment =
                subcommentTranslationOrDefault(expr.hasPreamble(), "expression statement body");
        // order is: comment, preamble, subcomment, expr
        final Translation exprStatement = subcomment.add(expr).mergePreamble();
        return comment.add(exprStatement).type(expr.type()).typeMetadata(expr.typeMetadata());
    }

    @Override
    public @Nonnull Translation returnPsudoStatement(@Nonnull final BashpileParser.ReturnPsudoStatementContext ctx) {
        final boolean exprExists = ctx.expression() != null;

        // check return matches with function declaration
        final BashpileParser.FunctionDeclarationStatementContext enclosingFunction =
                (BashpileParser.FunctionDeclarationStatementContext) ctx.parent.parent;
        final String functionName = enclosingFunction.typedId().Id().getText();
        final FunctionTypeInfo functionTypes = typeStack.getFunctionTypes(functionName);
        final Translation exprTranslation =
                exprExists ? visitor.visit(ctx.expression()) : Translation.EMPTY_TYPE;
        assertTypesCoerce(functionTypes.returnType(), exprTranslation.type(), functionName, lineNumber(ctx));

        if (!exprExists) {
            return EMPTY_TRANSLATION;
        }

        final Translation comment = createHoistedCommentTranslation("return statement", lineNumber(ctx));
        final Function<String, String> toPrintf =
                str -> "printf \"%s\"\n".formatted(STRING_QUOTES.matcher(str).replaceAll(""));
        final Translation exprBody = toParagraphTranslation(lambdaLastLine(exprTranslation.body(), toPrintf))
                .addPreamble(exprTranslation.preamble());
        return comment.add(exprBody.mergePreamble());
    }

    // expressions

    /**
     * True/False cast to 1/0.  Any number besides 0 casts to true.  "true" and "false" (any case) cast to BOOLs.
     * Quoted numbers (numbers in STRs) cast to BOOLs like numbers.  Anything cast to an STR gets quotes around it.
     */
    @Override
    public Translation typecastExpression(BashpileParser.TypecastExpressionContext ctx) {
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
        final List<Translation> argumentTranslations = hasArgs
                ? ctx.argumentList().expression().stream().map(visitor::visit).toList()
                : List.of();

        // check types
        final FunctionTypeInfo expectedTypes = typeStack.getFunctionTypes(functionName);
        final List<Type> actualTypes = argumentTranslations.stream().map(Translation::type).toList();
        Asserts.assertTypesCoerce(expectedTypes.parameterTypes(), actualTypes, functionName, lineNumber(ctx));

        // extract argText and preambles from argumentTranslations
        // empty list or ' arg1Text arg2Text etc.'
        String argText = "";
        if (hasArgs) {
            argText = " " + argumentTranslations.stream()
                    .map(Translation::body)
                    .map("\"%s\""::formatted)
                    .collect(Collectors.joining(" "));
        }
        final Translation preambles = argumentTranslations.stream().reduce(Translation::add).orElse(EMPTY_TRANSLATION);

        // lookup return type of this function
        final Type retType = typeStack.getFunctionTypes(id).returnType();

        // suppress output if we are a top-level statement
        // this covers the case of calling a str function without using the string
        final boolean topLevelStatement = isTopLevelShell();
        if (topLevelStatement) {
            return new Translation(preambles.preamble(), id + argText + " >/dev/null", retType, NORMAL);
        } // else return an inline (command substitution)
        final String text = "$(%s%s)".formatted(id, argText);
        return new Translation(preambles.preamble(), text, retType, INLINE);
    }

    @Override
    public Translation parenthesisExpression(@Nonnull final BashpileParser.ParenthesisExpressionContext ctx) {
        // drop parenthesis
        Translation ret = visitor.visit(ctx.expression());

        // only keep parenthesis for necessary operations (e.g. "(((5)))" becomes "5" outside of a calc)
        if (ret.type().isPossiblyNumeric() && LevelCounter.in(CALC_LABEL)) {
            ret = ret.parenthesizeBody();
        }
        return ret;
    }

    @Override
    public @Nonnull Translation calculationExpression(@Nonnull final BashpileParser.CalculationExpressionContext ctx) {
        // get the child translations
        List<Translation> childTranslations;
        try (var ignored = new LevelCounter(CALC_LABEL)) {
            childTranslations = ctx.children.stream().map(visitor::visit).toList();
        }

        // child translations in the format of 'expr operator expr', so we are only interested in the first and last
        final Translation first = childTranslations.get(0);
        final Translation second = getLast(childTranslations);
        // check for nested calc call
        if (LevelCounter.in(CALC_LABEL) && maybeNumericExpressions(first, second)) {
            return toTranslation(childTranslations.stream(), Type.NUMBER, NORMAL);
            // types section
        } else if (maybeStringExpressions(first, second)) {
            final String op = ctx.op.getText();
            Asserts.assertEquals("+", op, "Only addition is allowed on Strings, but got " + op);
            return toTranslation(Stream.of(first.unquoteBody(), second.unquoteBody()), STR, NORMAL);
        } else if (maybeNumericExpressions(first, second)) {
            final String translationsString = childTranslations.stream()
                    .map(Translation::body).collect(Collectors.joining(" "));
            return toTranslation(childTranslations.stream(), Type.NUMBER, INLINE)
                    .body("$(bc <<< \"%s\")".formatted(translationsString));
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
    public Translation idExpression(BashpileParser.IdExpressionContext ctx) {
        final String variableName = ctx.Id().getText();
        final Type type = typeStack.getVariableType(variableName);
        // use ${var} syntax instead of $var for string concatenations, e.g. `${var}someText`
        return new Translation("${%s}".formatted(ctx.getText()), type, NORMAL);
    }

    // expression helper rules

    @Override
    public Translation shellString(@Nonnull final BashpileParser.ShellStringContext ctx) {
        // get the contents -- ditches the #() syntax
        final Stream<Translation> contentsStream = ctx.shellStringContents().stream().map(visitor::visit);
        Translation contentsTranslation = toTranslation(contentsStream, UNKNOWN, NORMAL)
                .lambdaBody(body -> {
                    // find leading whitespace of first non-blank line.  Strip that many chars from each line
                    final String[] lines = body.split("\n");
                    int i = 0;
                    while(isBlank(lines[i])) {
                        i++;
                    }
                    final String line = lines[i];
                    final int spaces = line.length() - line.stripLeading().length();
                    final String trailingNewline = body.endsWith("\n") ? "\n" : "";
                    return Arrays.stream(lines)
                            .filter(str -> !Strings.isBlank(str))
                            .map(str -> str.substring(spaces))
                            .collect(Collectors.joining("\n"))
                            + trailingNewline;
                });
        if (LevelCounter.inCommandSubstitution()) {
            // then wrap in command substitution and unnest as needed
            contentsTranslation = contentsTranslation.body("$(%s)".formatted(contentsTranslation.body()));
            for (int i = 0; i < LevelCounter.getCommandSubstitution(); i++) {
                contentsTranslation = unnest(contentsTranslation);
            }
        } else if (LevelCounter.in(PRINT_LABEL) || LevelCounter.in(ASSIGNMENT_LABEL)) {
            contentsTranslation = contentsTranslation.body("$(%s)".formatted(contentsTranslation.body()));
        } // else top level -- no additional processing needed
        return contentsTranslation.unescapeBody();
    }

    /**
     * Unnests inlines (Bashpile command substitutions) as needed.
     *
     * @see Translation
     * @see #printStatement(BashpileParser.PrintStatementContext)
     * @param ctx the context.
     * @return A translation, possibly with the {@link Translation#preamble()} set.
     */
    @Override
    public Translation inline(BashpileParser.InlineContext ctx) {
        // get the inline nesting level before our try-with-resources statement
        final int inlineNestingDepth = LevelCounter.get(LevelCounter.INLINE_LABEL);
        try (var ignored = new LevelCounter(LevelCounter.INLINE_LABEL)) {
            final Stream<Translation> children = ctx.children.stream().map(visitor::visit);
            Translation childrenTranslation = toTranslation(children, Type.UNKNOWN, NORMAL).unescapeBody();
            for (int i = 0; i < inlineNestingDepth; i++) {
                childrenTranslation = unnest(childrenTranslation);
            }
            return childrenTranslation;
        }
    }

    // helpers

    /** Get the Bashpile script linenumber that ctx is found in. */
    private int lineNumber(@Nonnull final ParserRuleContext ctx) {
        return ctx.start.getLine();
    }

    private static @Nonnull Translation createCommentTranslation(@Nonnull final String name, final int lineNumber) {
        return toLineTranslation("# %s, Bashpile line %d\n".formatted(name, lineNumber));
    }

    private static @Nonnull Translation createHoistedCommentTranslation(
            @Nonnull final String name, final int lineNumber) {
        final String hoisted = LevelCounter.in(FORWARD_DECL_LABEL) ? " (hoisted)" : "";
        return toLineTranslation("# %s, Bashpile line %d%s\n".formatted(name, lineNumber, hoisted));
    }

    private static @Nonnull Translation subcommentTranslationOrDefault(
            final boolean subcommentNeeded, @Nonnull final String name) {
        if (subcommentNeeded) {
            return toLineTranslation("## %s\n".formatted(name));
        }
        return EMPTY_TRANSLATION;
    }

    private static @Nonnull String getLocalText() {
        return getLocalText(false);
    }

    private static @Nonnull String getLocalText(final boolean reassignment) {
        final boolean indented = LevelCounter.in(BLOCK_LABEL);
        if (indented && !reassignment) {
            return "local ";
        } else if (!indented && !reassignment) {
            return "export ";
        } else { // reassignment
            return "";
        }
    }

    /**
     * Subshell and inline errored exit codes are ignored in Bash despite all configurations.
     * This workaround explicitly propagates errored exit codes.
     * Unnests one level.
     *
     * @param tr The base translation.
     * @return A Translation where the preamble is <code>tr</code>'s body and the work-around.
     * The body is a Command Substitution of a created variable
     * that holds the results of executing <code>tr</code>'s body.
     */
    private Translation unnest(@Nonnull final Translation tr) {
        // guard to check if unnest not needed
        if (GENERATED_VARIABLE_NAME.matcher(tr.body()).matches()) {
            return tr;
        }

        // assign Strings to use in translations
        final String subshellReturn = "__bp_subshellReturn%d".formatted(subshellWorkaroundCounter);
        final String exitCodeName = "__bp_exitCode%d".formatted(subshellWorkaroundCounter++);

        // create 5 lines of translations
        final Translation subcomment = toLineTranslation("## unnest for %s\n".formatted(tr.body()));
        final Translation export     = toLineTranslation("export %s\n".formatted(subshellReturn));
        final Translation assign     = toLineTranslation("%s=%s\n".formatted(subshellReturn, tr.body()));
        final Translation exitCode   = toLineTranslation("%s=$?\n".formatted(exitCodeName));
        final Translation check      = toLineTranslation("""
                if [ "$%s" -ne 0 ]; then exit "$%s"; fi
                """.formatted(exitCodeName, exitCodeName));

        // add the lines up
        final Translation preambles = subcomment.add(export).add(assign).add(exitCode).add(check);

        // add the preambles and swap the body
        return tr.addPreamble(preambles.body()).body("${%s}".formatted(subshellReturn));
    }

    private static boolean isTopLevelShell() {
        return !in(CALC_LABEL) && !in(PRINT_LABEL);
    }

    // typecast helpers

    private static Translation typecastBool(
            @Nonnull final Type castTo,
            @Nonnull Translation expression,
            @Nonnull final TypeError typecastError) {
        switch (castTo) {
            case BOOL -> {}
            case INT -> expression =
                    expression.body(expression.body().equalsIgnoreCase("true") ? "1" : "0");
            case FLOAT -> expression =
                    expression.body(expression.body().equalsIgnoreCase("true") ? "1.0" : "0.0");
            case STR -> expression = expression.quoteBody();
            default -> throw typecastError;
        }
        return expression;
    }

    @Nonnull
    private static Translation typecastInt(
            @Nonnull final Type castTo,
            @Nonnull Translation expression,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        // parse expression to a BigInteger
        BigInteger expressionValue;
        try {
            expressionValue = new BigInteger(expression.body());
        } catch (final NumberFormatException e) {
            throw new UserError(
                    "Couldn't parse %s to a FLOAT".formatted(expression.body()), lineNumber);
        }

        // cast
        switch (castTo) {
            case BOOL -> expression =
                    expression.body(!expressionValue.equals(BigInteger.ZERO) ? "true" : "false");
            case INT, FLOAT -> {}
            case STR -> expression = expression.quoteBody();
            default -> throw typecastError;
        }
        return expression;
    }

    @Nonnull
    private static Translation typecastFloat(
            @Nonnull final Type castTo,
            @Nonnull Translation expression,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        // parse expression as a BigDecimal
        BigDecimal expressionValue;
        try {
            expressionValue = new BigDecimal(expression.body());
        } catch (final NumberFormatException e) {
            throw new UserError("Couldn't parse %s to a FLOAT".formatted(expression.body()), lineNumber);
        }

        // cast
        switch (castTo) {
            case BOOL -> expression =
                    expression.body(expressionValue.compareTo(BigDecimal.ZERO) != 0 ? "true" : "false");
            case INT -> expression = expression.body(expressionValue.toBigInteger().toString());
            case FLOAT -> {}
            case STR -> expression = expression.quoteBody();
            default -> throw typecastError;
        }
        return expression;
    }

    private static Translation typecastStr(
            @Nonnull final Type castTo,
            @Nonnull Translation expression,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        switch (castTo) {
            case BOOL -> {
                expression = expression.unquoteBody();
                if (Type.isNumberString(expression.body())) {
                    expression = typecastFloat(castTo, expression, lineNumber, typecastError);
                } else if (expression.body().equalsIgnoreCase("true")
                        || expression.body().equalsIgnoreCase("false")) {
                    expression = expression.body(expression.body().toLowerCase());
                } else {
                    throw new TypeError("""
                            Could not cast STR to BOOL.
                            Only 'true', 'false' and numbers in Strings allowed.
                            Text was %s.""".formatted(expression.body()), lineNumber);
                }
            }
            case INT -> {
                // no automatic rounding for things like `"2.5":int`
                expression = expression.unquoteBody();
                final Type foundType = Type.parseNumberString(expression.body());
                if (!INT.equals(foundType)) {
                    throw new TypeError("""
                        Could not cast FLOAT value in STR to INT.  Try casting to float first.  Text was %s."""
                            .formatted(expression.body()), lineNumber);
                }
            }
            case FLOAT -> {
                expression = expression.unquoteBody();
                // verify the body parses as a valid number
                try {
                    Type.parseNumberString(expression.body());
                } catch (NumberFormatException e) {
                    throw new TypeError("""
                        Could not cast STR to FLOAT.  Is not a FLOAT.  Text was %s."""
                            .formatted(expression.body()), lineNumber);
                }
            }
            case STR -> {}
            default -> throw typecastError;
        }
        return expression;
    }

    private static void typecastUnknown(@Nonnull final Type castTo, @Nonnull final TypeError typecastError) {
        switch (castTo) {
            case BOOL, INT, FLOAT, STR -> {}
            default -> throw typecastError;
        }
    }
}
