package com.bashpile.commandline;

import com.bashpile.ExecutionResults;
import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Runs commands in Bash.  Runs `wsl bash` in Windows.
 */
public class BashExecutor {

    private static final Pattern bogusScreenLine = Pattern.compile(
            "your \\d+x\\d+ screen size is bogus. expect trouble\r\n");

    private static final Logger log = LogManager.getLogger(BashExecutor.class);

    public static @Nonnull ExecutionResults run(@Nonnull final String bashText) throws IOException {
        return runHelper(bashText, true);
    }

    public static @Nonnull ExecutionResults failableRun(@Nonnull final String bashText) throws IOException {
        return runHelper(bashText, false);
    }

    private static @Nonnull ExecutionResults runHelper(
            @Nonnull final String bashText, final boolean throwOnBadExitCode) throws IOException {
        log.info("Executing bash text:\n" + bashText);
        final ProcessBuilder builder = new ProcessBuilder();
        if (isWindows()) {
            log.trace("Detected windows");
            builder.command("wsl");
        } else {
            log.trace("Detected 'nix");
            builder.command("bash");
        }
        builder.redirectErrorStream(true);

        int exitCode;
        try (final CommandLineExecutor commandLine = CommandLineExecutor.create(builder.start())) {

            // on Windows 11 `set -e` causes an exit code of 1 unless we do a sub-shell
            commandLine.write("bash\n");
            commandLine.write(StringUtils.appendIfMissing(bashText, "\n"));

            // exit from sub shell and shell
            commandLine.write("exit $?\n");
            commandLine.write("exit $?\n");

            exitCode = commandLine.join();

            final String stdoutString = commandLine.getStdOut();
            if (exitCode != 0 && throwOnBadExitCode) {
                throw new BashpileUncheckedException(
                        "Found failing (non-0) 'nix exit code: " + exitCode + ".  Full text results:\n" + stdoutString);
            }
            // return buffer stripped of random error lines
            log.trace("Shell output before processing: [{}]", stdoutString);
            final String processedCommandResultText =
                    bogusScreenLine.matcher(stdoutString).replaceAll("").trim();
            return new ExecutionResults(bashText, exitCode, processedCommandResultText);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
    }
}
