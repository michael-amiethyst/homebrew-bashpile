package com.bashpile;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Stack;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Our String utilities class */
public class Strings extends StringUtils {

    /** Finds starting/ending parenthesis */
    private static final Pattern PARENTHESIS = Pattern.compile("^\\(.*\\)$");

    /**
     * Checks if str is wrapped in parentheses like "(expr)" but not "(expr)(expr)".
     *
     * @param str The string to check.
     * @return If str is wrapped in matching parentheses or not.
     */
    public static boolean inParentheses(@Nonnull final String str) {
        return PARENTHESIS.matcher(str).matches()
                && matchingParenthesis(str)
                && (str.length() < 2 || matchingParenthesis(str.substring(1, str.length() - 1)));
    }

    /**
     * Remove quotes from the start and end of the string, if present.
     *
     * @param str The string to unquote.
     * @return The string stripped of leading and trailing quotes, if they match.
     */
    public static @Nonnull String unquote(@Nonnull final String str) {
        String toRemove = str.startsWith("\"") ? "\"" : "'";
        String stripped = StringUtils.removeStart(str, toRemove);
        return StringUtils.removeEnd(stripped, toRemove);
    }

    /**
     * Remove parentheses from the start and end of the string, if present.
     *
     * @param str The string to unparenthesize.
     * @return The string stripped if leading and trailing parenthesis, if they match.
     */
    public static @Nonnull String unparenthesize(@Nonnull final String str) {
        if (inParentheses(str)) { return str.substring(1, str.length() - 1); }
        return str;
    }

    /** @see StringEscapeUtils#unescapeJava(String) */
    public static @Nonnull String unescape(@Nonnull final String text) {
        return StringEscapeUtils.unescapeJava(text);
    }

    /**
     * Left-aligns text, preserves spacing of subsequent lines relative to first line.
     * <br>
     * E.g.  If the first non-blank line started with 8 spaces every line would have the first 8 characters removed.
     * Each line is stripped of trailing whitespace too.  If initial text ended with a newline it will be added back.
     *
     * @param text The text to dedent.
     * @return The first non-blank line text's initial whitespace chars count stripped from every subsequent line.
     */
    public static @Nonnull String dedent(@Nonnull final String text) {
        // find leading whitespace of first non-blank line.  Strip that many chars from each line
        final String[] lines = text.split("\n");
        int i = 0;
        while (isBlank(lines[i])) {
            i++;
        }
        final String line = lines[i];
        final int spaces = line.length() - line.stripLeading().length();
        final String trailingNewline = text.endsWith("\n") ? "\n" : "";
        return Arrays.stream(lines)
                .filter(str -> !Strings.isBlank(str))
                .map(str -> str.substring(spaces))
                .map(String::stripTrailing)
                .collect(Collectors.joining("\n"))
                + trailingNewline;
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

    /** From <a href="https://www.javatpoint.com/balanced-parentheses-in-java">Java Tutorials Point</a> */
    private static boolean matchingParenthesis(@Nonnull final String str) {
        Stack<Character> openCharStack = new Stack<>();
        char[] charArray = str.toCharArray();
        for (char current : charArray) {
            if (current == '{' || current == '[' || current == '(') {
                openCharStack.push(current);
                continue;
            }
            if (")]}".contains(String.valueOf(current)) && openCharStack.isEmpty()) {
                return false;
            }
            switch (current) {
                case ')' -> {
                    if (openCharStack.pop() != '(') { return false; }
                }
                case '}' -> {
                    if (openCharStack.pop() != '{') { return false; }
                }
                case ']' -> {
                    if (openCharStack.pop() != '[') { return false; }
                }
            }
        }
        return openCharStack.isEmpty();
    }
}
