package com.bashpile.engine.strongtypes;

import com.bashpile.BashpileParser;
import com.bashpile.Strings;
import com.bashpile.exceptions.TypeError;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.BigInteger;

/** The type of variable */
public enum Type {
    /** Not Found */
    NOT_FOUND,
    /** Not applicable -- as in for statements */
    NA,
    /**
     *  Type could not be determined (e.g. shell string results),
     *  coerces to any regular type (BOOL, INT, FLOAT, STR)
     */
    UNKNOWN,
    /** Instead of NIL or null we have the empty String or an empty object */
    EMPTY,
    /** A boolean */
    BOOL,
    /** An integer, for all non-fractional numbers */
    INT,
    /** A float, for all fractional numbers */
    FLOAT,
    /** For when INT or FLOAT cannot be determined. */
    NUMBER,
    /** A String */
    STR,
    /** A Bash array */
    LIST,
    /** A map */
    MAP,
    /** A Bash reference */
    REF;

    // static methods

    /** Gets the type specified in <code>ctx</code>. */
    public static @Nonnull Type valueOf(@Nonnull final BashpileParser.TypedIdContext ctx) {
        final boolean hasTypeInfo = ctx.Type() != null && Strings.isNotBlank(ctx.Type().getText());
        if (hasTypeInfo) {
            return valueOf(ctx.Type().getText().toUpperCase());
        }
        throw new TypeError("No type info for " + ctx.Id(), ctx.start.getLine());
    }

    /** True if <code>text</code> holds a number. */
    public static boolean isNumberString(@Nonnull final String text) {
        try {
            parseNumberString(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Throws NumberFormatException on bad parse. */
    public static @Nonnull Type parseNumberString(@Nonnull final String text) {
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

    /** Check if this type is unknown or numeric */
    public boolean isPossiblyNumeric() {
        return this.equals(UNKNOWN) || this.isNumeric();
    }

    /** Check if this type is a number, int or float */
    public boolean isNumeric() {
        return this.equals(NUMBER) || this.equals(INT) || this.equals(FLOAT);
    }

    /**
     * Checks if this type can coerce to <code>other</code>.
     * @see <a href=https://developer.mozilla.org/en-US/docs/Glossary/Type_coercion>Type Coercion</a>
     */
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
