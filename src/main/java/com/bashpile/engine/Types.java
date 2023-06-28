package com.bashpile.engine;

import com.bashpile.BashpileParser;
import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public enum Types {
    UNDEF,
    BOOL,
    INT,
    FLOAT,
    STR,
    ARRAY,
    MAP,
    REF;

    public static @Nonnull Types valueOf(@Nonnull BashpileParser.TypedIdContext ctx) {
        final boolean hasTypeInfo = ctx.TYPE() != null && StringUtils.isNotBlank(ctx.TYPE().getText());
        if (hasTypeInfo) {
            return valueOf(ctx.TYPE().getText().toUpperCase());
        }
        return UNDEF;
    }

    public static @Nonnull Types valueOf(@Nonnull Token token) {
        // TODO handle more cases
        try {
            Integer.parseInt(token.getText());
            return INT;
        } catch (NumberFormatException e) {
            return FLOAT;
        }
    }
}
