package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedAssertionException;

import java.util.regex.Pattern;

public class Asserts {

    private static final Pattern textBlock = Pattern.compile("(?m)(?:^[^\n]*$\n)*");

    private static final Pattern textLine = Pattern.compile("(?:^[^\n]*$\n)*");

    /**
     * A text block is a group of text lines.
     *
     * @see #assertTextLine(String)
     */
    public static void assertTextBlock(String str) {
        assertMatches(str, textBlock);
    }

    /**
     * A text line contains only one newline at the end of the string.
     *
     * @param str the string to check.
     */
    public static void assertTextLine(String str) {
        assertMatches(str, textLine);
    }

    public static void assertMatches(String str, Pattern regex) {
        if (!regex.matcher(str).matches()) {
            throw new BashpileUncheckedAssertionException(
                    "Str %s didn't match regex %s".formatted(str, regex.pattern()));
        }
    }
}
