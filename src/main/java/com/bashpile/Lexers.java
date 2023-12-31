package com.bashpile;

import com.bashpile.shell.BashShell;
import com.bashpile.shell.ExecutionResults;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
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

    /** A regex for a Bash assignment */
    private static final Pattern ASSIGN_PATTERN =
            Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*=(\"[^\"]*\"|'[^']*'|[0-9]+)*");

    /** File is really a command */
    private static final List<String> COMMAND_TYPES = List.of("alias", "function", "builtin", "file");

    /**
     * Checks if the command portion of the input Bash line is a valid Bash command.
     * <br>
     * Running 'type' to verify is expensive so we both check if the command is valid with a Regex and cache results.
     *
     * @param bashLine A line of Bash script to check.
     * @return Checks if the parsed command is valid.
     */
    @SuppressWarnings("unused")
    public static boolean isLinuxCommand(@Nonnull final String bashLine) {
        String[] lineParts = bashLine.split("[ \t\\n]");

        // check for var=value preambles
        int partsIndex = 0;
        while(ASSIGN_PATTERN.matcher(lineParts[partsIndex]).matches()) {
            partsIndex++;
        }
        final String command = lineParts[partsIndex];

        if (COMMAND_TO_VALIDITY_CACHE.containsKey(command)) {
            return COMMAND_TO_VALIDITY_CACHE.get(command);
        }

        try {
            if (COMMAND_PATTERN.matcher(command).matches()) {
                ExecutionResults results = BashShell.runAndJoin("type -t " + command);
                // exclude keywords like 'function'

                boolean ret = results.exitCode() == ExecutionResults.SUCCESS
                        && COMMAND_TYPES.contains(results.stdout().trim());
                COMMAND_TO_VALIDITY_CACHE.put(command, ret);
                return ret;
            } else {
                COMMAND_TO_VALIDITY_CACHE.put(command, false);
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }
}
