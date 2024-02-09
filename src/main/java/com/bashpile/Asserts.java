package com.bashpile;

import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.BashpileUncheckedAssertionException;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.TypeError;
import com.bashpile.shell.BashShell;
import com.bashpile.shell.ExecutionResults;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bashpile.exceptions.Exceptions.asUnchecked;
import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;

/** Assert stuff, in production as well */
public class Asserts {

    /** In MULTILINE mode use a non-capturing group to match 0 or more lines (or blanks) */
    private static final Pattern TEXT_BLOCK = Pattern.compile("(?m)(?:^[^\n]*$\n)*");

    /** Match a line of text with a Linux line ending at the end OR the empty string */
    private static final Pattern TEXT_LINE = Pattern.compile("^[^\n]*$\n|^$");

    private static final Pattern BLANK_LINE = Pattern.compile("(?m)^ *$");

    public static boolean assertTrue(final boolean test, @Nullable final String message) {
        if (!test) {
            throw new BashpileUncheckedAssertionException(message != null ? message : "True assert wasn't true");
        } // else
        return true;
    }

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

    /** Ensures that there are no blank lines */
    public static void assertNoBlankLines(@Nonnull final String str) {
        assertNoMatch(str, BLANK_LINE);
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

    /**
     * Checks for a complete match (i.e. whole string must match)
     */
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

    /**
     * Ensures that the shellcheck program can find no warnings.
     *
     * @param translatedShellScript The Bash script
     * @return The translatedShellScript for chaining.
     */
    public static @Nonnull String assertNoShellcheckWarnings(@Nonnull final String translatedShellScript) {
        final Path tempFile = Path.of("temp.bps");
        try {
            Files.writeString(tempFile, translatedShellScript);
            // ignore many errors that don't apply
            final String excludes = Stream.of(2034, 2050, 2071, 2072, 2157)
                    .map(i -> "--exclude=SC" + i).collect(Collectors.joining(" "));
            final ExecutionResults shellcheckResults = BashShell.runAndJoin(
                    "shellcheck --shell=bash --severity=warning %s %s".formatted(excludes, tempFile));
            if (shellcheckResults.exitCode() != 0) {
                final String message = "Script failed shellcheck.  Script:\n%s\nShellcheck output:\n%s".formatted(
                        translatedShellScript, shellcheckResults.stdout());
                throw new BashpileUncheckedAssertionException(message);
            }
            return translatedShellScript;
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        } finally {
            asUnchecked(() -> Files.deleteIfExists(tempFile));
        }
    }
}
