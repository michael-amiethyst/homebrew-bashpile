package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileVisitor;

import java.util.stream.Collectors;

public class BashTranslationEngine implements TranslationEngine {

    // TODO change to int
    private boolean inBlock = false;

    private int anonBlockCounter = 0;

    @Override
    public String strictMode() {
        return """
                set -euo pipefail
                export IFS=$'\\n\\t'
                """;
    }

    @Override
    public String assign(String variable, String value) {
        String localText = inBlock ? "local " : "";
        return "%s%s=%s\n".formatted(localText, variable, value);
    }

    @Override
    public String functionDecl(BashpileVisitor visitor, BashpileParser.FunctionDeclContext ctx) {
        inBlock = true;
        String block = "%s () {\n%s}\n".formatted(ctx.ID().getText(), visitBlock(ctx.block(), visitor));
        inBlock = false;
        return block;
    }

    @Override
    public String anonBlock(BashpileVisitor visitor, BashpileParser.AnonBlockContext ctx) {
        inBlock = true;
        String label = "anon" + anonBlockCounter++;
        String block = "%s () {\n%s}; %s\n".formatted(label, visitBlock(ctx.block(), visitor), label);
        inBlock = false;
        return block;
    }

    private static String visitBlock(BashpileParser.BlockContext ctx, BashpileVisitor visitor) {
        return ctx.stat().stream().map(visitor::visit).map(s -> "   " + s).collect(Collectors.joining());
    }

    @Override
    public String calc(BashpileVisitor visitor, BashpileParser.CalcContext ctx) {
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
}
