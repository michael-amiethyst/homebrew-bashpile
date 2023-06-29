package com.bashpile.engine.strongtypes;

import com.bashpile.BashpileParser;
import com.bashpile.exceptions.TypeError;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public enum Type {
    /** Not Found */
    EMPTY,
    /** Not applicable -- as in for statements */
    NA,
    /** NIL, aka null */
    NIL,
    BOOL,
    INT,
    FLOAT,
    /** For when INT or FLOAT cannot be determined. */
    NUMBER,
    STR,
    ARRAY,
    MAP,
    REF;

    public static @Nonnull Type valueOf(@Nonnull BashpileParser.TypedIdContext ctx) {
        final boolean hasTypeInfo = ctx.TYPE() != null && StringUtils.isNotBlank(ctx.TYPE().getText());
        if (hasTypeInfo) {
            return valueOf(ctx.TYPE().getText().toUpperCase());
        }
        throw new TypeError("No type info for " + ctx.ID());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static Type parseNumberString(String text) {
        Type type;
        try {
            Integer.parseInt(text);
            type = Type.INT;
        } catch (NumberFormatException ignored) {
            Float.parseFloat(text);
            type = Type.INT;
        }
        return type;
    }
}
