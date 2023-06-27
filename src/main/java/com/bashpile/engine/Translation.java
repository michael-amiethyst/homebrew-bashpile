package com.bashpile.engine;

import static org.apache.commons.lang3.StringUtils.join;

/** Decorator pattern for a String */
public record Translation(String text, TranslationType type) {

    public static final Translation empty = toStringTranslation("");

    public static Translation toStringTranslation(final String... text) {
        return new Translation(join(text), TranslationType.STRING);
    }

    public Translation add(final String appendText) {
        return new Translation(text + appendText, type);
    }

    public boolean isNotSubshell() {
        return !type.equals(TranslationType.SUBSHELL_COMPOUND) && !type.equals(TranslationType.SUBSHELL_SUBSTITUTION);
    }
}
