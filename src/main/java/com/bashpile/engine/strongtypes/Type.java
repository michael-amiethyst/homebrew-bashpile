package com.bashpile.engine.strongtypes;

import com.bashpile.BashpileParser;
import com.bashpile.exceptions.TypeError;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public enum Type {
    /** Not Found */
    NOT_FOUND,
    /** Not applicable -- as in for statements */
    NA,
    /** Instead of NIL or null we have the empty String or an empty object */
    EMPTY,
    BOOL,
    INT,
    FLOAT,
    /** For when INT or FLOAT cannot be determined. */
    NUMBER,
    STR,
    ARRAY,
    MAP,
    /** A Bash reference */
    REF;

    public static @Nonnull Type valueOf(@Nonnull BashpileParser.TypedIdContext ctx) {
        final boolean hasTypeInfo = ctx.TYPE() != null && StringUtils.isNotBlank(ctx.TYPE().getText());
        if (hasTypeInfo) {
            return valueOf(ctx.TYPE().getText().toUpperCase());
        }
        throw new TypeError("No type info for " + ctx.ID(), ctx.start.getLine());
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

    public boolean isNotFound() {
        return this.equals(NOT_FOUND);
    }

    public boolean isStr() {
        return this.equals(STR);
    }

    public boolean isNumeric() {
        return this.equals(NUMBER) || this.equals(INT) || this.equals(FLOAT);
    }
}
