package com.bashpile;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

/** Holds STDIN, the *nix exit code and STDOUT */
public record ExecutionResults(String stdin, int exitCode, String stdout) {

    public static final Pattern linesPattern = Pattern.compile("\r?\n");

    public @Nonnull String[] stdinLines() {
        return linesPattern.split(stdin);
    }

    public @Nonnull String[] stdoutLines() {
        return linesPattern.split(stdout);
    }
}
