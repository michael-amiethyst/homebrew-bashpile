package com.bashpile;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Our String utilities class */
public class Strings extends StringUtils {

    /** A pattern of starting and ending double quotes */
    private static final Pattern STRING_QUOTES = Pattern.compile("^([\"'])(.*)([\"'])$");

    public static @Nonnull String unquote(@Nonnull final String str) {
        return removeEndGroups(STRING_QUOTES, str);
    }

    /** @see StringEscapeUtils#unescapeJava(String) */
    public static @Nonnull String unescape(@Nonnull final String text) {
        return StringEscapeUtils.unescapeJava(text);
    }

    /** Applies a function to the first line only */
    public static @Nonnull String lambdaFirstLine(
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
    public static @Nonnull String lambdaLastLine(
            @Nonnull final String text, @Nonnull final Function<String, String> lambda) {
        final String[] retLines = text.split("\n");
        final int lastLineIndex = retLines.length - 1;
        retLines[lastLineIndex] = lambda.apply(retLines[lastLineIndex]);
        final String append = text.endsWith("\n") ? "\n" : "";
        return String.join("\n", retLines) + append;
    }

    /** Applies a function to the last line only */
    public static @Nonnull String lambdaAllLines(
            @Nonnull final String text, @Nonnull final Function<String, String> lambda) {
        final String[] lines = text.split("\n");
        final String tabbedBody = Arrays.stream(lines)
                .filter(Strings::isNotBlank)
                .map(lambda)
                .collect(Collectors.joining("\n"));
        final String append = text.endsWith("\n") ? "\n" : "";
        return tabbedBody + append;
    }

    // helpers

    private static @Nonnull String removeEndGroups(@Nonnull final Pattern pattern, @Nonnull final String str) {
        final Matcher matcher = pattern.matcher(str);
        return matcher.find()
                ? matcher.replaceFirst(Matcher.quoteReplacement(matcher.group(2)))
                : str;
    }
}
