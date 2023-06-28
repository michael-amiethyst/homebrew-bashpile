package com.bashpile.commandline;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Runs commands in Bash.  Runs `wsl bash` in Windows.
 */
public class BashExecutor {

    private static final Pattern bogusScreenLine = Pattern.compile(
            "your \\d+x\\d+ screen size is bogus. expect trouble\r\n");

    private static final Logger log = LogManager.getLogger(BashExecutor.class);

    public static Pair<String, Integer> run(final String bashText) throws IOException {
        return runHelper(bashText, true);
    }

    public static Pair<String, Integer> failableRun(final String bashText) throws IOException {
        return runHelper(bashText, false);
    }

    private static Pair<String, Integer> runHelper(
            final String bashText, final boolean throwOnBadExitCode) throws IOException {
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
            return Pair.of(processedCommandResultText, exitCode);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
    }
}
