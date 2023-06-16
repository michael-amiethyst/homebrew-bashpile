package com.bashpile.engine;

import com.bashpile.BashpileParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bashpile.engine.Translation.toStringTranslation;

/**
 * Translates to Bash5 with four spaces as a tab.
 */
public class BashTranslationEngine implements TranslationEngine {

    public static final String TAB = "    ";

    private BashpileVisitor visitor;

    private int anonBlockCounter = 0;
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
                set -euo pipefail -o posix
                export IFS=$'\\n\\t'
                """);
    }

    @Override
    public Translation assign(String variable, String value) {
        String localText = LevelCounter.getIndent() != 0 ? "local" : "export";
        return toStringTranslation("%s %s=%s\n".formatted(localText, variable, value));
    }

    @Override
    public Translation print(BashpileParser.PrintStmtContext ctx) {
        String printText = "%s\n".formatted(ctx.arglist().expr().stream()
                .map(translateIdsOrVisit)
                .map(tr -> !tr.isSubshell() ?
                        "echo " + tr.text()
                        // bash chicanery to prevent loss of error exit code in a line like `echo $(badCommand)`
                        : """
                        __textReturn=%s
                        __exitCode=$?
                        if [ "$__exitCode" -ne 0 ]; then exit "$__exitCode"; fi
                        echo "$__textReturn";""".formatted(tr.text()))
                .collect(Collectors.joining(" ")));
        return toStringTranslation(printText);
    }

    @Override
    public Translation functionDecl(BashpileParser.FunctionDeclStmtContext ctx) {
        String block;
        try (LevelCounter counter = new LevelCounter("block")) {
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
            String blockText = visitBlock(addContexts(ctx.block().stmt(), ctx.block().returnRule())).text();
            block = "%s () {\n%s%s%s}\n".formatted(ctx.ID().getText(), namedParams, blockText, endIndent);
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
        try (LevelCounter counter = new LevelCounter("block")) {
            counter.noop();
            String label = "anon" + anonBlockCounter++;
            String endIndent = TAB.repeat(LevelCounter.getIndentMinusOne());
            // explicit cast needed
            Stream<ParserRuleContext> stmtStream = ctx.stmt().stream().map(x -> x);
            block = "%s () {\n%s%s}; %s\n".formatted(label, visitBlock(stmtStream).text(), endIndent, label);
        }
        return toStringTranslation(block);
    }

    @Override
    public Translation returnRule(BashpileParser.ReturnRuleContext ctx) {
        String ret = visitor.visit(ctx.expr()).text();
        return toStringTranslation("echo %s\n".formatted(ret));
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

    @Override
    public Translation calc(ParserRuleContext ctx) {
        // prepend $ to variable name, e.g. "var" becomes "$var"
        final String calcLabel = "calc";
        String text;
        try (LevelCounter counter = new LevelCounter(calcLabel)) {
            counter.noop();
            text = ctx.children.stream()
                    .map(translateIdsOrVisit)
                    .map(Translation::text)
                    .collect(Collectors.joining());
        }
        return LevelCounter.in(calcLabel) ?
                toStringTranslation(text)
                : new Translation("$(bc <<< \"%s\")".formatted(text), TranslationType.SUBSHELL_SUBSTITUTION);
    }

    @Override
    public Translation functionCall(BashpileParser.FunctionCallExprContext ctx) {
        String id = ctx.ID().getText();
        boolean hasArgs = ctx.arglist() != null;
        // empty list or ' arg1Text arg2Text etc'
        String args = hasArgs ? " " + ctx.arglist().expr().stream()
                .map(RuleContext::getText)
                .collect(Collectors.joining(" "))
                : "";
        return new Translation("$(%s%s)".formatted(id, args), TranslationType.SUBSHELL_SUBSTITUTION);
    }
}
