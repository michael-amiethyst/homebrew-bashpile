package com.bashpile;

import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

    public static String unescape(@Nonnull final String text) {
        return StringEscapeUtils.unescapeJava(text);
    }

    /**
     * Prepends a String to the last line of the text.
     *
     * @param prepend The string to prepend
     * @param text Does not need to end with a newline.
     * @return A text block (ends with a newline).
     */
    public static String prependLastLine(@Nonnull final String prepend, @Nonnull final String text) {
        final String[] retLines = text.split("\n");
        final int lastLineIndex = retLines.length - 1;
        retLines[lastLineIndex] = prepend + retLines[lastLineIndex];
        return String.join("\n", retLines) + "\n";
    }
}
