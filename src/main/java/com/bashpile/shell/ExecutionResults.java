package com.bashpile.shell;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Holds STDIN, the Linux exit code and STDOUT */
public record ExecutionResults(@Nonnull String stdin, int exitCode, @Nonnull String stdout) {

    /** The Linux exit code indicating success -- 0 */
    public static final int SUCCESS = 0;

    /** The Linux errored exit code indicating a command not found -- 127 */
    public static final int COMMAND_NOT_FOUND = 127;

    private static final Pattern WINDOWS_LINE_ENDINGS = Pattern.compile("\r\n");

    public ExecutionResults(@Nonnull final String stdin, final int exitCode, @Nonnull final String stdout) {
        // convert windows line ending to Linux line endings
        this.stdin = WINDOWS_LINE_ENDINGS.matcher(stdin).replaceAll("\n");
        this.exitCode = exitCode;
        this.stdout = WINDOWS_LINE_ENDINGS.matcher(stdout).replaceAll("\n");
    }

    /**
     * Return stdin as a list of lines (not ending with '\n').
     * <br>
     * Ending blank lines are not preserved. As a workaround check {@link #stdin()} instead.
     *
     * @return A list of lines.  The end elements of the list may not be the empty String.
     */
    public @Nonnull List<String> stdinLines() {
        return Arrays.asList(stdin.split("\n"));
    }

    /**
     * Return stdout as a list of lines (not ending with '\n').
     * <br>
     * Ending blank lines are not preserved. As a workaround check {@link #stdout()} instead.
     *
     * @return A list of lines.  The end elements of the list may not be the empty String.
     */
    public @Nonnull List<String> stdoutLines() {
        return Arrays.asList(stdout.split("\n"));
    }
}
