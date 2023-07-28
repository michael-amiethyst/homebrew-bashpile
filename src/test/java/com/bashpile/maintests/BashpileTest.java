package com.bashpile.maintests;

import com.bashpile.BashpileMain;
import com.bashpile.commandline.ExecutionResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

abstract public class BashpileTest {

    private static final Logger LOG = LogManager.getLogger(BashpileTest.class);

    protected static void assertSuccessfulExitCode(@Nonnull final ExecutionResults executionResults) {
        assertEquals(0, executionResults.exitCode(),
                "Found failing (non-0) exit code: %s.  Full text results:\n%s".formatted(
                        executionResults.exitCode(), executionResults.stdout()));
    }

    protected static void assertFailedExitCode(@Nonnull final ExecutionResults executionResults) {
        assertNotEquals(0, executionResults.exitCode(),
                "Found successful exit code (0) when expecting errored exit code.  Full text results:\n%s".formatted(
                        executionResults.stdout()));
    }

    protected @Nonnull ExecutionResults runText(@Nonnull final String bashText) {
        LOG.debug("Start of:\n{}", bashText);
        BashpileMain bashpile = new BashpileMain(bashText);
        return bashpile.execute();
    }

    protected @Nonnull ExecutionResults runPath(@Nonnull final Path file) {
        final Path filename = !file.isAbsolute() ? Path.of("src/test/resources/scripts/" + file) : file;
        LOG.debug("Start of {}", filename);
        final BashpileMain bashpile = new BashpileMain(filename);
        return bashpile.execute();
    }
}
