package com.bashpile;

import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.BashpileUncheckedAssertionException;
import com.bashpile.exceptions.TypeError;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;

/** Assert stuff, in production as well */
public class Asserts {

    /** In MULTILINE mode use a non-capturing group to match 0 or more lines (or blanks) */
    private static final Pattern textBlock = Pattern.compile("(?m)(?:^[^\n]*$\n)*");

    /** Match a line of text with a Linux line ending at the end OR the empty string */
    private static final Pattern textLine = Pattern.compile("^[^\n]*$\n|^$");

    /**
     * A text block is a group of text lines.
     *
     * @see #assertTextLine(String)
     */
    public static void assertTextBlock(@Nonnull final String str) {
        assertMatches(str, textBlock);
    }

    /**
     * A text line contains only one newline at the end of the string.
     *
     * @param str the string to check.
     */
    public static void assertTextLine(@Nonnull final String str) {
        assertMatches(str, textLine);
    }

    public static void assertMatches(@Nonnull final String str, @Nonnull final Pattern regex) {
        final Matcher matchResults = regex.matcher(str);
        if (!matchResults.matches()) {
            final String winNewlines = str.contains("\r") ? "" : "not ";
            throw new BashpileUncheckedAssertionException(
                    "Str [[[%s]]] didn't match regex %s, windows newlines %sfound"
                            .formatted(escapeJava(str), escapeJava(regex.pattern()), winNewlines));
        }
    }

    public static void assertTypesMatch(
            @Nonnull final Type expectedType,
            @Nonnull final Type actualType,
            @Nonnull final String functionName,
            int contextStartLine) {
        assertTypesMatch(List.of(expectedType), List.of(actualType), functionName, contextStartLine);
    }

    public static void assertTypesMatch(
            @Nonnull final List<Type> expectedTypes,
            @Nonnull final List<Type> actualTypes,
            @Nonnull final String functionName,
            int contextStartLine) {

        // check if the argument lengths match
        boolean typesMatch = actualTypes.size() == expectedTypes.size();
        // if they match iterate over both lists
        if (typesMatch) {
            for (int i = 0; i < actualTypes.size(); i++) {
                Type expected = expectedTypes.get(i);
                Type actual = actualTypes.get(i);
                // the types match if they are equal
                typesMatch &= expected.equals(actual)
                        // FLOAT also matches INT
                        || (expected.equals(Type.FLOAT) && actual.equals(Type.INT))
                        // and NUMBER matches INT or FLOAT
                        || (expected.equals(Type.NUMBER) && (actual.equals(Type.INT) || actual.equals(Type.FLOAT)))
                        // INT and FLOAT also match NUMBER
                        || ((expected.equals(Type.INT) || expected.equals(Type.FLOAT)) && (actual.equals(Type.NUMBER)));
            }
        }
        if (!typesMatch) {
            throw new TypeError("Expected %s %s but was %s %s on Bashpile Line %s"
                    .formatted(functionName, expectedTypes, functionName, actualTypes, contextStartLine));
        }
    }

    public static void assertEquals(final int expected, final int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected %d but got %d".formatted(expected, actual));
        }
    }

    public static void assertEquals(
            @Nonnull final String expected, @Nullable final String actual, @Nullable final String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(requireNonNullElse(message, "Expected %s but got %s".formatted(expected, actual)));
        }
    }
}
