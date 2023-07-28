package com.bashpile.engine.strongtypes;

import com.bashpile.BashpileParser;
import com.bashpile.StringUtils;
import com.bashpile.exceptions.TypeError;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.BigInteger;

public enum Type {
    /** Not Found */
    NOT_FOUND,
    /** Not applicable -- as in for statements */
    NA,
    /** Type could not be determined, coerces to any regular type (BOOL, INT, FLOAT, STR) */
    UNKNOWN,
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

    // static methods

    public static @Nonnull Type valueOf(@Nonnull final BashpileParser.TypedIdContext ctx) {
        final boolean hasTypeInfo = ctx.Type() != null && StringUtils.isNotBlank(ctx.Type().getText());
        if (hasTypeInfo) {
            return valueOf(ctx.Type().getText().toUpperCase());
        }
        throw new TypeError("No type info for " + ctx.Id(), ctx.start.getLine());
    }

    public static boolean isNumberString(@Nonnull final String text) {
        try {
            parseNumberString(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Throws NumberFormatException on bad parse. */
    public static Type parseNumberString(@Nonnull final String text) {
        Type type;
        try {
            new BigInteger(text);
            type = Type.INT;
        } catch (NumberFormatException ignored) {
            new BigDecimal(text);
            type = Type.FLOAT;
        }
        return type;
    }

    // class/enum methods

    public boolean isNotFound() {
        return this.equals(NOT_FOUND);
    }

    public boolean isPossiblyNumeric() {
        return this.equals(UNKNOWN) || this.isNumeric();
    }

    public boolean isNumeric() {
        return this.equals(NUMBER) || this.equals(INT) || this.equals(FLOAT);
    }

    public boolean coercesTo(@Nonnull final Type other) {
        // the types match if they are equal
        return this.equals(other)
                // unknown coerces to everything
                || this.equals(Type.UNKNOWN) || other.equals(Type.UNKNOWN)
                // an INT coerces to a FLOAT
                || (this.equals(Type.INT) && other.equals(Type.FLOAT))
                // a NUMBER coerces to an INT or a FLOAT
                || (this.equals(Type.NUMBER) && other.isNumeric())
                // an INT or a FLOAT coerces to a NUMBER
                || (this.isNumeric() && other.equals(Type.NUMBER));
    }
}
