package com.bashpile;

import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

    public static String unescape(@Nonnull final String text) {
        return StringEscapeUtils.unescapeJava(text);
    }

    public static String lambdaFirstLine(
            @Nonnull final String text, @Nonnull final Function<String, String> lambda) {
        final String[] retLines = text.split("\n");
        retLines[0] = lambda.apply(retLines[0]);
        final String append = text.endsWith("\n") ? "\n" : "";
        return String.join("\n", retLines) + append;
    }

    /**
     * Prepends a String to the last line of the text.
     *
     * @param lambda The lambda to apply to the last line
     * @param text Does not need to end with a newline.
     * @return A paragraph (ends with a newline).
     */
    public static String lambdaLastLine(
            @Nonnull final String text, @Nonnull final Function<String, String> lambda) {
        final String[] retLines = text.split("\n");
        final int lastLineIndex = retLines.length - 1;
        retLines[lastLineIndex] = lambda.apply(retLines[lastLineIndex]);
        final String append = text.endsWith("\n") ? "\n" : "";
        return String.join("\n", retLines) + append;
    }

    public static String lambdaAllLines(@Nonnull final String text, @Nonnull final Function<String, String> lambda) {
        final String[] lines = text.split("\n");
        final String tabbedBody = Arrays.stream(lines)
                .filter(StringUtils::isNotBlank)
                .map(lambda)
                .collect(Collectors.joining("\n"));
        final String append = text.endsWith("\n") ? "\n" : "";
        return tabbedBody + append;
    }
}
