package com.bashpile.engine;

/** Decorator pattern for a String */
public record Translation(String text, TranslationType type) {

    public static final Translation empty = toStringTranslation("");

    public static Translation toStringTranslation(String text) {
        return new Translation(text, TranslationType.STRING);
    }

    public Translation add(String appendText) {
        return new Translation(text + appendText, type);
    }
}
