package com.bashpile.engine.strongtypes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;

import com.bashpile.Asserts;
import com.bashpile.BashpileParser;
import com.bashpile.Strings;
import com.bashpile.exceptions.TypeError;

import static com.bashpile.engine.strongtypes.Type.TypeNames.*;

/**
 * Basically a pair of SimpleTypes.  The secondary type is for the type of the contents for a list, hash or ref.
 */
public record Type(@Nonnull TypeNames mainTypeName, @Nonnull Optional<Type> contentsType) {

    // static section

    public enum TypeNames {
        NA,
        UNKNOWN,
        EMPTY,
        NOT_FOUND,
        BOOL,
        INT,
        FLOAT,
        NUMBER,
        STR,
        LIST
    }

    /** Not applicable -- usually used instead of NULL */
    public static final Type NA_TYPE = Type.of(NA.name());

    /**
     *  Type could not be determined (e.g. shell string results),
     *  coerces to any regular type (BOOL, INT, FLOAT, STR)
     */
    public static final Type UNKNOWN_TYPE = Type.of(UNKNOWN.name());

    /** Similar to the empty string ("") */
    public static final Type EMPTY_TYPE = Type.of(EMPTY.name());

    /** For when a search returns no results */
    public static final Type NOT_FOUND_TYPE = Type.of(NOT_FOUND.name());

    public static final Type BOOL_TYPE = Type.of(BOOL.name());

    /** All Integers (unlimited size) */
    public static final Type INT_TYPE = Type.of(INT.name());

    /** All floats (to 10 decimals) */
    public static final Type FLOAT_TYPE = Type.of(FLOAT.name());

    /** Generic number.  INT or FLOAT but don't know which one. */
    public static final Type NUMBER_TYPE = Type.of(NUMBER.name());

    /** Strings */
    public static final Type STR_TYPE = Type.of(STR.name());

    /** Lists of unknown contents */
    public static final Type LIST_TYPE = Type.of(LIST, UNKNOWN_TYPE);

    public static @Nonnull Type of(@Nonnull String mainType) {
        return new Type(TypeNames.valueOf(mainType), Optional.empty());
    }

    public static @Nonnull Type of(@Nonnull final TypeNames mainType, @Nonnull final Type contentsType) {
        return new Type(mainType, Optional.of(contentsType));
    }

    /** Gets the Type with mainTypeName and contentsType info */
    public static @Nonnull Type valueOf(@Nonnull String mainType, int lineNumber) {
        if (mainType.equalsIgnoreCase(UNKNOWN.name())) {
            return UNKNOWN_TYPE;
        } else if (mainType.equalsIgnoreCase(NOT_FOUND.name()) || mainType.equalsIgnoreCase("notfound")) {
            return NOT_FOUND_TYPE;
        } else if (mainType.equalsIgnoreCase(EMPTY.name())) {
            return EMPTY_TYPE;
        } else if (mainType.equalsIgnoreCase(BOOL.name())) {
            return BOOL_TYPE;
        } else if (mainType.equalsIgnoreCase(INT.name())) {
            return INT_TYPE;
        } else if (mainType.equalsIgnoreCase(FLOAT.name())) {
            return FLOAT_TYPE;
        } else if (mainType.equalsIgnoreCase(NUMBER.name())) {
            return NUMBER_TYPE;
        } else if (mainType.equalsIgnoreCase(STR.name())) {
            return STR_TYPE;
        } else if (mainType.equalsIgnoreCase(LIST.name())) {
            return LIST_TYPE;
        } else {
            throw new TypeError("Could not find Type of " + mainType, lineNumber);
        }
    }

    /** Gets the Type with mainTypeName and contentsType info */
    public static @Nonnull Type valueOf(@Nonnull BashpileParser.TypeContext ctx) {
        // guard
        final boolean hasTypeInfo = ctx.Type(0) != null && Strings.isNotBlank(ctx.Type(0).getText());
        Asserts.assertTrue(hasTypeInfo, "Type information somehow missing");

        // body
        final String mainTypeName = ctx.Type(0).getText().toUpperCase();
        final int line = ctx.start.getLine();
        boolean isSimpleType = ctx.Type(1) == null || Strings.isBlank(ctx.Type(1).getText());
        if (isSimpleType) {
            return valueOf(mainTypeName, line);
        } else {
            final Type contentsType = valueOf(ctx.Type(1).getText(), line);
            return new Type(TypeNames.valueOf(mainTypeName), Optional.of(contentsType));
        }
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
        try {
            new BigInteger(text);
            return INT_TYPE;
        } catch (NumberFormatException ignored) {
            new BigDecimal(text);
            return FLOAT_TYPE;
        }
    }

    // Class methods

    public @Nonnull String name() {
        if (isBasic()) {
            return mainTypeName.name();
        } // else
        return "%s<%s>".formatted(mainTypeName, contentsType.orElseThrow().name());
    }

    /** Is the type basic (e.g. not a List, Hash or Ref)? */
    public boolean isBasic() {
        return !isList();
    }

    /** Is the main type LIST? */
    public boolean isList() {
        return mainTypeName == LIST;
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

    // TODO factor out common isType method
    /** Is this an integer? */
    public boolean isInt() {
        return mainTypeName.name().equalsIgnoreCase(INT.name())
                && (contentsType.isEmpty() || contentsType.orElseThrow().mainTypeName.equals(NA));
    }

    public boolean isFloat() {
        return mainTypeName.name().equalsIgnoreCase(FLOAT.name())
                && (contentsType.isEmpty() || contentsType.orElseThrow().mainTypeName.equals(NA));
    }

    /**
     * Check if this type is a number, int or float.
     * @see #isNumber()
     */
    public boolean isNumeric() {
        return isInt() || isFloat() || isNumber();
    }

    /**
     * Is this specifically the NUMBER_TYPE and NOT known to be int or float specifically?
     * @see #isNumeric()
     */
    public boolean isNumber() {
        return mainTypeName.name().equalsIgnoreCase(NUMBER.name())
                && (contentsType.isEmpty() || contentsType.orElseThrow().mainTypeName.equals(NA));
    }

    /** Is this a String? */
    public boolean isStr() {
        return mainTypeName.name().equalsIgnoreCase(STR.name())
                && (contentsType.isEmpty() || contentsType.orElseThrow().mainTypeName.equals(NA));
    }

    /**
     * Checks if this type can coerce to <code>other</code>.
     * @see <a href=https://developer.mozilla.org/en-US/docs/Glossary/Type_coercion>Type Coercion</a>
     */
    public boolean coercesTo(@Nonnull Type other) {
        // contents type of null can coerce to anything
        if (this.isBasic() && other.isBasic()) {
            return typesCoerce(mainTypeName, other.mainTypeName);
        } else if (!this.isBasic() && !other.isBasic()) {
            return typesCoerce(mainTypeName, other.mainTypeName)
                    && (contentsType.isEmpty()
                        || contentsType.orElse(NA_TYPE).coercesTo(other.contentsType.orElse(NA_TYPE)));
        } else if (other.isList()) {
            return other.contentsType.isEmpty()
                    || typesCoerce(other.contentsType.orElse(NA_TYPE).mainTypeName, mainTypeName);
        } else {
            // mismatch (e.g. list to a string, or int to a list)
            return false;
        }
    }

    /** Returns the Type of the contents (e.g. a List&lt;str&gt; would return a str type) */
    public @Nonnull Optional<Type> asContentsType() {
        return contentsType;
    }

    // autogenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Type type = (Type) o;
        return Objects.equals(contentsType, type.contentsType) && mainTypeName == type.mainTypeName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainTypeName, contentsType);
    }

    /**
     * Checks if this type (represented by a name) can coerce to <code>other</code>.
     * @see <a href=https://developer.mozilla.org/en-US/docs/Glossary/Type_coercion>Type Coercion</a>
     */
    private static boolean typesCoerce(@Nonnull final TypeNames first, @Nonnull final TypeNames other) {
        // the types match if they are equal
        return first.equals(other)
                // unknown coerces to everything
                || (first.equals(UNKNOWN_TYPE.mainTypeName) || other.equals(UNKNOWN_TYPE.mainTypeName))
                // an INT coerces to a FLOAT
                || (first.equals(INT_TYPE.mainTypeName) && other.equals(FLOAT_TYPE.mainTypeName))
                // a NUMBER coerces to an INT or a FLOAT
                || (first.equals(NUMBER_TYPE.mainTypeName) && (new Type(other, Optional.empty())).isNumeric())
                // an INT or a FLOAT coerces to a NUMBER
                || ((new Type(first, Optional.empty())).isNumeric() && other.equals(NUMBER_TYPE.mainTypeName));
    }
}
