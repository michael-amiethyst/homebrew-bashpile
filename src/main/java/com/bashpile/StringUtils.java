package com.bashpile;

import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

    public static String unescape(@Nonnull final String text) {
        return StringEscapeUtils.unescapeJava(text);
    }

    /**
     * Prepends a String to the last line of the text.
     *
     * @param prependFunc The lambda to apply to the last line
     * @param text Does not need to end with a newline.
     * @return A text block (ends with a newline).
     */
    public static String lambdaLastLine(
            @Nonnull final Function<String, String> prependFunc, @Nonnull final String text) {
        final String[] retLines = text.split("\n");
        final int lastLineIndex = retLines.length - 1;
        retLines[lastLineIndex] = prependFunc.apply(retLines[lastLineIndex]);
        return String.join("\n", retLines) + "\n";
    }
}
