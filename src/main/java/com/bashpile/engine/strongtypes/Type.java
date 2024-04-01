package com.bashpile.engine.strongtypes;

import com.bashpile.BashpileParser;
import com.bashpile.Strings;
import com.bashpile.exceptions.TypeError;

import javax.annotation.Nonnull;

/**
 * Basically a pair of SimpleTypes.  The secondary type is for the type of the contents for a list, hash or ref.
 */
public record Type(SimpleType mainType, SimpleType contentsType) {

    /** Not applicable -- usually used instead of NULL */
    public static final Type NA_TYPE = Type.of(SimpleType.NA);

    /**
     *  Type could not be determined (e.g. shell string results),
     *  coerces to any regular type (BOOL, INT, FLOAT, STR)
     */
    public static final Type UNKNOWN_TYPE = Type.of(SimpleType.UNKNOWN);

    /** Similar to the empty string ("") */
    public static final Type EMPTY_TYPE = Type.of(SimpleType.EMPTY);

    /** For when a search returns no results */
    public static final Type NOT_FOUND_TYPE = Type.of(SimpleType.NOT_FOUND);

    public static final Type BOOL_TYPE = Type.of(SimpleType.BOOL);

    /** All Integers (unlimited size) */
    public static final Type INT_TYPE = Type.of(SimpleType.INT);

    /** All floats (to 10 decimals) */
    public static final Type FLOAT_TYPE = Type.of(SimpleType.FLOAT);

    /** Generic number.  INT or FLOAT but don't know which one. */
    public static final Type NUMBER_TYPE = Type.of(SimpleType.NUMBER);

    /** Strings */
    public static final Type STR_TYPE = Type.of(SimpleType.STR);

    // DO NOT USE LIST_TYPE, use isList() instead

    public static @Nonnull Type of(@Nonnull SimpleType simpleType) {
        if (simpleType.isBasic()) {
            return new Type(simpleType, SimpleType.EMPTY);
        } // else
        return new Type(simpleType, SimpleType.UNKNOWN);
    }

    /** Gets the Type with mainType and contentsType info */
    public static @Nonnull Type valueOf(@Nonnull BashpileParser.TypedIdContext ctx) {
        return valueOf(ctx.type());
    }

    /** Gets the Type with mainType and contentsType info */
    public static Type valueOf(@Nonnull BashpileParser.TypeContext ctx) {
        final boolean hasTypeInfo = ctx.Type(0) != null && Strings.isNotBlank(ctx.Type(0).getText());
        final String mainTypeName = ctx.Type(0).getText().toUpperCase();
        final SimpleType mainType = SimpleType.valueOf(mainTypeName);
        if (hasTypeInfo && mainType.isBasic()) {
            return of(mainType);
        } else if (hasTypeInfo) /* and not basic */ {
            final SimpleType contentsType = SimpleType.valueOf(ctx.Type(1).getText().toUpperCase());
            return new Type(mainType, contentsType);
        }
        throw new TypeError("No type info for " + mainTypeName, ctx.start.getLine());
    }

    public @Nonnull String name() {
        if (contentsType.isEmpty()) {
            return mainType.name();
        } // else
        return "%s<%s>".formatted(mainType.name(), contentsType.name());
    }

    /** Is the type basic (e.g. not a List, Hash or Ref)? */
    public boolean isBasic() {
        return mainType.isBasic();
    }

    /** Is the main type LIST? */
    public boolean isList() {
        return mainType.equals(SimpleType.LIST);
    }

    /** Is this UNKNOWN? */
    public boolean isUnknown() {
        return equals(UNKNOWN_TYPE);
    }

    /** Is this NOT_FOUND? */
    public boolean isNotFound() {
        return equals(NOT_FOUND_TYPE);
    }

    /** Is this anything else besides NOT_FOUND? */
    public boolean isFound() {
        return !isNotFound();
    }

    /** Is this an integer? */
    public boolean isInt() {
        return equals(INT_TYPE);
    }

    /**
     * Check if this type is unknown or numeric.
     * @see #isNumeric()
     * @see #isGenericNumberType()
     */
    public boolean isPossiblyNumeric() {
        return mainType.isPossiblyNumeric();
    }

    /**
     * Check if this type is a number, int or float.
     * @see #isPossiblyNumeric()
     * @see #isGenericNumberType()
     */
    public boolean isNumeric() {
        return mainType.isNumeric();
    }

    /**
     * Is this specifically the NUMBER_TYPE and NOT known to be int or float specifically?
     * @see #isPossiblyNumeric()
     * @see #isNumeric()
     */
    public boolean isGenericNumberType() {
        return equals(NUMBER_TYPE);
    }

    /** Is this a String? */
    public boolean isStr() {
        return equals(STR_TYPE);
    }

    /**
     * Checks if this type can coerce to <code>other</code>.
     * @see <a href=https://developer.mozilla.org/en-US/docs/Glossary/Type_coercion>Type Coercion</a>
     */
    public boolean coercesTo(@Nonnull Type other) {
        return mainType.coercesTo(other.mainType) && contentsType.coercesTo(other.contentsType);
    }

    /** Returns the Type of the contents (e.g. a List&lt;str&gt; would return a str type) */
    public @Nonnull Type asContentsType() {
        if (isBasic()) {
            return this;
        } else {
            return Type.of(contentsType);
        }
    }
}
