package com.bashpile;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Holds STDIN, the *nix exit code and STDOUT */
public record ExecutionResults(String stdin, int exitCode, String stdout) {

    public static final Pattern linesPattern = Pattern.compile("\r?\n");

    public @Nonnull List<String> stdinLines() {
        return linesPattern.splitAsStream(stdin).collect(Collectors.toList());
    }

    public @Nonnull List<String> stdoutLines() {
        if (stdout.matches(".*?\r?\n\r?\n")) {
            // split has bug where trailing blank lines are ignored
            // add a final ';' then trim the line off to end up with a list with trailing blanks
            List<String> lines = linesPattern.splitAsStream(stdout + ";").collect(Collectors.toList());
            return lines.subList(0, lines.size() - 1);
        } // else
        return linesPattern.splitAsStream(stdout).collect(Collectors.toList());
    }
}
