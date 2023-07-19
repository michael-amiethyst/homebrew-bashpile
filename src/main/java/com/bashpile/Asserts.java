package com.bashpile;

import com.bashpile.commandline.ExecutionResults;
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

    /** Checks for a complete match (i.e. whole string must match) */
    public static void assertMatches(@Nonnull final String str, @Nonnull final Pattern regex) {
        final Matcher matchResults = regex.matcher(str);
        if (!matchResults.matches()) {
            final String winNewlines = str.contains("\r") ? "found" : "not found";
            throw new BashpileUncheckedAssertionException(
                    "Str [%s] didn't match regex %s, windows newlines %s"
                            .formatted(escapeJava(str), escapeJava(regex.pattern()), winNewlines));
        }
    }

    /** Checks for a complete match (i.e. whole string must match) */
    public static void assertNoMatch(@Nonnull final String str, @Nonnull final Pattern regex) {
        final Matcher matchResults = regex.matcher(str);
        if (matchResults.matches()) {
            final String winNewlines = str.contains("\r") ? "found" : "not found";
            throw new BashpileUncheckedAssertionException(
                    "Str [%s] matched regex %s, windows newlines were %s"
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

    /**
     * Match does not mean equal: an expected FLOAT matches INT or FLOAT; NUMBER matches FLOAT or INT; and (INT or FLOAT) matches NUMBER.
     *
     * @param expectedTypes The types expected.
     * @param actualTypes  The types found.
     * @param functionName for the error message on a bad match.
     * @param contextStartLine for the error message on a bad match.
     */
    public static void assertTypesMatch(
            @Nonnull final List<Type> expectedTypes,
            @Nonnull final List<Type> actualTypes,
            @Nonnull final String functionName,
            int contextStartLine) {

        // check if the argument lengths match
        boolean typesMatch = actualTypes.size() == expectedTypes.size();
        // if the lengths match iterate over both lists
        if (typesMatch) {
            // lazily iterate over both lists looking for a non-match
            int i = 0;
            while (typesMatch && i < actualTypes.size()) {
                final Type expected = expectedTypes.get(i);
                final Type actual = actualTypes.get(i++);
                // the types match if they are equal
                // TODO move to Type
                typesMatch = expected.equals(actual)
                        // unknown matches everything
                        || expected.equals(Type.UNKNOWN) || actual.equals(Type.UNKNOWN)
                        // a FLOAT also matches an INT
                        || (expected.equals(Type.FLOAT) && actual.equals(Type.INT))
                        // a NUMBER also matches an INT or a FLOAT
                        || (expected.equals(Type.NUMBER) && (actual.equals(Type.INT) || actual.equals(Type.FLOAT)))
                        // an INT or a FLOAT also match NUMBER
                        || ((expected.equals(Type.INT) || expected.equals(Type.FLOAT)) && (actual.equals(Type.NUMBER)));
            }
        }
        if (!typesMatch) {
            throw new TypeError("Expected %s %s but was %s %s"
                    .formatted(functionName, expectedTypes, functionName, actualTypes), contextStartLine);
        }
    }

    public static void assertEquals(final int expected, final int actual) {
        assertEquals(expected, actual, null);
    }

    public static void assertEquals(final int expected, final int actual, @Nullable final String message) {
        if (expected != actual) {
            throw new AssertionError(requireNonNullElse(message, "Expected %d but got %d".formatted(expected, actual)));
        }
    }

    public static void assertExecutionSuccess(@Nonnull final ExecutionResults executionResults) {
        assertEquals(0, executionResults.exitCode(),
                "Found failing (non-0) 'nix exit code: %s.  Full text results:\n%s".formatted(
                        executionResults.exitCode(), executionResults.stdout()));
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
