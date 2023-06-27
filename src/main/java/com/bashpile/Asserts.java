package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedAssertionException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static void assertTextBlock(final String str) {
        assertMatches(str, textBlock);
    }

    /**
     * A text line contains only one newline at the end of the string.
     *
     * @param str the string to check.
     */
    public static void assertTextLine(final String str) {
        assertMatches(str, textLine);
    }

    public static void assertMatches(final String str, final Pattern regex) {
        final Matcher matchResults = regex.matcher(str);
        if (!matchResults.matches()) {
            final String winNewlines = str.contains("\r") ? "" : "not ";
            throw new BashpileUncheckedAssertionException(
                    "Str [[[%s]]] didn't match regex %s, windows newlines %sfound"
                            .formatted(escapeJava(str), escapeJava(regex.pattern()), winNewlines));
        }
    }
}
