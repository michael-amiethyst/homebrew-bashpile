package com.bashpile.engine;

import com.bashpile.Asserts;
import com.bashpile.BashpileParser;
import com.bashpile.engine.strongtypes.FunctionTypeInfo;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.engine.strongtypes.TypeStack;
import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bashpile.AntlrUtils.*;
import static com.bashpile.Asserts.assertTextBlock;
import static com.bashpile.Asserts.assertTextLine;
import static com.bashpile.engine.LevelCounter.*;
import static com.bashpile.engine.Translation.toStringTranslation;
import static com.bashpile.engine.strongtypes.MetaType.NORMAL;
import static com.bashpile.engine.strongtypes.MetaType.SUBSHELL_SUBSTITUTION;
import static com.google.common.collect.Iterables.getLast;

/**
 * Translates to Bash5 with four spaces as a tab.
 */
public class BashTranslationEngine implements TranslationEngine {

    // static variables

    public static final String TAB = "    ";

    // static methods

    private static @Nonnull String getLocalText() {
        return getLocalText(false);
    }
    private static @Nonnull String getLocalText(final boolean reassignment) {
        final boolean indented = getIndent() > 0;
        if (indented && !reassignment) {
            return "local ";
        } else if (indented) { // and a reassignment
            return "";
        } else { // not indented
            return "export ";
        }
    }

    private static @Nonnull String getHoisted() {
        return LevelCounter.in(FORWARD_DECL) ? " (hoisted)" : "";
    }

    private static void append(@Nonnull final AtomicReference<String> strRef, @Nonnull final String toAppend) {
        final String appended = strRef.get() + toAppend;
        strRef.set(appended);
    }

    // instance variables

    private final TypeStack typeStack = new TypeStack();

    private BashpileVisitor visitor;

    /** We need to name the anonymous blocks, anon0, anon1, anon2, etc.  We keep that counter here. */
    private int anonBlockCounter = 0;

    /** All the functions hoisted so far, so we can ensure we don't emit them twice */
    private final Set<String> foundForwardDeclarations = new HashSet<>();

    /** put variable into ${}, e.g. "var" becomes "${var}" */
    private final Function<ParseTree, Translation> translateIdsOrVisit = parseTree -> {
        if (parseTree instanceof BashpileParser.IdExprContext ctx) {
            // return `${varName}` syntax with the previously declared type of the variable
            final String variableName = ctx.ID().getText();
            final Type type = typeStack.getVariableType(variableName);
            return new Translation("${%s}".formatted(ctx.getText()), type, NORMAL);
        } // else
        return visitor.visit(parseTree);
    };

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
        return toStringTranslation("# strict mode header\n%s".formatted(strictMode));
    }

    @Override
    public @Nonnull Translation importsHeaders() {
        // stub
        String text = "";
        return toStringTranslation(text);
    }

    // statement translations

    @Override
    public @Nonnull Translation assignmentStatement(@Nonnull final BashpileParser.AssignStmtContext ctx) {
        // add this variable to the type map
        final String variableName = ctx.typedId().ID().getText();
        final Type type = Type.valueOf(ctx.typedId().TYPE().getText().toUpperCase());
        typeStack.putVariableType(variableName, type);

        // visit the right hand expression
        Translation exprTranslation = visitor.visit(ctx.expr());
        Asserts.assertTypesMatch(type, exprTranslation.type(), ctx.typedId().ID().getText(), ctx.start.getLine());

        // create translation
        final String lineComment = "# assign statement, Bashpile line %d".formatted(ctx.start.getLine());
        final String value = exprTranslation.text();
        return new Translation(
                "%s\n%s%s=%s\n".formatted(lineComment, getLocalText(), variableName, value),
                Type.NA, NORMAL);
    }

    @Override
    public @Nonnull Translation reassignmentStatement(@Nonnull final BashpileParser.ReAssignStmtContext ctx) {
        // get name and type
        final String variableName = ctx.ID().getText();
        final Type expectedType = typeStack.getVariableType(variableName);
        if (expectedType.isNotFound()) {
            throw new TypeError(variableName + " has not been declared");
        }

        // get expression and it's type
        final Translation exprTranslation = visitor.visit(ctx.expr());
        final Type actualType = exprTranslation.type();
        Asserts.assertTypesMatch(expectedType, actualType, variableName, ctx.start.getLine());

        // create translation
        final String lineComment = "# reassign statement, Bashpile line %d".formatted(ctx.start.getLine());
        final String value = exprTranslation.text();
        return new Translation(
                "%s\n%s%s=%s\n".formatted(lineComment, getLocalText(true), variableName, value),
                Type.NA, NORMAL);
    }

    @Override
    public @Nonnull Translation printStatement(@Nonnull final BashpileParser.PrintStmtContext ctx) {
        final String lineComment = "# print statement, Bashpile line %d".formatted(ctx.start.getLine());
        final BashpileParser.ArglistContext argList = ctx.arglist();
        if (argList == null || argList.isEmpty()) {
            return toStringTranslation("echo\n");
        }
        final String printText = ("%s\n%s\n").formatted(lineComment, argList.expr().stream()
                .map(translateIdsOrVisit)
                .map(tr -> {
                    if (tr.isNotSubshell()) {
                        return "echo " + tr.text();
                    }
                    // we have a subshell -- we need to handle the exit codes and pass them on in case of error
                    return """
                            __bp_textReturn=%s
                            __bp_exitCode=$?
                            if [ "$__bp_exitCode" -ne 0 ]; then exit "$__bp_exitCode"; fi
                            echo "$__bp_textReturn";""".formatted(tr.text());
                })
                .collect(Collectors.joining(" ")));
        return toStringTranslation(printText);
    }

    @Override
    public @Nonnull Translation functionForwardDeclStatement(
            @Nonnull final BashpileParser.FunctionForwardDeclStmtContext ctx) {
        final ParserRuleContext functionDeclCtx = getFunctionDeclCtx(visitor, ctx);
        try (LevelCounter ignored = new LevelCounter(FORWARD_DECL)) {
            final String lineComment =
                    "# function forward declaration, Bashpile line %d".formatted(ctx.start.getLine());
            final String hoistedFunctionText = visitor.visit(functionDeclCtx).text();
            assertTextBlock(hoistedFunctionText);
            final String ret = "%s\n%s".formatted(lineComment, hoistedFunctionText);
            return toStringTranslation(ret);
        } finally {
            foundForwardDeclarations.add(ctx.typedId().ID().getText());
        }
    }

    @Override
    public @Nonnull Translation functionDeclStatement(@Nonnull final BashpileParser.FunctionDeclStmtContext ctx) {
        // avoid translating twice if was part of a forward declaration
        final String functionName = ctx.typedId().ID().getText();
        if (foundForwardDeclarations.contains(functionName)) {
            return Translation.empty;
        }

        // check for double declaration
        if (typeStack.containsFunction(functionName)) {
            throw new UserError(functionName + " was declared twice (function overloading is not supported)");
        }

        // regular processing -- no forward declaration

        // register function param types and return type
        final List<Type> typeList = ctx.paramaters().typedId()
                .stream().map(Type::valueOf).collect(Collectors.toList());
        final Type retType = Type.valueOf(ctx.typedId().TYPE().getText().toUpperCase());
        typeStack.putFunctionTypes(functionName, new FunctionTypeInfo(typeList, retType));

        // create block
        String block;
        try (LevelCounter ignored = new LevelCounter(BLOCK)) {
            typeStack.push();

            // register local variable types
            ctx.paramaters().typedId().forEach(
                    x -> typeStack.putVariableType(x.ID().getText(), Type.valueOf(x.TYPE().getText().toUpperCase())));

            // handles nested blocks
            final AtomicInteger i = new AtomicInteger(1);
            // the empty string or ...
            final String namedParams = ctx.paramaters().typedId().isEmpty() ? "" :
                    // local var1=$1; local var2=$2; etc
                    "%s%s\n".formatted(TAB, ctx.paramaters().typedId().stream()
                                    .map(BashpileParser.TypedIdContext::ID)
                                    .map(visitor::visit)
                                    .map(Translation::text)
                                    .map(str -> "local %s=$%s;".formatted(str, i.getAndIncrement()))
                                    .collect(Collectors.joining(" ")));
            assertTextLine(namedParams);
            final Stream<ParserRuleContext> contextStream = addContexts(ctx.block().stmt(), ctx.block().returnRule());
            final String blockText = visitBlock(visitor, contextStream).text();
            assertTextBlock(blockText);
            final String functionComment = "# function declaration, Bashpile line %d%s"
                    .formatted(ctx.start.getLine(), getHoisted());
            block = "%s\n%s () {\n%s%s}\n"
                    .formatted(functionComment, functionName, namedParams, blockText);
        } finally {
            typeStack.pop();
        }
        assertTextBlock(block);
        return toStringTranslation(block);
    }

    @Override
    public @Nonnull Translation anononymousBlockStatement(@Nonnull final BashpileParser.AnonBlockStmtContext ctx) {
        String block;
        try (LevelCounter ignored = new LevelCounter(BLOCK)) {
            typeStack.push();
            // behind the scenes we need to name the anonymous function
            final String anonymousFunctionName = "anon" + anonBlockCounter++;
            // map of x to x needed for upcasting to parent type
            final Stream<ParserRuleContext> stmtStream = ctx.stmt().stream().map(x -> x);
            final String blockBodyTextBlock = visitBlock(visitor, stmtStream).text();
            assertTextBlock(blockBodyTextBlock);
            final String lineComment = "# anonymous block, Bashpile line %d%s"
                    .formatted(ctx.start.getLine(), getHoisted());
            // define function and then call immediately with no arguments
            block = "%s\n%s () {\n%s}; %s\n"
                    .formatted(lineComment, anonymousFunctionName, blockBodyTextBlock, anonymousFunctionName);
        } finally {
            typeStack.pop();
        }
        return toStringTranslation(block);
    }

    @Override
    public @Nonnull Translation returnRuleStatement(@Nonnull final BashpileParser.ReturnRuleContext ctx) {
        final Translation ret = visitor.visit(ctx.expr());
        // insert echo right at start of last line
        // not a text block, ret.text() does not end in newline
        final String exprText = "# return statement, Bashpile line %d%s\n%s"
                .formatted(ctx.start.getLine(), getHoisted(), ret.text());
        final String[] retLines = exprText.split("\n");
        retLines[retLines.length - 1] = "echo " + retLines[retLines.length - 1];
        final String retText = String.join("\n", retLines) + "\n";
        return toStringTranslation(retText);
    }

    // expressions

    @Override
    public @Nonnull Translation calcExpression(@Nonnull final BashpileParser.CalcExprContext ctx) {
        final Pair<String, List<Translation>> pair = unwindChildren(ctx);
        final String unwoundSubshells = pair.getLeft();
        final List<Translation> childTranslations = pair.getRight();

        Translation first = childTranslations.get(0);
        Translation second = getLast(childTranslations);
        // check for nested calc call
        if (LevelCounter.in(CALC) && isNumberExpression(childTranslations)) {
            final String translationsLine = childTranslations.stream()
                    .map(Translation::text).collect(Collectors.joining(""));
            return new Translation(translationsLine, Type.NUMBER, NORMAL);
        // types section
        } else if (isStringExpression(childTranslations)) {
            final String op = ctx.op.getText();
            Asserts.assertEquals("+", op, "Only addition is allowed on Strings, but got " + op);
            return toStringTranslation(first.text() + second.text());
        } else if (isNumberExpression(childTranslations)) {
            final String translationsString = childTranslations.stream()
                    .map(Translation::text).collect(Collectors.joining(" "));
            return new Translation(
                    "%s$(bc <<< \"%s\")".formatted(unwoundSubshells, translationsString),
                    Type.NUMBER, SUBSHELL_SUBSTITUTION);
        // found no matching types -- error section
        } else if (first.type().equals(Type.NOT_FOUND) || second.type().equals(Type.NOT_FOUND)) {
            throw new UserError("`%s` or `%s` are undefined at Bashpile line %d".formatted(
                    first.text(), second.text(), ctx.start.getLine()));
        } else {
            // throw type error for all others
            throw new TypeError("Incompatible types in calc on Bashpile line %d: %s and %s".formatted(
                    ctx.start.getLine(), first.type(), second.type()));
        }
    }

    /**
     * Bash doesn't support nested subshells so as a work-around we unwind (unnest) the nesting.
     * More details in the comments of the unnestSubshells Function below.
     * Simplified example: if a single child is a subshell `$(cmd)` we return <`var=$(cmd)`, `$var`>
     *
     * @param ctx the parent context.
     * @return a preamble String of the unnested shells assigned to variables and the results of visiting the children
     */
    private Pair<String, List<Translation>> unwindChildren(@Nonnull final ParserRuleContext ctx) {
        List<Translation> childTranslations;
        // subshellVarTextBlock accumulates all the inner shells' results
        final AtomicReference<String> subshellVarTextBlock = new AtomicReference<>("");
        try (LevelCounter ignored = new LevelCounter(CALC)) {
            final AtomicInteger varTextCount = new AtomicInteger(0);

            // this is a work-around for nested sub-shells
            // we run the inner sub-shell and put the results into a variable,
            // and put the variable into the outer sub-shell
            final Function<Translation, Translation> unnestSubshells = childTranslation -> {
                if (childTranslation.isNotSubshell()) {
                    return childTranslation;
                } else {
                    // create an assignment for later, store in subshellVarTextBlock
                    final String varName = "__bp_%d".formatted(varTextCount.getAndIncrement());
                    // last %s is a subshell
                    final String assignString = "%s %s=%s\n"
                            .formatted(getLocalText(), varName, childTranslation.text());
                    append(subshellVarTextBlock, assignString);

                    // create our translation as ${varName}
                    return new Translation("${%s}".formatted(varName), Type.NUMBER, NORMAL);
                }
            };

            childTranslations = ctx.children.stream()
                    .map(translateIdsOrVisit)
                    .map(unnestSubshells)
                    .collect(Collectors.toList());
        } // end try-with-resources

        assertTextBlock(subshellVarTextBlock.get());
        return Pair.of(subshellVarTextBlock.get(), childTranslations);
    }

    private boolean isStringExpression(@Nonnull final List<Translation> translations) {
        Asserts.assertEquals(3, translations.size());
        final Translation first = translations.get(0);
        final Translation last = getLast(translations);
        return first.type().isStr() && last.type().isStr();
    }

    private boolean isNumberExpression(@Nonnull final List<Translation> translations) {
        Asserts.assertEquals(3, translations.size());
        final Translation first = translations.get(0);
        final Translation last = getLast(translations);
        return first.type().isNumeric() && last.type().isNumeric();
    }

    @Override
    public Translation parensExpression(@Nonnull final BashpileParser.ParensExprContext ctx) {
        final Translation expr = visitor.visit(ctx.expr());
        // No parens for strings and no parens for numbers not in a calc (e.g. "(((5)))" becomes "5" eventually)
        final String format = expr.type().isNumeric() && LevelCounter.in(CALC) ? "(%s)" : "%s";
        return new Translation(format.formatted(expr.text()), expr.type(), expr.metaType());
    }

    @Override
    public @Nonnull Translation functionCallExpression(@Nonnull final BashpileParser.FunctionCallExprContext ctx) {
        final String id = ctx.ID().getText();

        // check arg types

        // get functionName and a stream creator
        final String functionName = ctx.ID().getText();
        final Supplier<Stream<Translation>> argListTranslationStream =
                () -> ctx.arglist().expr().stream().map(translateIdsOrVisit);
        // get the expected and actual types
        final FunctionTypeInfo expectedTypes = typeStack.getFunctionTypes(functionName);
        final List<Type> actualTypes = ctx.arglist() != null
                ? argListTranslationStream.get().map(Translation::type).collect(Collectors.toList())
                : List.of();
        // assert equals
        Asserts.assertTypesMatch(expectedTypes.parameterTypes(), actualTypes, functionName, ctx.start.getLine());

        // get arguments

        final boolean hasArgs = ctx.arglist() != null;
        // empty list or ' arg1Text arg2Text etc.'
        final String args = hasArgs
                ? " " + argListTranslationStream.get().map(Translation::text).collect(Collectors.joining(" "))
                : "";

        // lookup return type of this function
        final Type retType = typeStack.getFunctionTypes(id).returnType();

        return new Translation("$(%s%s)".formatted(id, args), retType, SUBSHELL_SUBSTITUTION);
    }
}
