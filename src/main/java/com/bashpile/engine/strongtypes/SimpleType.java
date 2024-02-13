package com.bashpile.engine.strongtypes;

import com.bashpile.BashpileParser;
import com.bashpile.Strings;
import com.bashpile.exceptions.TypeError;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/** The type of variable.  Aggregated by Type to handle Lists, Maps and Refs */
public enum SimpleType {
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
    public static @Nonnull SimpleType valueOf(@Nonnull final BashpileParser.TypedIdContext ctx) {
        return valueOf(ctx.type());
    }

    /** Gets the type specified in <code>ctx</code>. */
    public static @Nonnull SimpleType valueOf(@Nonnull final BashpileParser.TypeContext ctx) {
        final boolean hasTypeInfo = ctx.Type(0) != null && Strings.isNotBlank(ctx.Type(0).getText());
        final String typeName = ctx.Type(0).getText().toUpperCase();
        if (hasTypeInfo) {
            return valueOf(typeName);
        }
        throw new TypeError("No type info for " + typeName, ctx.start.getLine());
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
    public static @Nonnull SimpleType parseNumberString(@Nonnull final String text) {
        SimpleType type;
        try {
            new BigInteger(text);
            type = SimpleType.INT;
        } catch (NumberFormatException ignored) {
            new BigDecimal(text);
            type = SimpleType.FLOAT;
        }
        return type;
    }

    // class/enum methods

    public boolean isEmpty() {
        return this.equals(EMPTY);
    }

    /** Check if this type is unknown or numeric */
    public boolean isPossiblyNumeric() {
        return this.equals(UNKNOWN) || this.isNumeric();
    }

    /** Check if this type is a number, int or float */
    public boolean isNumeric() {
        return this.equals(NUMBER) || this.equals(INT) || this.equals(FLOAT);
    }

    public boolean isBasic() {
        return !List.of(LIST, MAP, REF).contains(this);
    }

    /**
     * Checks if this type can coerce to <code>other</code>.
     * @see <a href=https://developer.mozilla.org/en-US/docs/Glossary/Type_coercion>Type Coercion</a>
     */
    public boolean coercesTo(@Nonnull final SimpleType other) {
        // the types match if they are equal
        return this.equals(other)
                // unknown coerces to everything
                || this.equals(SimpleType.UNKNOWN) || other.equals(SimpleType.UNKNOWN)
                // an INT coerces to a FLOAT
                || (this.equals(SimpleType.INT) && other.equals(SimpleType.FLOAT))
                // a NUMBER coerces to an INT or a FLOAT
                || (this.equals(SimpleType.NUMBER) && other.isNumeric())
                // an INT or a FLOAT coerces to a NUMBER
                || (this.isNumeric() && other.equals(SimpleType.NUMBER));
    }
}
