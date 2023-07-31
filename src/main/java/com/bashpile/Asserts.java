package com.bashpile;

import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.BashpileUncheckedAssertionException;
import com.bashpile.exceptions.TypeError;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;

/** Assert stuff, in production as well */
public class Asserts {

    /** In MULTILINE mode use a non-capturing group to match 0 or more lines (or blanks) */
    private static final Pattern TEXT_BLOCK = Pattern.compile("(?m)(?:^[^\n]*$\n)*");

    /** Match a line of text with a Linux line ending at the end OR the empty string */
    private static final Pattern TEXT_LINE = Pattern.compile("^[^\n]*$\n|^$");

    private static final Pattern BLANK_LINE = Pattern.compile("\n *\n");

    /**
     * A text block is a group of text lines.  Each line ends with a newline.
     *
     * @see #assertIsLine(String)
     */
    public static String assertIsParagraph(@Nonnull final String str) {
        assertMatches(str, TEXT_BLOCK);
        return str;
    }

    /**
     * A text line contains only one newline at the end of the string.  Must end with newline or be blank.
     *
     * @param str the string to check.
     */
    public static String assertIsLine(@Nonnull final String str) {
        assertMatches(str, TEXT_LINE);
        return str;
    }

    public static void assertNoBlankLines(@Nonnull final String str) {
        assertNoMatch(str, BLANK_LINE);
    }

    /** Checks for a complete match (i.e. whole string must match) */
    public static void assertMatches(@Nonnull final String str, @Nonnull final Pattern regex) {
        final Matcher matchResults = regex.matcher(str);
        if (!matchResults.matches()) {
            throw new BashpileUncheckedAssertionException(
                    "Str [%s] didn't match regex %s".formatted(escapeJava(str), escapeJava(regex.pattern())));
        }
    }

    /** Checks for a complete match (i.e. whole string must match) */
    public static void assertNoMatch(@Nonnull final String str, @Nonnull final Pattern regex) {
        final Matcher matchResults = regex.matcher(str);
        if (matchResults.matches()) {
            throw new BashpileUncheckedAssertionException(
                    "Str [%s] matched regex %s".formatted(escapeJava(str), escapeJava(regex.pattern())));
        }
    }

    public static void assertTypesCoerce(
            @Nonnull final Type expectedType,
            @Nonnull final Type actualType,
            @Nonnull final String functionName,
            final int contextStartLine) {
        assertTypesCoerce(List.of(expectedType), List.of(actualType), functionName, contextStartLine);
    }

    /**
     * Match does not mean equal: an expected FLOAT matches INT or FLOAT; NUMBER matches FLOAT or INT; and (INT or FLOAT) matches NUMBER.
     *
     * @param expectedTypes The types expected.
     * @param actualTypes  The types found.
     * @param functionName for the error message on a bad match.
     * @param contextStartLine for the error message on a bad match.
     */
    public static void assertTypesCoerce(
            @Nonnull final List<Type> expectedTypes,
            @Nonnull final List<Type> actualTypes,
            @Nonnull final String functionName,
            final int contextStartLine) {

        // check if the argument lengths match
        boolean typesCoerce = actualTypes.size() == expectedTypes.size();

        // lazily iterate over both lists looking for a non-match
        int i = 0;
        while (typesCoerce && i < actualTypes.size()) {
            final Type expected = expectedTypes.get(i);
            final Type actual = actualTypes.get(i++);
            typesCoerce = actual.coercesTo(expected);
        }

        if (!typesCoerce) {
            throw new TypeError("Expected %s %s but was %s %s"
                    .formatted(functionName, expectedTypes, functionName, actualTypes), contextStartLine);
        }
    }

    public static void assertEquals(
            @Nonnull final String expected, @Nullable final String actual, @Nullable final String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(requireNonNullElse(message, "Expected %s but got %s".formatted(expected, actual)));
        }
    }

    /**
     * Throws {@code uncheckedException} or an {@link AssertionError} on failure.
     */
    public static <K, V> void assertMapDoesNotContainKey(
            @Nonnull final K key,
            @Nonnull final Map<K, V> map,
            @Nullable final RuntimeException uncheckedException) {
        if (map.containsKey(key)) {
            if (uncheckedException != null) {
                throw uncheckedException;
            }
            throw new AssertionError("Found key %s in map %s".formatted(key, map));
        }
    }
}
