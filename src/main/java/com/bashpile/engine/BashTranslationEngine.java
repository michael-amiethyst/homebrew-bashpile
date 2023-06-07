package com.bashpile.engine;

import com.bashpile.BashpileParser;

import java.util.stream.Collectors;

public class BashTranslationEngine implements TranslationEngine {
    @Override
    public String strictMode() {
        return """
                set -euo pipefail
                export IFS=$'\\n\\t'
                """;
    }

    @Override
    public String assign(String variable, String value) {
        return "export %s=%s\n".formatted(variable, value);
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
