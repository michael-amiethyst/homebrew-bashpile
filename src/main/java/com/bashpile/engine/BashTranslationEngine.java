package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.exceptions.BashpileUncheckedException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bashpile.Asserts.assertTextBlock;
import static com.bashpile.Asserts.assertTextLine;
import static com.bashpile.engine.LevelCounter.*;
import static com.bashpile.engine.Translation.toStringTranslation;
import static com.bashpile.engine.TranslationType.SUBSHELL_SUBSTITUTION;

/**
 * Translates to Bash5 with four spaces as a tab.
 */
public class BashTranslationEngine implements TranslationEngine {

    public static final String TAB = "    ";

    private static String getLocalText() {
        return LevelCounter.getIndent() != 0 ? "local" : "export";
    }

    private static String getHoisted() {
        return LevelCounter.in(FORWARD_DECL) ? " (hoisted)" : "";
    }

    private BashpileVisitor visitor;

    /** We need to name the anonymous blocks, anon0, anon1, anon2, etc.  We keep that counter here. */
    private int anonBlockCounter = 0;

    private final Set<String> foundForwardDeclarations = new HashSet<>();

    /** prepend $ to variable name, e.g. "var" becomes "$var" */
    private final Function<ParseTree, Translation> translateIdsOrVisit =
            x -> x instanceof BashpileParser.IdExprContext ?
                    toStringTranslation("$" + x.getText())
                    : visitor.visit(x);

    @Override
    public void setVisitor(BashpileVisitor visitor) {
        this.visitor = visitor;
    }

    @Override
    public Translation strictMode() {
        // we need '-o posix' so that all subshells inherit the -eu options
        // see https://unix.stackexchange.com/a/23099
        return toStringTranslation("""
                # strict mode header
                set -euo pipefail -o posix
                export IFS=$'\\n\\t'
                """);
    }

    @Override
    public Translation assign(BashpileParser.AssignStmtContext ctx) {
        // TODO implement final keyword with vigor
        String lineComment = "# assign statement, Bashpile line %d".formatted(ctx.start.getLine());
        String variable = ctx.ID().getText();
        String value = ctx.expr().getText();
        return toStringTranslation("%s\n%s %s=%s\n".formatted(lineComment, getLocalText(), variable, value));
    }

    @Override
    public Translation print(BashpileParser.PrintStmtContext ctx) {
        String lineComment = "# print statement, Bashpile line %d".formatted(ctx.start.getLine());
        String printText = ("%s\n%s\n").formatted(lineComment, ctx.arglist().expr().stream()
                .map(translateIdsOrVisit)
                .map(tr -> !tr.isSubshell() ?
                        "echo " + tr.text()
                        // bash chicanery to prevent loss of error exit code in a line like `echo $(badCommand)`
                        : """
                        __bp_textReturn=%s
                        __bp_exitCode=$?
                        if [ "$__bp_exitCode" -ne 0 ]; then exit "$__bp_exitCode"; fi
                        echo "$__bp_textReturn";""".formatted(tr.text()))
                .collect(Collectors.joining(" ")));
        return toStringTranslation(printText);
    }

    @Override
    public Translation functionForwardDecl(BashpileParser.FunctionForwardDeclStmtContext ctx) {
        final ParserRuleContext functionDeclCtx = getFunctionDeclCtx(ctx);
        try (LevelCounter forwardDeclCounter = new LevelCounter(FORWARD_DECL)) {
            forwardDeclCounter.noop();
            String lineComment = "# function forward declaration, Bashpile line %d".formatted(ctx.start.getLine());
            String ret = "%s\n%s".formatted(lineComment, visitor.visit(functionDeclCtx).text());
            return toStringTranslation(ret);
        } finally {
            foundForwardDeclarations.add(ctx.ID().getText());
        }
    }

    /** Helper to {@link #functionDecl(BashpileParser.FunctionDeclStmtContext)} */
    private ParserRuleContext getFunctionDeclCtx(BashpileParser.FunctionForwardDeclStmtContext ctx) {
        final String functionName = ctx.ID().getText();
        Stream<ParserRuleContext> allContexts = stream(visitor.getContextRoot());
        // TODO make sure args match too
        Predicate<ParserRuleContext> namesMatch =
                x -> {
                    boolean isDeclaration = x instanceof BashpileParser.FunctionDeclStmtContext;
                    // is a function declaration and the names match
                    return isDeclaration && ((BashpileParser.FunctionDeclStmtContext) x)
                            .ID().getText().equals(functionName);
                };
        return allContexts
                .filter(namesMatch)
                .findFirst()
                .orElseThrow(
                        () -> new BashpileUncheckedException("No matching function declaration for " + functionName));
    }

    /**
     * Lazy DFS.  Helper to {@link #getFunctionDeclCtx(BashpileParser.FunctionForwardDeclStmtContext).
     *
     * @see <a href="https://stackoverflow.com/questions/26158082/how-to-convert-a-tree-structure-to-a-stream-of-nodes-in-java>Stack Overflow</a>
     * @param parentNode the root.
     * @return Flattened stream of parent nodes' rule context children.
     */
    private Stream<ParserRuleContext> stream(ParserRuleContext parentNode) {
        if (parentNode.getChildCount() == 0) {
            return Stream.of(parentNode);
        } else {
            return Stream.concat(Stream.of(parentNode),
                    parentNode.getRuleContexts(ParserRuleContext.class).stream().flatMap(this::stream));
        }
    }

    @Override
    public Translation functionDecl(BashpileParser.FunctionDeclStmtContext ctx) {
        // avoid translating twice if was part of a forward declaration
        if (foundForwardDeclarations.contains(ctx.ID().getText())) {
            return Translation.empty;
        }

        // regular processing -- no forward declaration
        String block;
        try (LevelCounter counter = new LevelCounter(BLOCK)) {
            counter.noop();
            // handles nested blocks
            String endIndent = TAB.repeat(LevelCounter.getIndentMinusOne());
            AtomicInteger i = new AtomicInteger(1);
            // the empty string or ...
            String namedParams = ctx.paramaters().ID().isEmpty() ? "" :
                    // local var1=$1; local var2=$2; etc
                    "%s%s\n".formatted(TAB.repeat(LevelCounter.getIndent()),
                            ctx.paramaters().ID().stream()
                                    .map(visitor::visit)
                                    .map(str -> "local %s=$%s;".formatted(str.text(), i.getAndIncrement()))
                                    .collect(Collectors.joining(" ")));
            assertTextLine(namedParams);
            String blockText = visitBlock(addContexts(ctx.block().stmt(), ctx.block().returnRule())).text();
            assertTextBlock(blockText);
            String functionComment = "# function declaration, Bashpile line %d%s"
                    .formatted(ctx.start.getLine(), getHoisted());
            block = (functionComment + "\n%s () {\n%s%s%s}\n")
                    .formatted(ctx.ID().getText(), namedParams, blockText, endIndent);
        }
        return toStringTranslation(block);
    }

    /** Concatenates inputs into stream */
    private Stream<ParserRuleContext> addContexts(
            List<BashpileParser.StmtContext> stmts, BashpileParser.ReturnRuleContext ctx) {
        // map of x to x needed for upcasting to parent type
        Stream<ParserRuleContext> stmt = stmts.stream().map(x -> x);
        return Stream.concat(stmt, Stream.of(ctx));
    }

    @Override
    public Translation anonBlock(BashpileParser.AnonBlockStmtContext ctx) {
        String block;
        try (LevelCounter counter = new LevelCounter(BLOCK)) {
            counter.noop();
            String label = "anon" + anonBlockCounter++;
            String endIndent = TAB.repeat(LevelCounter.getIndentMinusOne());
            // map of x to x needed for upcasting to parent type
            Stream<ParserRuleContext> stmtStream = ctx.stmt().stream().map(x -> x);
            String blockBodyTextBlock = visitBlock(stmtStream).text();
            assertTextBlock(blockBodyTextBlock);
            String lineComment = "# anonymous block, Bashpile line %d%s".formatted(ctx.start.getLine(), getHoisted());
            // define function and then call immediately with no arguments
            block = "%s\n%s () {\n%s%s}; %s\n"
                    .formatted(lineComment, label, blockBodyTextBlock, endIndent, label);
        }
        return toStringTranslation(block);
    }

    @Override
    public Translation returnRule(BashpileParser.ReturnRuleContext ctx) {
        Translation ret = visitor.visit(ctx.expr());
        // insert echo right at start of last line
        // not a text block, ret.text() does not end in newline
        String exprText = "# return statement, Bashpile line %d%s\n%s"
                .formatted(ctx.start.getLine(), getHoisted(), ret.text());
        String[] retLines = exprText.split("\n");
        retLines[retLines.length - 1] = "echo " + retLines[retLines.length - 1];
        String retText = String.join("\n", retLines) + "\n";
        return toStringTranslation(retText);
    }

    private Translation visitBlock(Stream<ParserRuleContext> stmtStream) {
        String translationText = stmtStream.map(visitor::visit)
                .map(Translation::text)
                // visit results may be multiline strings, convert to array of single lines
                .map(str -> str.split("\n"))
                // stream the lines, indent each line, then flatten
                .flatMap(lines -> Arrays.stream(lines).sequential().map(s -> TAB + s + "\n"))
                .collect(Collectors.joining());
        return toStringTranslation(translationText);
    }

    // expressions

    @Override
    public Translation calc(ParserRuleContext ctx) {
        // prepend $ to variable name, e.g. "var" becomes "$var"
        String text;
        AtomicReference<String> subshellVarText = new AtomicReference<>("");
        try (LevelCounter counter = new LevelCounter(CALC)) {
            counter.noop();
            AtomicInteger varTextCount = new AtomicInteger(0);
            text = ctx.children.stream()
                    .map(translateIdsOrVisit)
                    .map(translation -> {
                        if (!translation.isSubshell()) {
                            return translation.text();
                        } else {
                            // need to unpack the recursion
                            String varName = "__bp_%d".formatted(varTextCount.getAndIncrement());
                            subshellVarText.set(subshellVarText.get() + "%s %s=%s\n".formatted(
                                    getLocalText(), varName, translation.text()));
                            return "$" + varName;
                        }
                    })
                    .collect(Collectors.joining());
        }
        assertTextBlock(subshellVarText.get());
        return LevelCounter.in(CALC) ?
                toStringTranslation(text)
                : new Translation("%s$(bc <<< \"%s\")".formatted(subshellVarText, text), SUBSHELL_SUBSTITUTION);
    }

    @Override
    public Translation functionCall(BashpileParser.FunctionCallExprContext ctx) {
        String id = ctx.ID().getText();
        boolean hasArgs = ctx.arglist() != null;
        // empty list or ' arg1Text arg2Text etc'
        String args = hasArgs ? " " + ctx.arglist().expr().stream()
                .map(translateIdsOrVisit)
                .map(Translation::text)
                .collect(Collectors.joining(" "))
                : "";
        return new Translation("$(%s%s)".formatted(id, args), SUBSHELL_SUBSTITUTION);
    }
}
