package com.bashpile.engine;

import com.bashpile.Asserts;
import com.bashpile.BashpileParser;
import com.bashpile.exceptions.TypeError;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.*;
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
import static com.bashpile.engine.MetaType.NORMAL;
import static com.bashpile.engine.Translation.toStringTranslation;
import static com.bashpile.engine.MetaType.SUBSHELL_SUBSTITUTION;
import static java.util.Objects.requireNonNullElse;

/**
 * Translates to Bash5 with four spaces as a tab.
 */
public class BashTranslationEngine implements TranslationEngine {

    public static final String TAB = "    ";

    private static @Nonnull String getLocalText() {
        return LevelCounter.getIndent() != 0 ? "local" : "export";
    }

    private static @Nonnull String getHoisted() {
        return LevelCounter.in(FORWARD_DECL) ? " (hoisted)" : "";
    }

    private static void append(@Nonnull final AtomicReference<String> strRef, @Nonnull final String toAppend) {
        final String appended = strRef.get() + toAppend;
        strRef.set(appended);
    }

    // TODO make sure this works quickly with large programs (100+ functions)
    // TODO use a call stack instead to implement lexical scoping
    /**
     * A map of function name (ID) to a list of argument types and the return type.  Dynamic scoping
     */
    private final Map<String, FunctionTypeInfo> functionArgumentTypes = HashMap.newHashMap(10);

    // TODO use a call stack instead to implement lexical scoping
    /** Map of all variable types -- dynamic scoping */
    private final Map<String, Type> variableTypes = HashMap.newHashMap(10);

    private BashpileVisitor visitor;

    /** We need to name the anonymous blocks, anon0, anon1, anon2, etc.  We keep that counter here. */
    private int anonBlockCounter = 0;

    /** All the functions hoisted so far, so we can ensure we don't emit them twice */
    private final Set<String> foundForwardDeclarations = new HashSet<>();

    /** prepend $ to variable name, e.g. "var" becomes "$var" */
    private final Function<ParseTree, Translation> translateIdsOrVisit =
            parseTree -> parseTree instanceof BashpileParser.IdExprContext
                    ? new Translation("$" + parseTree.getText(),
                            variableTypes.get(((BashpileParser.IdExprContext) parseTree).ID().getText()), NORMAL)
                    : visitor.visit(parseTree);

    @Override
    public void setVisitor(@Nonnull final BashpileVisitor visitor) {
        this.visitor = visitor;
    }

    /**
     * We need '-o posix' so that all subshells inherit the -eu options.
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
    public @Nonnull Translation imports() {
        String text = "# no imports yet (this is a stub)\n";
        return toStringTranslation(text);
    }

    @Override
    public @Nonnull Translation assign(@Nonnull final BashpileParser.AssignStmtContext ctx) {
        // add this variable to the type map
        final String variableName = ctx.typedId().ID().getText();
        final Type type = Type.valueOf(ctx.typedId().TYPE().getText().toUpperCase());
        variableTypes.put(variableName, type);

        // create translation
        final String lineComment = "# assign statement, Bashpile line %d".formatted(ctx.start.getLine());
        final String value = ctx.expr().getText();
        return new Translation(
                "%s\n%s %s=%s\n".formatted(lineComment, getLocalText(), variableName, value), Type.NA, NORMAL);
    }

    @Override
    public @Nonnull Translation reassign(@Nonnull final BashpileParser.ReAssignStmtContext ctx) {
        final String variableName = ctx.ID().getText();
        if (!variableTypes.containsKey(variableName)) {
            throw new TypeError(variableName + " has not been declared");
        }
        final String lineComment = "# reassign statement, Bashpile line %d".formatted(ctx.start.getLine());
        final String value = ctx.expr().getText();
        return new Translation(
                "%s\n%s %s=%s\n".formatted(lineComment, getLocalText(), variableName, value), Type.NA, NORMAL);
    }

    @Override
    public @Nonnull Translation print(@Nonnull final BashpileParser.PrintStmtContext ctx) {
        final String lineComment = "# print statement, Bashpile line %d".formatted(ctx.start.getLine());
        final String printText = ("%s\n%s\n").formatted(lineComment, ctx.arglist().expr().stream()
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
    public @Nonnull Translation functionForwardDecl(@Nonnull final BashpileParser.FunctionForwardDeclStmtContext ctx) {
        final ParserRuleContext functionDeclCtx = getFunctionDeclCtx(visitor, ctx);
        try (LevelCounter forwardDeclCounter = new LevelCounter(FORWARD_DECL)) {
            forwardDeclCounter.noop();
            final String lineComment = "# function forward declaration, Bashpile line %d".formatted(ctx.start.getLine());
            final String hoistedFunctionText = visitor.visit(functionDeclCtx).text();
            assertTextBlock(hoistedFunctionText);
            final String ret = "%s\n%s".formatted(lineComment, hoistedFunctionText);
            return toStringTranslation(ret);
        } finally {
            foundForwardDeclarations.add(ctx.typedId().ID().getText());
        }
    }

    @Override
    public @Nonnull Translation functionDecl(@Nonnull final BashpileParser.FunctionDeclStmtContext ctx) {
        // avoid translating twice if was part of a forward declaration
        if (foundForwardDeclarations.contains(ctx.typedId().ID().getText())) {
            return Translation.empty;
        }

        // regular processing -- no forward declaration

        // register function param types and return type
        final String functionName = ctx.typedId().ID().getText();
        final List<Type> typeList = ctx.paramaters().typedId()
                .stream().map(Type::valueOf).collect(Collectors.toList());
        final Type retType = Type.valueOf(ctx.typedId().TYPE().getText().toUpperCase());
        functionArgumentTypes.put(functionName, new FunctionTypeInfo(typeList, retType));

        // register local variable types
        ctx.paramaters().typedId().forEach(
                x -> variableTypes.put(x.ID().getText(), Type.valueOf(x.TYPE().getText().toUpperCase())));

        // create block
        String block;
        try (LevelCounter counter = new LevelCounter(BLOCK)) {
            counter.noop();
            // handles nested blocks
            final String endIndent = TAB.repeat(LevelCounter.getIndentMinusOne());
            final AtomicInteger i = new AtomicInteger(1);
            // the empty string or ...
            final String namedParams = ctx.paramaters().typedId().isEmpty() ? "" :
                    // local var1=$1; local var2=$2; etc
                    "%s%s\n".formatted(TAB.repeat(LevelCounter.getIndent()),
                            ctx.paramaters().typedId().stream()
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
            block = "%s\n%s () {\n%s%s%s}\n"
                    .formatted(functionComment, ctx.typedId().ID().getText(), namedParams, blockText, endIndent);
        }
        assertTextBlock(block);
        return toStringTranslation(block);
    }

    @Override
    public @Nonnull Translation anonBlock(@Nonnull final BashpileParser.AnonBlockStmtContext ctx) {
        String block;
        try (LevelCounter counter = new LevelCounter(BLOCK)) {
            counter.noop();
            final String label = "anon" + anonBlockCounter++;
            // map of x to x needed for upcasting to parent type
            final Stream<ParserRuleContext> stmtStream = ctx.stmt().stream().map(x -> x);
            final String blockBodyTextBlock = visitBlock(visitor, stmtStream).text();
            assertTextBlock(blockBodyTextBlock);
            final String lineComment = "# anonymous block, Bashpile line %d%s"
                    .formatted(ctx.start.getLine(), getHoisted());
            // define function and then call immediately with no arguments
            block = "%s\n%s () {\n%s}; %s\n"
                    .formatted(lineComment, label, blockBodyTextBlock, label);
        }
        return toStringTranslation(block);
    }

    @Override
    public @Nonnull Translation returnRule(@Nonnull final BashpileParser.ReturnRuleContext ctx) {
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
    public @Nonnull Translation calc(@Nonnull final ParserRuleContext ctx) {
        // prepend $ to variable name, e.g. "var" becomes "$var"
        String text;
        final AtomicReference<String> subshellVarText = new AtomicReference<>("");
        try (LevelCounter counter = new LevelCounter(CALC)) {
            counter.noop();
            final AtomicInteger varTextCount = new AtomicInteger(0);
            text = ctx.children.stream()
                    .map(translateIdsOrVisit)
                    .map(translation -> {
                        if (translation.isNotSubshell()) {
                            return translation.text();
                        } else {
                            // need to unpack the recursion
                            final String varName = "__bp_%d".formatted(varTextCount.getAndIncrement());
                            final String assignString = "%s %s=%s\n"
                                    .formatted(getLocalText(), varName, translation.text());
                            append(subshellVarText, assignString);
                            return "$" + varName;
                        }
                    })
                    .collect(Collectors.joining());
        }
        assertTextBlock(subshellVarText.get());
        return LevelCounter.in(CALC) ?
                toStringTranslation(text)
                : new Translation(
                        "%s$(bc <<< \"%s\")".formatted(subshellVarText, text), Type.NUMBER, SUBSHELL_SUBSTITUTION);
    }

    @Override
    public @Nonnull Translation functionCall(@Nonnull final BashpileParser.FunctionCallExprContext ctx) {
        final String id = ctx.ID().getText();

        // check arg types

        // get functionName and a stream creator
        final String functionName = ctx.ID().getText();
        final Supplier<Stream<Translation>> argListTranslationStream =
                () -> ctx.arglist().expr().stream().map(translateIdsOrVisit);
        // get the expected and actual types
        final FunctionTypeInfo expectedTypes =
                (FunctionTypeInfo) requireNonNullElse(functionArgumentTypes.get(functionName), Pair.emptyArray());
        final List<Type> actualTypes = ctx.arglist() != null
                ? argListTranslationStream.get().map(Translation::type).collect(Collectors.toList())
                : List.of();
        // assert equals
        Asserts.assertTypesMatch(expectedTypes.parameterTypes(), actualTypes, ctx);

        // get arguments

        final boolean hasArgs = ctx.arglist() != null;
        // empty list or ' arg1Text arg2Text etc'
        final String args = hasArgs
                ? " " + argListTranslationStream.get().map(Translation::text).collect(Collectors.joining(" "))
                : "";

        // lookup return type of this function
        final Type retType = functionArgumentTypes.get(id).returnType();

        return new Translation("$(%s%s)".formatted(id, args), retType, SUBSHELL_SUBSTITUTION);
    }
}
