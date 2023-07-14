package com.bashpile.engine;

import com.bashpile.engine.strongtypes.MetaType;
import com.bashpile.engine.strongtypes.Type;

import static org.apache.commons.lang3.StringUtils.join;

/** Decorator pattern for a String */
public record Translation(String text, Type type, MetaType metaType) {

    public static final Translation EMPTY_TYPE = new Translation("", Type.EMPTY, MetaType.NORMAL);
    public static final Translation EMPTY_STRING = toStringTranslation("");

    public static Translation toStringTranslation(final String... text) {
        return new Translation(join(text), Type.STR, MetaType.NORMAL);
    }

    public Translation add(final String appendText) {
        return new Translation(text + appendText, this.type, metaType);
    }

    public boolean isNotSubshell() {
        return !metaType.equals(MetaType.SUBSHELL) && !metaType.equals(MetaType.COMMAND_SUBSTITUTION);
    }
}
