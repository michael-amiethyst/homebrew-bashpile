package com.bashpile;

import com.bashpile.maintests.BashpileTest;
import com.bashpile.shell.ExecutionResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.bashpile.shell.BashShell.runAndJoin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BashpileMainIntegrationTest extends BashpileTest {

    private static final Logger log = LogManager.getLogger(BashpileMainIntegrationTest.class);

    private static boolean bprDeployed = false;

    @Test @Timeout(10) @Order(5)
    public void bprDeploysSuccessfully() throws IOException {
        log.debug("In bprDeploysSuccessfully");

        final String translatedFilename = "bin/bpr";
        final String command = "bin/bpc --outputFile=%s bin/bpr.bps".formatted(translatedFilename);
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        bprDeployed = true;
        final List<String> lines = results.stdoutLines();
        assertEquals(translatedFilename, lines.get(lines.size() - 1));
    }

    @Test @Timeout(10) @Order(10)
    public void bpcTranspiles() throws IOException {
        log.debug("In noSubCommandTest");
        Assumptions.assumeTrue(bprDeployed);

        final String command = "bin/bpc src/test/resources/testrigData.bps";
        final String translatedFilename = "src/test/resources/testrigData.bps.bpt";
        try {
            final ExecutionResults results = runAndJoin(command);
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            final List<String> lines = results.stdoutLines();
            assertEquals(translatedFilename, lines.get(lines.size() - 1));
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }

    @Test @Timeout(20) @Order(11)
    public void bprWorks() throws IOException {
        log.debug("In noSubCommandTest");
        Assumptions.assumeTrue(bprDeployed);

        // testrigTree has bpr in shebang line
        final String command = "bin/bpr src/test/resources/scripts/bprShebang.bps";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
    }

    @Test @Timeout(8) @Order(20)
    public void noSubCommandWithNoExtensionTranspiles() throws IOException {
        log.debug("In noSubCommandWithNoExtensionTranspiles");
        Assumptions.assumeTrue(bprDeployed);

        final String command = "bin/bpc src/test/resources/testrigData";
        final String translatedFilename = "src/test/resources/testrigData.bpt";
        final ExecutionResults results = runAndJoin(command);
        try {
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            final List<String> lines = results.stdoutLines();
            assertEquals(translatedFilename, lines.get(lines.size() - 1));
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }

    @Test @Timeout(8) @Order(30)
    public void noSubCommandWithMissingFileFails() throws IOException {
        log.debug("In noSubCommandWithMissingFileFails");
        Assumptions.assumeTrue(bprDeployed);

        final String command = "bin/bpc src/test/resources/testrigData.fileDoesNotExist";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertFailedExitCode(results);
        assertFalse(Files.exists(Path.of("output.txt")));
    }

    @Test @Timeout(15) @Order(40)
    public void noSubCommandWithOutputSpecifiedTranspiles() throws IOException {
        log.debug("In noSubCommandWithNoExtensionTranspiles");
        Assumptions.assumeTrue(bprDeployed);

        final String translatedFilename = "src/test/resources/testrigData.example.bps";
        final String command = "bin/bpc src/test/resources/testrigData --outputFile " + translatedFilename;
        ExecutionResults results = runAndJoin(command);
        try {
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            List<String> lines = results.stdoutLines();
            assertEquals(translatedFilename, lines.get(lines.size() - 1));

            // 2nd run to verify overwrites OK
            results = runAndJoin(command);
            assertSuccessfulExitCode(results);
            lines = results.stdoutLines();
            assertEquals(translatedFilename, lines.get(lines.size() - 1));
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }
}
