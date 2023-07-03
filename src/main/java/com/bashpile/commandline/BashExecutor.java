package com.bashpile.commandline;

import com.bashpile.ExecutionResults;
import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.appendIfMissing;

/**
 * Runs commands in Bash.  Runs `wsl bash` in Windows.
 */
public class BashExecutor {

    private static final Pattern bogusScreenLine = Pattern.compile(
            "your \\d+x\\d+ screen size is bogus. expect trouble\r\n");

    private static final Logger log = LogManager.getLogger(BashExecutor.class);

    /**
     * Executes @{link bashString} like it was at a Bash command prompt in spawned background threads.
     *
     * @param bashString We run these command(s) or text.  bashString may be large, like a whole program.
     * @return The STDIN, STDOUT and exit code wrapped in an ExecutionResults object.
     * @throws IOException and {@link BashpileUncheckedException} wrapping
     *  ExecutionException, InterruptedException or TimeoutException.
     */
    public static @Nonnull ExecutionResults run(@Nonnull final String bashString) throws IOException {
        // guard
        log.info("Executing bash text:\n" + bashString);

        // create and configure our ProcessBuilder
        final ProcessBuilder builder = new ProcessBuilder();
        if (isWindows()) {
            log.trace("Detected windows");
            builder.command("wsl");
        } else {
            log.trace("Detected 'nix");
            builder.command("bash");
        }
        builder.redirectErrorStream(true);

        // run our CommandLine process in background threads
        int exitCode;
        try (final CommandLineExecutor commandLine = CommandLineExecutor.create(builder.start())) {

            // on Windows 11 `set -e` causes an exit code of 1 unless we do a sub-shell
            commandLine.write("bash\n");

            // this is the core of the method
            final String bashTextBlock = appendIfMissing(bashString, "\n");
            commandLine.write(bashTextBlock);

            // exit from subshell and shell
            commandLine.write("exit $?\n");
            commandLine.write("exit $?\n");

            // wait for background threads to complete
            exitCode = commandLine.join();

            // munge stdout
            final String stdoutString = commandLine.getStdOut();
            log.trace("Shell output before processing: [{}]", stdoutString);
            // return buffer stripped of random error lines
            final String processedCommandResultText =
                    bogusScreenLine.matcher(stdoutString).replaceAll("").trim();
            return new ExecutionResults(bashString, exitCode, processedCommandResultText);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
    }
}
