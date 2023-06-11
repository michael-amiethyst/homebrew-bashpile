package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileVisitor;

import java.util.stream.Collectors;

public class BashTranslationEngine implements TranslationEngine {

    private boolean inBlock = false;

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
    public String block(BashpileVisitor visitor, BashpileParser.BlockContext ctx) {
        inBlock = true;
        String block = "anon () {\n" +
                ctx.stat().stream().map(visitor::visit).map(s -> "   " + s).collect(Collectors.joining()) +
                "}; anon\n";
        inBlock = false;
        return block;
    }

    @Override
    public String calc(BashpileParser.CalcContext ctx) {
        // prepend $ to variable name, e.g. "var" becomes "$var"
        String text = ctx.children.stream().map(
                        x -> x instanceof BashpileParser.IdContext ? "$" + x.getText() : x.getText())
                .collect(Collectors.joining());
        return "bc <<< \"%s\"\n".formatted(text);
    }
}
