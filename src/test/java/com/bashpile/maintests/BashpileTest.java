package com.bashpile.maintests;

import com.bashpile.BashpileMainHelper;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.UserError;
import com.bashpile.shell.BashShell;
import com.bashpile.shell.ExecutionResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

// TODO right after 0.23.0 - create GitHubAction/dockerfile for redhat
/** Base class for Bashpile Tests */
abstract public class BashpileTest {

    private static final Logger LOG = LogManager.getLogger(BashpileTest.class);

    /** Asserts the exit code is 0 */
    protected static void assertSuccessfulExitCode(@Nonnull final ExecutionResults executionResults) {
        assertEquals(ExecutionResults.SUCCESS, executionResults.exitCode(),
                "Found failing (non-0) exit code: %s.  Full text results:\n%s".formatted(
                        executionResults.exitCode(), executionResults.stdout()));
    }

    /** Asserts exit code is NOT zero */
    protected static void assertFailedExitCode(@Nonnull final ExecutionResults executionResults) {
        assertNotEquals(ExecutionResults.SUCCESS, executionResults.exitCode(),
                "Found successful exit code (0) when expecting errored exit code.  Full text results:\n%s".formatted(
                        executionResults.stdout()));
    }

    /** Runs {@code bashText} in a Bash environment */
    protected static @Nonnull ExecutionResults runText(@Nonnull final String bashText, @Nullable String... args) {
        LOG.debug("Start of:\n{}", bashText);
        try {
            return execute(BashpileMainHelper.transpileScript(bashText), args);
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    /** Runs {@code file} from src/test/resources/scripts as a script in a Bash environment. */
    protected static @Nonnull ExecutionResults runPath(@Nonnull final Path file) {
        final Path filename = !file.isAbsolute() ? Path.of("src/test/resources/scripts/" + file) : file;
        LOG.debug("Start of {}", filename);
        try {
            return execute(BashpileMainHelper.transpileNioFile(filename), null);
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    // helpers

    private static @Nonnull ExecutionResults execute(@Nonnull final String bashScript, @Nullable final String[] args) {
        LOG.debug("In {}", System.getProperty("user.dir"));
        try {
            return BashShell.runAndJoin(bashScript, args);
        } catch (UserError | AssertionError e) {
            throw e;
        } catch (Throwable e) {
            throw createExecutionException(e, bashScript, args);
        }
    }

    private static BashpileUncheckedException createExecutionException(
            @Nonnull final Throwable e, @Nullable final String bashScript, @Nullable String[] args) {
        if (e.getMessage() != null && e.getMessage().contains("shellcheck") && e.getMessage().contains("not found")) {
            return new BashpileUncheckedException("Please install shellcheck (e.g. via `brew install shellcheck`)");
        }
        String msg;
        if (bashScript != null) {
            args = Objects.requireNonNullElse(args, new String[0]);
            msg = "\nCouldn't run Args: (%s), Script: %s".formatted(String.join(" ", args), bashScript);
        } else {
            msg = "\nCouldn't parse input";
        }
        if (e.getMessage() != null) {
            msg += " because of:\n`%s`".formatted(e.getMessage().trim());
        }
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            msg += "\n caused by `%s`".formatted(e.getCause().getMessage().trim());
        }
        return new BashpileUncheckedException(msg, e);
    }
}
