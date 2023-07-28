package com.bashpile.commandline;

import com.bashpile.exceptions.BashpileUncheckedException;
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

    private static final Pattern BOGUS_SCREEN_LINE = Pattern.compile(
            "your \\d+x\\d+ screen size is bogus. expect trouble\r?\n");

    private static final Logger LOG = LogManager.getLogger(BashExecutor.class);

    /**
     * Executes @{link bashString} like it was at a Bash command prompt in spawned background threads.
     *
     * @param bashString We run these command(s) or text.  bashString may be large, like a whole program.
     * @return The STDIN, STDOUT and exit code wrapped in an ExecutionResults object.
     * @throws IOException and {@link BashpileUncheckedException} wrapping
     *  ExecutionException, InterruptedException or TimeoutException.
     */
    public static @Nonnull ExecutionResults run(@Nonnull final String bashString) throws IOException {
        LOG.info("Executing bash text:\n" + bashString);

        // run our CommandLine process in background threads
        int exitCode;
        try (final IoManager commandLine = IoManager.spawnConsumer(spawnLinuxProcess())) {

            // on Windows 11 `set -e` causes an exit code of 1 unless we do a sub-shell
            // also the Linux process starts in the user's shell, which may not be Bash (e.g. zsh)
            commandLine.writeLn("bash");

            // this is the core of the method
            commandLine.writeLn(bashString);

            // exit from subshell
            commandLine.writeLn("exit $?");
            // exit from shell
            commandLine.writeLn("exit $?");

            // wait for background threads to complete
            exitCode = commandLine.join();

            // munge stdout -- strip out inappropriate error lines
            String stdoutString = commandLine.getStdOut();
            LOG.trace("Shell output before processing: [{}]", stdoutString);
            stdoutString = BOGUS_SCREEN_LINE.matcher(stdoutString).replaceAll("");
            return new ExecutionResults(bashString, exitCode, stdoutString);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    // helpers

    private static Process spawnLinuxProcess() throws IOException {
        ProcessBuilder linuxProcess = createProcessBuilder();
        linuxProcess.redirectErrorStream(true);
        return linuxProcess.start();
    }

    private static ProcessBuilder createProcessBuilder() {
        final ProcessBuilder builder = new ProcessBuilder();
        if (isWindows()) {
            LOG.trace("Detected windows");
            return builder.command("wsl");
        } // else
        LOG.trace("Detected *nix");
        return builder.command("bash");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
    }
}
