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

    /** Throws up test isn't true with optional message */
    public static boolean assertTrue(final boolean test, @Nullable final String message) {
        if (!test) {
            throw new BashpileUncheckedAssertionException(message != null ? message : "True assert wasn't true");
        } // else
        return true;
    }

    /** Throws {@code BashpileUncheckedAssertionException} when the test isn't true with optional message */
    public static boolean assertFalse(final boolean test, @Nullable final String message) {
        if (test) {
            throw new BashpileUncheckedAssertionException(message != null ? message : "False assert was actually true");
        } // else
        return true;
    }

    /** Throws {@code BashpileUncheckedAssertionException} if the Java list is empty */
    public static <T> @Nonnull List<T> assertNotEmpty(@Nonnull final List<T> list) {
        if (list.isEmpty()) {
            throw new BashpileUncheckedAssertionException("List was empty");
        }
        return list;
    }

    /**
     * A text block is a group of text lines.  Each line ends with a newline.
     *
     * @see #assertIsLine(String)
     */
    public static @Nonnull String assertIsParagraph(@Nonnull final String str) {
        try {
            return assertMatches(str, TEXT_BLOCK);
        } catch (BashpileUncheckedAssertionException e) {
            throw new BashpileUncheckedAssertionException(
                    "Expected String [%s] to be a paragraph and have every line end in a '\\n'.".formatted(str));
        }
    }

    /**
     * A text line contains only one newline at the end of the string.  Must end with newline or be blank.
     *
     * @param str the string to check.
     */
    public static @Nonnull String assertIsLine(@Nonnull final String str) {
        try {
            return assertMatches(str, TEXT_LINE);
        } catch (BashpileUncheckedAssertionException e) {
            throw new BashpileUncheckedAssertionException(
                    "Expected String [%s] to be a single line.  It should have a single '\\n', at the end.".formatted(
                            str));
        }
    }

    /** Checks for a complete match (i.e. whole string must match) */
    public static @Nonnull String assertMatches(@Nonnull final String str, @Nonnull final Pattern regex) {
        final Matcher matchResults = regex.matcher(str);
        if (!matchResults.matches()) {
            throw new BashpileUncheckedAssertionException(
                    "Str [%s] didn't match regex %s".formatted(escapeJava(str), escapeJava(regex.pattern())));
        }
        return str;
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

        // TODO reimplement with optional arguments in mind
        // check if the argument lengths match
//        boolean typesCoerce = expectedTypes.size() == actualTypes.size();
//        if (!typesCoerce) {
//            final String message = "Mismatch of type list lengths for function %s.  Expected %d arguments and found %d"
//                    .formatted(functionName, expectedTypes.size(), actualTypes.size());
//            throw new TypeError(message, contextStartLine);
//        }

        // lazily iterate over both lists looking for a non-match
        int i = 0;
        boolean typesCoerce = true;
        while (typesCoerce && i < actualTypes.size()) {
            final Type expected = expectedTypes.get(i);
            final Type actual = actualTypes.get(i++);
            // &= operator not needed
            typesCoerce = actual.coercesTo(expected) ||
                    (expected.isList() && actual.coercesTo(expected.asContentsType().orElseThrow()));
        }

        if (!typesCoerce) {
            if (i != 0) {
                i--; // cancel out last i++
            }
            final Type expectedType = expectedTypes.get(i);
            final Type actualType = actualTypes.get(i);
            String message;
            if (expectedType.isBasic()) {
                message = "'%s' expected %s but found %s".formatted(
                        functionName, expectedType, actualType);
            } else {
                message = "Tried to coerce %s to %s and failed".formatted(actualType, expectedType);
            }
            throw new TypeError(message, contextStartLine);
        }
    }

    /**
     * Checks that expected {@link #equals(Object)} actual.
     * Throws {@link BashpileUncheckedAssertionException} on failed assert.
     *
     * @param expected The expected int.
     * @param actual The actually found int.
     * @param message The optional message for a failed assert.
     */
    public static void assertEquals(
            int expected, int actual, @Nullable final String message) {
        if (expected != actual) {
            throw new BashpileUncheckedAssertionException(
                    requireNonNullElse(message, "Expected %s but got %s".formatted(expected, actual)));
        }
    }

    /**
     * Checks that expected {@link #equals(Object)} actual.
     * Throws {@link BashpileUncheckedAssertionException} on failed assert.
     *
     * @param expected The expected String.
     * @param actual The actually found String.
     * @param message The optional message for a failed assert.
     */
    public static void assertEquals(
            @Nonnull final String expected, @Nullable final String actual, @Nullable final String message) {
        if (!expected.equals(actual)) {
            throw new BashpileUncheckedAssertionException(
                    requireNonNullElse(message, "Expected %s but got %s".formatted(expected, actual)));
        }
    }

    /**
     * Asserts that {@code actual} isn't more than {@code max}.
     */
    public static void assertNotOver(
            final long max, final long actual, @Nullable final String message) {
        if (actual > max) {
            throw new BashpileUncheckedAssertionException(
                    requireNonNullElse(message, "Expected nothing over %d but found %d".formatted(max, actual)));
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
            throw new BashpileUncheckedAssertionException("Found key %s in map %s".formatted(key, map));
        }
    }
}
