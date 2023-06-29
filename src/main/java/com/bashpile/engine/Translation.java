package com.bashpile.engine;

import static org.apache.commons.lang3.StringUtils.join;

/** Decorator pattern for a String */
public record Translation(String text, Type type, MetaType metaType) {

    public static final Translation empty = toStringTranslation("");

    public static Translation toStringTranslation(final String... text) {
        return new Translation(join(text), Type.STR, MetaType.NORMAL);
    }

    public Translation add(final String appendText) {
        return new Translation(text + appendText, this.type, metaType);
    }

    public boolean isNotSubshell() {
        return !metaType.equals(MetaType.SUBSHELL_COMPOUND) && !metaType.equals(MetaType.SUBSHELL_SUBSTITUTION);
    }
}
