package com.bashpile;

import com.bashpile.shell.BashShell;
import com.bashpile.shell.ExecutionResults;
import com.google.common.annotations.VisibleForTesting;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.misc.Interval;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for BashpileLexer.
 */
public class Lexers {

    /**
     * Maps a Bash Command to if it is valid (installed, executable and reachable) or not.
     * <br>
     * Is a Hashtable to support testing in parallel.
     */
    private static final Map<String, Boolean> COMMAND_TO_VALIDITY_CACHE = new Hashtable<>(100);

    /** A regex for a valid Bash identifier */
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*");

    private static final Pattern FILE_PATTERN = Pattern.compile("^(?:/?[a-zA-Z_.][a-zA-Z0-9_.]*)+");

    /** A regex for a Bash assignment */
    private static final Pattern ASSIGN_PATTERN =
            Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*=(\"[^\"]*\"|'[^']*'|[^ ]+|[0-9]+)+\\s*");

    /** A regex for the `type` result of `script is script` */
    private static final Pattern LOCAL_SCRIPT_PATTERN = Pattern.compile("(^[a-zA-Z_][a-zA-Z0-9_]*) is \1");

    /** File is really a command */
    private static final List<String> COMMAND_TYPES = List.of("alias", "function", "builtin", "file");

    /** Should be excluded from being a Linux command */
    private static final List<String> BASHPILE_KEYWORDS = List.of("return", "readonly", "unset");

    /**
     * Checks if the command portion of the input Bash line is a valid Bash command.
     * <br>
     * Running 'type' to verify is expensive so we both check if the command is valid with a Regex and cache results.
     *
     * @param charStream From the `_input` of a Semantic Predicate in the BashpileLexer
     * @return Checks if the parsed command is valid.
     */
    @SuppressWarnings("unused")
    public static boolean isLinuxCommand(@Nonnull final CharStream charStream) {
        // guard
        if (charStream.size() == 0) {
            return false;
        }

        // body
        boolean startOfLine = true;
        // scan backwards until at start, the last newline or a character besides space or newline
        int i = charStream.index() - 1;
        while(i >= 0 && !Objects.equals(charStream.getText(Interval.of(i, i)), "\n")) {
            String curr = charStream.getText(Interval.of(i, i));
            if (!curr.equals(" ")) {
                startOfLine = false;
                break;
            }
            i--;
        }
        if (startOfLine) {
            // chop off everything before charStream's index
            return isLinuxCommand(charStream.getText(Interval.of(charStream.index(), charStream.size())));
        } else {
            return false;
        }
    }

    /**
     * Checks if the command portion of the input Bash line is a valid Bash command.
     * <br>
     * Running 'type' to verify is expensive so we both check if the command is valid with a Regex and cache results.
     *
     * @param bashLine A line of Bash script to check.
     * @return Checks if the parsed command is valid.
     */
    @VisibleForTesting
    /* package */ static boolean isLinuxCommand(@Nonnull String bashLine) {
        // guard
        if (StringUtils.isBlank(bashLine) || bashLine.startsWith(" ")) {
            return false;
        }

        // check for var=value preambles and remove
        Matcher match = ASSIGN_PATTERN.matcher(bashLine);
        while (match.find()) {
            bashLine = match.replaceFirst("");
            match = ASSIGN_PATTERN.matcher(bashLine);
        }

        // split on whitespace or Bash command separator
        final String command = bashLine.split("[ \n;]")[0];

        if (COMMAND_TO_VALIDITY_CACHE.containsKey(command)) {
            return COMMAND_TO_VALIDITY_CACHE.get(command);
        }

        try {
            // may need a 'and not find with createsStatementRegex' when we add file path recognition to shell lines
            if (COMMAND_PATTERN.matcher(command).matches()) {
                ExecutionResults results = BashShell.runAndJoin("type -t " + command);
                // exclude keywords like 'function'

                final String typeResults = results.stdout().trim();
                boolean ret = results.exitCode() == ExecutionResults.SUCCESS
                        && (COMMAND_TYPES.contains(typeResults))
                        && !BASHPILE_KEYWORDS.contains(command);
                COMMAND_TO_VALIDITY_CACHE.put(command, ret);
                return ret;
            } else if (FILE_PATTERN.matcher(command).matches()) {
                COMMAND_TO_VALIDITY_CACHE.put(command, true);
                return true;
            } else {
                COMMAND_TO_VALIDITY_CACHE.put(command, false);
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }
}
