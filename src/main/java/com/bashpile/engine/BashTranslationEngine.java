package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileVisitor;
import org.antlr.v4.runtime.RuleContext;

import java.util.stream.Collectors;

public class BashTranslationEngine implements TranslationEngine {

    public static final String TAB = "    ";

    private BashpileVisitor visitor;

    private int anonBlockCounter = 0;

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
        String localText = LevelCounter.getIndent() != 0 ? "local " : "";
        return "%s%s=%s\n".formatted(localText, variable, value);
    }

    @Override
    public String functionDecl(BashpileParser.FunctionDeclContext ctx) {
        String block;
        try (LevelCounter counter = new LevelCounter("block")) {
            String endIndent = TAB.repeat(LevelCounter.getIndentMinusOne());
            block = "%s () {\n%s%s}\n".formatted(ctx.ID().getText(), visitBlock(ctx.block()), endIndent);
        }
        return block;
    }

    @Override
    public String anonBlock(BashpileParser.AnonBlockContext ctx) {
        String block;
        try (LevelCounter counter = new LevelCounter("block")) {
            String label = "anon" + anonBlockCounter++;
            String endIndent = TAB.repeat(LevelCounter.getIndentMinusOne());
            block = "%s () {\n%s%s}; %s\n".formatted(label, visitBlock(ctx.block()), endIndent, label);
        }
        return block;
    }

    private String visitBlock(BashpileParser.BlockContext ctx) {
        String indent = TAB.repeat(LevelCounter.getIndent());
        return ctx.stat().stream().map(visitor::visit).map(s -> indent + s).collect(Collectors.joining());
    }

    @Override
    public String calc(BashpileParser.CalcContext ctx) {
        // prepend $ to variable name, e.g. "var" becomes "$var"
        final String name = "calc";
        String text;
        try (LevelCounter counter = new LevelCounter(name)) {
            counter.noop();
            text = ctx.children.stream().map(
                            x -> x instanceof BashpileParser.IdContext ? "$" + x.getText() : visitor.visit(x))
                    .collect(Collectors.joining());
        }
        return LevelCounter.in("calc") ? text : "bc <<< \"%s\"\n".formatted(text);
    }

    @Override
    public String functionCall(BashpileParser.FunctionCallContext ctx) {
        return ctx.ID().getText() + " " + ctx.paramaters().expr().stream()
                .map(RuleContext::getText).collect(Collectors.joining(" ")) + "\n";
    }
}
