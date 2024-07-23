package com.bashpile.shell;

import com.bashpile.Strings;
import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Runs commands in Bash.  Runs `wsl bash` in Windows.
 */
public class BashShell implements Closeable {

    private static final Pattern BOGUS_SCREEN_LINE = Pattern.compile(
            "your \\d+x\\d+ screen size is bogus. expect trouble\r?\n");

    /**
     * @see <a href="https://unix.stackexchange.com/questions/564981/what-is-this-3jh2j">Stack Exchange</a>
     */
    private static final Pattern CLEAR_CONTROL_CODE = Pattern.compile(".\\[3J.\\[H.\\[2J\r?\n");

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
    public static @Nonnull ExecutionResults runAndJoin(@Nonnull final String bashString)
            throws IOException {
        return runAndJoin(bashString, null);
    }

    /**
     * Executes @{link bashString} like it was at a Bash command prompt in spawned background threads.
     *
     * @param bashString We run these command(s) or text.  bashString may be large, like a whole program.
     * @param args The arguments
     * @return The STDIN, STDOUT and exit code wrapped in an ExecutionResults object.
     * @throws IOException and {@link BashpileUncheckedException} wrapping
     *  ExecutionException, InterruptedException or TimeoutException.
     */
    public static @Nonnull ExecutionResults runAndJoin(@Nonnull final String bashString, @Nullable final String[] args)
            throws IOException {
        try(final BashShell shell = runAsync(bashString, args)) {
            return shell.join();
        }
    }

    /**
     * Runs bashString in a non-interactive login Bash shell and supporting worker threads in the background.
     *
     * @param bashString The Bash script to run
     * @return A BashShell holding the running threads.
     * @throws IOException on error.
     *
     * @see #sendTerminationSignal()
     * @see #join()
     */
    public static @Nonnull BashShell runAsync(@Nonnull String bashString, @Nullable String[] args)
            throws IOException {
        args = Objects.requireNonNullElse(args, new String[0]);
        // info for large runs, trace for small commands
        final String message = "Executing bash text:\n" + bashString;
        if (bashString.length() > 20) {
            LOG.info(message);
        } else {
            LOG.trace(message);
        }

        // run our CommandLine process in background threads
        final IoManager commandLine = IoManager.of(spawnLinuxProcess());
        final BashShell processes = new BashShell(commandLine, bashString);

        // on Windows 11 `set -e` causes an exit code of 1 unless we do a sub-shell
        // also the Linux process starts in the user's shell, which may not be Bash (e.g. zsh)
        commandLine.writeLn("bash --login");

        // this is the core of the method
        final String filename = "bashshell.bash";
        final Path filepath = Path.of(filename);
        if (args.length != 0) {
            // write bashString to script file and execute script with args
            Files.write(filepath, bashString.getBytes());
            bashString = "chmod +x %s && ./%s %s; rm -f %s"
                    .formatted(filename, filename, String.join(" ", args), filename);
        }

        // some OS's (Debian at least) drop the original PATH info during brew install
        final String envPath = Strings.defaultString(System.getenv("PATH"), ".");
        commandLine.writeLn("export PATH=%s:$PATH; %s".formatted(envPath, bashString));

        // exit from subshell
        commandLine.writeLn("exit $?");
        // exit from shell
        commandLine.writeLn("exit $?");
        return processes;
    }

    /* package */ BashShell(@Nonnull final IoManager ioManager, @Nonnull final String bashScript) {
        this.ioManager = ioManager;
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
     */
    public @Nonnull ExecutionResults join() {
        // wait for background threads to complete
        final Pair<Integer, String> ret = ioManager.join();

        // munge stdout -- strip out inappropriate error lines
        String stdout = ret.getValue();
        LOG.trace("Shell output before processing: [{}]", stdout);
        stdout = BOGUS_SCREEN_LINE.matcher(stdout).replaceAll("");
        stdout = CLEAR_CONTROL_CODE.matcher(stdout).replaceAll("");
        // replace trailing newline if it was stripped out earlier
        if (!stdout.isEmpty()) {
            stdout = StringUtils.appendIfMissing(stdout, "\n");
        }

        return new ExecutionResults(bashScript, ret.getKey(), stdout);
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

    private static @Nonnull Process spawnLinuxProcess() throws IOException {
        ProcessBuilder linuxProcess = createProcessBuilder();
        linuxProcess.redirectErrorStream(true);
        return linuxProcess.start();
    }

    private static @Nonnull ProcessBuilder createProcessBuilder() {
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
