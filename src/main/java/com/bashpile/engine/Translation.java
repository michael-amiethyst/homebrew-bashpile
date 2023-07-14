package com.bashpile.engine;

import com.bashpile.engine.strongtypes.TypeMetadata;
import com.bashpile.engine.strongtypes.Type;

import javax.annotation.Nonnull;

import static org.apache.commons.lang3.StringUtils.join;

/** Decorator pattern for a String */
public record Translation(@Nonnull String text, @Nonnull Type type, @Nonnull TypeMetadata typeMetadata) {

    public static final Translation EMPTY_TYPE = new Translation("", Type.EMPTY, TypeMetadata.NORMAL);
    public static final Translation EMPTY_STRING = toStringTranslation("");

    public static Translation toStringTranslation(final String... text) {
        return new Translation(join(text), Type.STR, TypeMetadata.NORMAL);
    }

    public Translation add(final String appendText) {
        return new Translation(text + appendText, this.type, typeMetadata);
    }

    public boolean isNotSubshell() {
        return !typeMetadata.equals(TypeMetadata.SUBSHELL) && !typeMetadata.equals(TypeMetadata.COMMAND_SUBSTITUTION);
    }
}
