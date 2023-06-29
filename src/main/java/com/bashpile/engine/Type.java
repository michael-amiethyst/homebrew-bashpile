package com.bashpile.engine;

import com.bashpile.BashpileLexer;
import com.bashpile.BashpileParser;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.TypeError;
import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

public enum Type {
    UNDEF,
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

    public static Type parseNumberString(String text) {
        Type type;
        try {
            Float.parseFloat(text);
            type = Type.FLOAT;
        } catch (NumberFormatException ignored) {
            type = Type.INT;
        }
        return type;
    }
}
