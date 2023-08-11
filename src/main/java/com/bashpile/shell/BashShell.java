package com.bashpile.shell;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Runs commands in Bash.  Runs `wsl bash` in Windows.
 */
public class BashShell implements Closeable {

    private static final Pattern BOGUS_SCREEN_LINE = Pattern.compile(
            "your \\d+x\\d+ screen size is bogus. expect trouble\r?\n");

    private static final Logger LOG = LogManager.getLogger(BashShell.class);

    @Nonnull
    private final IoManager ioManager;

    @Nonnull
    private final String bashScript;

    /**
     * Executes @{link bashString} like it was at a Bash command prompt in spawned background threads.
     *
     * @param bashString We run these command(s) or text.  bashString may be large, like a whole program.
     * @return The STDIN, STDOUT and exit code wrapped in an ExecutionResults object.
     * @throws IOException and {@link BashpileUncheckedException} wrapping
     *  ExecutionException, InterruptedException or TimeoutException.
     */
    public static @Nonnull ExecutionResults runAndJoin(@Nonnull String bashString) throws IOException {
        try(final BashShell shell = runAsync(bashString)) {
            return shell.join();
        }
    }

    /**
     * Runs bashString and supporting worker threads in the background
     *
     * @param bashString The Bash script to run
     * @return A BashShell holding the running threads.
     * @throws IOException on error.
     *
     * @see #sendTerminationSignal()
     * @see #join()
     */
    public static @Nonnull BashShell runAsync(@Nonnull String bashString) throws IOException {
        bashString = prependAdditionalPath(bashString);

        LOG.info("Executing bash text:\n" + bashString);

        // run our CommandLine process in background threads
        final IoManager commandLine = IoManager.of(spawnLinuxProcess());
        final BashShell processes = new BashShell(commandLine, bashString);

        // on Windows 11 `set -e` causes an exit code of 1 unless we do a sub-shell
        // also the Linux process starts in the user's shell, which may not be Bash (e.g. zsh)
        commandLine.writeLn("bash");

        // this is the core of the method
        commandLine.writeLn(bashString);

        // exit from subshell
        commandLine.writeLn("exit $?");
        // exit from shell
        commandLine.writeLn("exit $?");
        return processes;
    }

    public BashShell(@Nonnull final IoManager ioManager, @Nonnull final String bashScript) {
        this.ioManager = ioManager;
        // TODO get rid of bashScript
        this.bashScript = bashScript;
    }

    /**
     * This is the same as running `kill` on the async process.  You still need to call {@link #join()}
     */
    public void sendTerminationSignal() {
        ioManager.sigterm();
    }

    /**
     * Shuts down the background threads gracefully
     *
     * @return The ExecutionResults.
     * @throws IOException on error.
     */
    public @Nonnull ExecutionResults join() throws IOException {
        try {
            // wait for background threads to complete
            final int exitCode = ioManager.join();

            // munge stdout -- strip out inappropriate error lines
            String stdoutString = ioManager.getStdOut();
            LOG.trace("Shell output before processing: [{}]", stdoutString);
            stdoutString = BOGUS_SCREEN_LINE.matcher(stdoutString).replaceAll("");
            return new ExecutionResults(bashScript, exitCode, stdoutString);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    @Override
    public void close() {
        try {
            ioManager.close();
        } finally {
            IOUtils.closeQuietly(ioManager);
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

    private static String prependAdditionalPath(@Nonnull final String bashString) throws IOException {
        final Properties prop = new Properties();
        final String fileName = "src/main/resources/config.properties";
        try (FileInputStream fis = new FileInputStream(fileName)) {
            prop.load(fis);
        }
        Object additionalPath = prop.get("additionalPath");
        return additionalPath != null ? "export PATH=$PATH:%s\n%s".formatted(additionalPath, bashString) : bashString;
    }
}
