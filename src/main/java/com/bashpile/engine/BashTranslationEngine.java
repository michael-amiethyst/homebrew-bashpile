package com.bashpile.engine;

import com.bashpile.Asserts;
import com.bashpile.BashpileParser;
import com.bashpile.engine.strongtypes.FunctionTypeInfo;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.engine.strongtypes.TypeStack;
import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import org.antlr.v4.runtime.ParserRuleContext;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bashpile.AntlrUtils.*;
import static com.bashpile.Asserts.*;
import static com.bashpile.StringUtils.prependLastLine;
import static com.bashpile.engine.LevelCounter.*;
import static com.bashpile.engine.Translation.*;
import static com.bashpile.engine.strongtypes.Type.*;
import static com.bashpile.engine.strongtypes.TypeMetadata.INLINE;
import static com.bashpile.engine.strongtypes.TypeMetadata.NORMAL;
import static com.google.common.collect.Iterables.getLast;

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

    private BashpileVisitor visitor;

    /** We need to name the anonymous blocks, anon0, anon1, anon2, etc.  We keep that counter here. */
    private int anonBlockCounter = 0;

    /** Used to ensure variable names are unique */
    private int subshellWorkaroundCounter = 0;

    /** All the functions hoisted so far, so we can ensure we don't emit them twice */
    private final Set<String> foundForwardDeclarations = new HashSet<>();

    // instance methods

    @Override
    public void setVisitor(@Nonnull final BashpileVisitor visitor) {
        this.visitor = visitor;
    }

    // header translations

    /**
     * Set Bash options for scripts.
     * <br>
     * <ul><li>set -e: exit on failed command</li>
     * <li>set -u: exit on undefined variable --
     *  we don't need this for Bashpile generated code but there may be `source`d code.</li>
     * <li>set -o pipefail: exit immediately when a command in a pipeline fails.</li>
     * <li>set -o posix: Posix mode -- we need this so that all subshells inherit the -eu options.</li></ul>
     *
     * @see <a href=https://unix.stackexchange.com/a/23099">Q & A </a>
     * @return The Strict Mode header
     */
    @Override
    public @Nonnull Translation strictModeHeader() {
        String strictMode = """
                set -euo pipefail -o posix
                export IFS=$'\\n\\t'
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

    @Override
    public Translation expressionStatement(@Nonnull final BashpileParser.ExpressionStatementContext ctx) {
        final Translation expr = visitor.visit(ctx.expression()).add(NEWLINE);
        final Translation comment = toLineTranslation(
                "# expression statement, Bashpile line %d\n".formatted(lineNumber(ctx)));
        final Translation subcomment =
                toLineTranslation(expr.hasPreamble() ? "## expression statement body\n" : "");
        // order is: comment, preamble, subcomment, expr
        final Translation exprStatement = subcomment.add(expr).mergePreamble();
        return comment.add(exprStatement).type(expr.type()).typeMetadata(expr.typeMetadata());
    }

    @Override
    public @Nonnull Translation assignmentStatement(@Nonnull final BashpileParser.AssignmentStatementContext ctx) {
        // add this variable to the type map
        final String variableName = ctx.typedId().Id().getText();
        final Type type = Type.valueOf(ctx.typedId().Type().getText().toUpperCase());
        typeStack.putVariableType(variableName, type, lineNumber(ctx));

        // visit the right hand expression
        final boolean exprExists = ctx.expression() != null;
        final Translation exprTranslation = exprExists
                ? visitor.visit(ctx.expression())
                : Translation.EMPTY_TRANSLATION;
        assertTypesMatch(type, exprTranslation.type(), ctx.typedId().Id().getText(), lineNumber(ctx));

        // create translations
        final Translation comment = toLineTranslation(
                "# assign statement, Bashpile line %d\n".formatted(lineNumber(ctx)));
        final Translation subcomment =
                toLineTranslation(exprTranslation.hasPreamble() ? "## assign statement body\n" : "");
        final Translation varDecl = toLineTranslation(getLocalText() + variableName + "\n");
        // merge expr into the assignment
        final String assignmentBody = exprExists ? "%s=%s\n".formatted(variableName, exprTranslation.body()) : "";
        final Translation assignment = toParagraphTranslation(assignmentBody).appendPreamble(exprTranslation.preamble());

        // order is comment, preamble, subcomment, variable declaration, assignment
        final Translation subcommentToAssignment = subcomment.add(varDecl).add(assignment);
        return comment.add(subcommentToAssignment.mergePreamble()).type(NA).typeMetadata(NORMAL);
    }

    @Override
    public @Nonnull Translation reassignmentStatement(@Nonnull final BashpileParser.ReassignmentStatementContext ctx) {
        // get name and type
        final String variableName = ctx.Id().getText();
        final Type expectedType = typeStack.getVariableType(variableName);
        if (expectedType.isNotFound()) {
            throw new TypeError(variableName + " has not been declared", lineNumber(ctx));
        }

        // get expression and it's type
        final Translation exprTranslation = visitor.visit(ctx.expression());
        final Type actualType = exprTranslation.type();
        Asserts.assertTypesMatch(expectedType, actualType, variableName, lineNumber(ctx));

        // create translations
        final Translation comment = toLineTranslation(
                "# reassign statement, Bashpile line %d\n".formatted(lineNumber(ctx)));
        final Translation subcomment =
                toLineTranslation(exprTranslation.hasPreamble() ? "## reassignment statement body\n" : "");
        // merge exprTranslation into reassignment
        final String reassignmentBody = "%s%s=%s\n".formatted(
                getLocalText(true), variableName, exprTranslation.body());
        final Translation reassignment =
                toLineTranslation(reassignmentBody).appendPreamble(exprTranslation.preamble());

        // order is: comment, preamble, subcomment, reassignment
        final Translation preambleToReassignment = subcomment.add(reassignment).mergePreamble();
        return comment.add(preambleToReassignment).type(NA).typeMetadata(NORMAL);
    }

    @Override
    public @Nonnull Translation printStatement(@Nonnull final BashpileParser.PrintStatementContext ctx) {
        // guard
        final BashpileParser.ArgumentListContext argList = ctx.argumentList();
        if (argList == null) {
            return toLineTranslation("echo\n");
        }

        // body
        try (final var ignored = new LevelCounter(PRINT_LABEL)) {
            final Translation comment = toLineTranslation(
                    "# print statement, Bashpile line %d\n".formatted(lineNumber(ctx)));
            final Translation arguments = argList.expression().stream()
                    .map(visitor::visit)
                    .map(tr -> tr.isInlineOrSubshell() && inCommandSubstitution() ? unnest(tr) : tr)
                    .map(tr -> tr.body("echo %s\n".formatted(tr.body())))
                    .reduce(Translation::add)
                    .orElseThrow();
            final Translation subcomment =
                    toLineTranslation(arguments.hasPreamble() ? "## print statement body\n" : "");
            return comment.add(subcomment.add(arguments).mergePreamble());
        }
    }

    @Override
    public @Nonnull Translation functionForwardDeclarationStatement(
            @Nonnull final BashpileParser.FunctionForwardDeclarationStatementContext ctx) {
        final ParserRuleContext functionDeclCtx = getFunctionDeclCtx(visitor, ctx);
        try (var ignored = new LevelCounter(FORWARD_DECL_LABEL)) {
            final Translation comment = toLineTranslation(
                    "# function forward declaration, Bashpile line %d\n".formatted(lineNumber(ctx)));
            final Translation hoistedFunctionText = toParagraphTranslation(visitor.visit(functionDeclCtx).body());
            return comment.add(hoistedFunctionText);
        } finally {
            foundForwardDeclarations.add(ctx.typedId().Id().getText());
        }
    }

    @Override
    public @Nonnull Translation functionDeclarationStatement(
            @Nonnull final BashpileParser.FunctionDeclarationStatementContext ctx) {
        // avoid translating twice if was part of a forward declaration
        final String functionName = ctx.typedId().Id().getText();
        if (foundForwardDeclarations.contains(functionName)) {
            return Translation.EMPTY_TRANSLATION;
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

        try (var ignored = new LevelCounter(BLOCK_LABEL); var ignored2 = typeStack.closable()) {

            // register local variable types
            ctx.paramaters().typedId().forEach(
                    x -> typeStack.putVariableType(
                            x.Id().getText(), Type.valueOf(x.Type().getText().toUpperCase()), lineNumber(ctx)));

            // create Translations
            final Translation comment = toLineTranslation(
                    "# function declaration, Bashpile line %d%s\n".formatted(lineNumber(ctx), getHoisted()));
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
            final Stream<ParserRuleContext> contextStream =
                    addContexts(ctx.functionBlock().statement(), ctx.functionBlock().returnPsudoStatement());
            final String blockBody = visitBlock(visitor, contextStream).body();
            final Translation functionDeclaration = toParagraphTranslation("%s () {\n%s%s}\n"
                    .formatted(functionName, assertIsLine(namedParams), assertIsParagraph(blockBody)));
            return comment.add(functionDeclaration);
        }
    }

    @Override
    public @Nonnull Translation anonymousBlockStatement(
            @Nonnull final BashpileParser.AnonymousBlockStatementContext ctx) {
        try (var ignored = new LevelCounter(BLOCK_LABEL); var ignored2 = typeStack.closable()) {
            final Translation comment = toLineTranslation(
                    "# anonymous block, Bashpile line %d%s\n".formatted(lineNumber(ctx), getHoisted()));
            // behind the scenes we need to name the anonymous function
            final String anonymousFunctionName = "anon" + anonBlockCounter++;
            // map of x to x needed for upcasting to parent type
            final Stream<ParserRuleContext> stmtStream = ctx.statement().stream().map(x -> x);
            final String blockBody = visitBlock(visitor, stmtStream).body();
            // define function and then call immediately with no arguments
            final Translation selfCallingAnonymousFunction = toParagraphTranslation("%s () {\n%s}; %s\n"
                    .formatted(anonymousFunctionName, assertIsParagraph(blockBody), anonymousFunctionName));
            return comment.add(selfCallingAnonymousFunction);
        }
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
        assertTypesMatch(functionTypes.returnType(), exprTranslation.type(), functionName, lineNumber(ctx));

        if (!exprExists) {
            return EMPTY_TRANSLATION;
        }

        final Translation comment = toLineTranslation(
                "# return statement, Bashpile line %d%s\n".formatted(lineNumber(ctx), getHoisted()));
        final Translation exprBody = toParagraphTranslation(prependLastLine("echo ", exprTranslation.body()))
                .appendPreamble(exprTranslation.preamble());
        return comment.add(exprBody.mergePreamble());
    }

    // expressions

    @Override
    public Translation typecastExpression(BashpileParser.TypecastExpressionContext ctx) {
        final Type castTo = Type.valueOf(ctx.Type().getText().toUpperCase());
        Translation expression = visitor.visit(ctx.expression());
        final TypeError typecastError = new TypeError(
                "Casting %s to %s is not supported".formatted(expression.type(), castTo), lineNumber(ctx));
        // double switch -- first is for the type we're casting from, the second is for the type we're casting to
        switch (expression.type()) {
            case BOOL -> {
                switch (castTo) {
                    case BOOL -> {}
                    case INT -> expression = expression.body(expression.body().equals("true") ? "1" : "0");
                    case FLOAT ->
                            expression = expression.body(expression.body().equals("true") ? "1.0" : "0.0");
                    case STR -> expression = expression.quoteBody();
                    default -> throw typecastError;
                }
            }
            case INT -> {
                switch (castTo) {
                    case BOOL -> expression = expression.body(!expression.body().equals("0") ? "true" : "false");
                    case INT, FLOAT -> {}
                    case STR -> expression = expression.quoteBody();
                    default -> throw typecastError;
                }
            }
            case FLOAT -> {
                BigDecimal expressionValue;
                try {
                    expressionValue = new BigDecimal(expression.body());
                } catch (final NumberFormatException e) {
                    throw new UserError(
                            "Couldn't parse %s to a FLOAT".formatted(expression.body()), lineNumber(ctx));
                }
                switch (castTo) {
                    case BOOL -> expression = expression.body(expressionValue.compareTo(BigDecimal.ZERO) != 0
                            ? "true"
                            : "false");
                    case INT -> expression = expression.body(expressionValue.toBigInteger().toString());
                    case FLOAT -> {}
                    case STR -> expression = expression.quoteBody();
                    default -> throw typecastError;
                }
            }
            case STR -> {
                switch (castTo) {
                    case BOOL -> {
                        expression = expression.unquoteBody();
                        if (!expression.body().equalsIgnoreCase("true")
                                && !expression.body().equalsIgnoreCase("false")) {
                            throw new TypeError("""
                                Could not cast STR to BOOL.  Only 'true' and 'false' allowed.  Text was %s."""
                                    .formatted(expression.body()), lineNumber(ctx));
                        }
                        expression = expression.body(expression.body().toLowerCase());
                    }
                    case INT -> {
                        // no automatic rounding for things like `"2.5":int`
                        expression = expression.unquoteBody();
                        final Type foundType = Type.parseNumberString(expression.body());
                        if (!INT.equals(foundType)) {
                            throw new TypeError("""
                                Could not cast FLOAT value in STR to INT.  Try casting to float first.  Text was %s."""
                                    .formatted(expression.body()), lineNumber(ctx));
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
                                    .formatted(expression.body()), lineNumber(ctx));
                        }
                    }
                    case STR -> {}
                    default -> throw typecastError;
                }
            }
            case UNKNOWN -> {
                switch (castTo) {
                    case BOOL, INT, FLOAT, STR -> {}
                    default -> throw typecastError;
                }
            }
            default -> throw typecastError;
        }
        expression = expression.type(castTo);
        return expression;
    }

    @Override
    public @Nonnull Translation calculationExpression(@Nonnull final BashpileParser.CalculationExpressionContext ctx) {
        // get the child translations
        List<Translation> childTranslations;
        try (var ignored = new LevelCounter(CALC_LABEL)) {
            childTranslations = ctx.children.stream()
                    .map(visitor::visit)
                    // if we have a nested command substitution, then unnest
                    .map(tr -> tr.isInlineOrSubshell() && inCommandSubstitution() ? unnest(tr) : tr)
                    .collect(Collectors.toList());
        }

        // child translations in the format of 'expr operator expr', so we are only interested in the first and last
        final Translation first = childTranslations.get(0);
        final Translation second = getLast(childTranslations);
        // check for nested calc call
        if (LevelCounter.in(CALC_LABEL) && areNumberExpressions(first, second)) {
            return toTranslation(childTranslations.stream(), Type.NUMBER, NORMAL);
        // types section
        } else if (areStringExpressions(first, second)) {
            final String op = ctx.op.getText();
            Asserts.assertEquals("+", op, "Only addition is allowed on Strings, but got " + op);
            return toTranslation(Stream.of(first, second), STR, NORMAL);
        } else if (areNumberExpressions(first, second)) {
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
        Asserts.assertTypesMatch(expectedTypes.parameterTypes(), actualTypes, functionName, lineNumber(ctx));

        // extract argText and preambles from argumentTranslations
        // empty list or ' arg1Text arg2Text etc.'
        String argText = "";
        if (hasArgs) {
            argText = " " + argumentTranslations.stream()
                    .map(Translation::body).collect(Collectors.joining(" "));
        }
        final Translation preambles = argumentTranslations.stream().reduce(Translation::add).orElse(EMPTY_TRANSLATION);

        // lookup return type of this function
        final Type retType = typeStack.getFunctionTypes(id).returnType();

        // suppress output if we are a top-level statement
        // this covers the case of calling a str function without using the string
        final boolean topLevelStatement = !in(CALC_LABEL) && !in(PRINT_LABEL);
        if (topLevelStatement) {
            return new Translation(preambles.preamble(), id + argText + " >/dev/null", retType, NORMAL);
        } // else return an inline (command substitution)
        final String text = "$(%s%s)".formatted(id, argText);
        return new Translation(preambles.preamble(), text, retType, INLINE);
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
        Translation contentsTranslation = toTranslation(contentsStream, UNKNOWN, NORMAL).unescapeBody();
        if (LevelCounter.inCommandSubstitution()) {
            // then wrap in command substitution and unnest as needed
            contentsTranslation = contentsTranslation.body("$(%s)".formatted(contentsTranslation.body()));
            for (int i = 0; i < LevelCounter.getCommandSubstitution(); i++) {
                contentsTranslation = unnest(contentsTranslation);
            }
        } else if (LevelCounter.in(PRINT_LABEL)) {
            contentsTranslation = contentsTranslation.body("$(%s)".formatted(contentsTranslation.body()));
        } // else top level -- no additional processing needed
        return contentsTranslation;
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

    private static @Nonnull String getHoisted() {
        return LevelCounter.in(FORWARD_DECL_LABEL) ? " (hoisted)" : "";
    }

    /**
     * Subshell errored exit codes are ignored in Bash despite all configurations.
     * This workaround explicitly propagates errored exit codes.
     * Unnests one level.
     *
     * @param tr The base translation.
     * @return A Translation where the preamble is <code>tr</code>'s body and the work-around.
     * The body is a Command Substitution of a created variable
     * that holds the results of executing <code>tr</code>'s body.
     */
    private Translation unnest(@Nonnull final Translation tr) {
        // check input
        assertNoMatch(tr.body(), GENERATED_VARIABLE_NAME);

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
        return tr.appendPreamble(preambles.body()).body("${%s}".formatted(subshellReturn));
    }
    
    /** Get the Bashpile script linenumber that ctx is found in. */
    private int lineNumber(@Nonnull final ParserRuleContext ctx) {
        return ctx.start.getLine();
    }

}
