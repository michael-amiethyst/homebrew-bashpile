package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BashTranslationEngine implements TranslationEngine {

    public static final String TAB = "    ";

    private BashpileVisitor visitor;

    private int anonBlockCounter = 0;
    private final Function<ParseTree, String> translateIdsOrVisit =
            x -> x instanceof BashpileParser.IdExprContext ? "$" + x.getText() : visitor.visit(x);

    @Override
    public void setVisitor(BashpileVisitor visitor) {
        this.visitor = visitor;
    }

    @Override
    public String strictMode() {
        return """
                set -euo pipefail
                export IFS=$'\\n\\t'
                """;
    }

    @Override
    public String assign(String variable, String value) {
        String localText = LevelCounter.getIndent() != 0 ? "local" : "export";
        return "%s %s=%s\n".formatted(localText, variable, value);
    }

    @Override
    public String print(BashpileParser.PrintStmtContext ctx) {
        return "echo %s\n".formatted(ctx.arglist().expr().stream()
                .map(translateIdsOrVisit)
                .collect(Collectors.joining(" ")));
    }

    @Override
    public String functionDecl(BashpileParser.FunctionDeclStmtContext ctx) {
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
                                    .map(str -> "local %s=$%s;".formatted(str, i.getAndIncrement()))
                                    .collect(Collectors.joining(" ")));
            String blockText = visitBlock(addContexts(ctx.block().stmt(), ctx.block().returnRule()));
            block = "%s () {\n%s%s%s}\n".formatted(ctx.ID().getText(), namedParams, blockText, endIndent);
        }
        return block;
    }

    /** Concatenates inputs into stream */
    private Stream<ParserRuleContext> addContexts(
            List<BashpileParser.StmtContext> stmts, BashpileParser.ReturnRuleContext ctx) {
        // map of x to x needed for upcasting to parent type
        Stream<ParserRuleContext> stmt = stmts.stream().map(x -> x);
        return Stream.concat(stmt, Stream.of(ctx));
    }

    @Override
    public String anonBlock(BashpileParser.AnonBlockStmtContext ctx) {
        String block;
        try (LevelCounter counter = new LevelCounter("block")) {
            counter.noop();
            String label = "anon" + anonBlockCounter++;
            String endIndent = TAB.repeat(LevelCounter.getIndentMinusOne());
            // explicit cast needed
            Stream<ParserRuleContext> stmtStream = ctx.stmt().stream().map(x -> x);
            block = "%s () {\n%s%s}; %s\n".formatted(label, visitBlock(stmtStream), endIndent, label);
        }
        return block;
    }

    @Override
    public String returnRule(BashpileParser.ReturnRuleContext ctx) {
        String ret = visitor.visit(ctx.expr());
        return "echo %s\n".formatted(ret);
    }

    private String visitBlock(Stream<ParserRuleContext> stmtStream) {
        String indent = TAB.repeat(LevelCounter.getIndent());
        return stmtStream.map(visitor::visit).map(s -> indent + s).collect(Collectors.joining());
    }

    @Override
    public String calc(BashpileParser.CalcExprContext ctx) {
        // prepend $ to variable name, e.g. "var" becomes "$var"
        final String name = "calc";
        String text;
        try (LevelCounter counter = new LevelCounter(name)) {
            counter.noop();
            text = ctx.children.stream().map(translateIdsOrVisit)
                    .collect(Collectors.joining());
        }
        return LevelCounter.in("calc") ? text : "$(bc <<< \"%s\")".formatted(text);
    }

    @Override
    public String functionCall(BashpileParser.FunctionCallExprContext ctx) {
        String id = ctx.ID().getText();
        boolean hasArgs = ctx.arglist() != null;
        // empty list or ' arg1Text arg2Text etc'
        String args = hasArgs ? " " + ctx.arglist().expr().stream()
                .map(RuleContext::getText).collect(Collectors.joining(" "))
                : "";
        return "$(%s%s)".formatted(id, args);
    }
}
