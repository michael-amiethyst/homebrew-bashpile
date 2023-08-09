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
import java.util.stream.Stream;

import static com.bashpile.shell.BashShell.runAndJoin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BashpileMainIntegrationTest extends BashpileTest {

    private static final Logger log = LogManager.getLogger(BashpileMainIntegrationTest.class);

    @Test @Timeout(8) @Order(10)
    public void noSubCommandTranspiles() throws IOException {
        log.debug("In noSubCommandTest");

        final String command = "bin/bpc src/test/resources/testrigData.bps";
        final String translatedFilename = "src/test/resources/testrigData.bps.bpt";
        try {
            final ExecutionResults results = runAndJoin(command);
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            final List<String> lines = results.stdoutLines();
            assertEquals(translatedFilename, lines.get(lines.size() - 1));
            try (final Stream<Path> outputFiles = Files.walk(Path.of("."))
                    .filter(path -> path.getFileName().toString().startsWith("output"))) {
                assertEquals(0, outputFiles.count());
            }
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }

    @Test @Timeout(5) @Order(20)
    public void noSubCommandWithNoExtensionTranspiles() throws IOException {
        log.debug("In noSubCommandWithNoExtensionTranspiles");

        final String command = "bin/bpc src/test/resources/testrigData";
        final String translatedFilename = "src/test/resources/testrigData.bpt";
        final ExecutionResults results = runAndJoin(command);
        try {
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            final List<String> lines = results.stdoutLines();
            assertEquals(translatedFilename, lines.get(lines.size() - 1));
            try (final Stream<Path> outputFiles = Files.walk(Path.of("."))
                    .filter(path -> path.getFileName().toString().startsWith("output"))) {
                assertEquals(0, outputFiles.count());
            }
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }

    @Test @Timeout(5) @Order(30)
    public void noSubCommandWithMissingFileFails() throws IOException {
        log.debug("In noSubCommandWithMissingFileFails");

        final String command = "bin/bpc src/test/resources/testrigData.fileDoesNotExist";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertFailedExitCode(results);
        assertFalse(Files.exists(Path.of("output.txt")));
    }

    @Test @Timeout(10) @Order(20)
    public void noSubCommandWithOutputSpecifiedTranspiles() throws IOException {
        log.debug("In noSubCommandWithNoExtensionTranspiles");

        final String translatedFilename = "src/test/resources/testrigData.example.bps";
        final String command = "bin/bpc src/test/resources/testrigData --outputFile " + translatedFilename;
        ExecutionResults results = runAndJoin(command);
        try {
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            List<String> lines = results.stdoutLines();
            assertEquals(translatedFilename, lines.get(lines.size() - 1));
            try (final Stream<Path> outputFiles = Files.walk(Path.of("."))
                    .filter(path -> path.getFileName().toString().startsWith("output"))) {
                assertEquals(0, outputFiles.count());
            }

            // 2nd run to verify overwrites OK
            results = runAndJoin(command);
            assertSuccessfulExitCode(results);
            lines = results.stdoutLines();
            assertEquals(translatedFilename, lines.get(lines.size() - 1));
            try (final Stream<Path> outputFiles = Files.walk(Path.of("."))
                    .filter(path -> path.getFileName().toString().startsWith("output"))) {
                assertEquals(0, outputFiles.count());
            }
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }
}
