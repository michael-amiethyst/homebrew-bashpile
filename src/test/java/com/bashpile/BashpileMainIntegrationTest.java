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
import static org.junit.jupiter.api.Assertions.*;

/**
 * If we invoke bpc directly it uses the shebang to find the brew installed bpr and the installed jar.
 * However, we want the local bpr and the local jar so we call `bin/bpr bin/bpc ...`.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BashpileMainIntegrationTest extends BashpileTest {

    private static final Logger log = LogManager.getLogger(BashpileMainIntegrationTest.class);

    private static boolean bprDeployed = false;

    @Test @Timeout(20) @Order(5)
    public void bprDeploysSuccessfully() throws IOException {
        log.info("In bprDeploysSuccessfully");

        // ensure bin/bpr is using /n instead of /r/n
        final String translatedFilename = "bin/bpr";
        ensureLinuxLineEndings(translatedFilename);
        ensureLinuxLineEndings("bin/bpc");

        // weird, intermittent errors running bpr in Java like characters getting skipped
        int exitCode = ExecutionResults.GENERIC_FAILURE;
        int loops = 0;
        ExecutionResults results = null;
        while(exitCode != ExecutionResults.SUCCESS && loops++ < 3) {
            final String command = "bin/bpr bin/bpc --outputFile=%s bin/bpr.bps".formatted(translatedFilename);
            results = runAndJoin(command);
            log.trace("Output text:\n{}", results.stdout());
            exitCode = results.exitCode();
        }

        assertSuccessfulExitCode(results);
        bprDeployed = true;
        final List<String> lines = results.stdoutLines();
        assertTrue(lines.get(lines.size() - 1).endsWith(translatedFilename));
    }

    private static void ensureLinuxLineEndings(String translatedFilename) throws IOException {
        // from https://superuser.com/a/1066353/1850749
        final String awkCommand = """
                awk 'BEGIN{RS="\\1";ORS="";getline;gsub("\\r","");print>ARGV[1]}' %s""".formatted(translatedFilename);
        ExecutionResults results1 = runAndJoin(awkCommand);
        assertSuccessfulExitCode(results1);
    }

    @Test @Timeout(10) @Order(10)
    public void bpcTranspiles() throws IOException {
        log.info("In noSubCommandTest");
        Assumptions.assumeTrue(bprDeployed);

        final String command = "bin/bpr bin/bpc src/test/resources/testrigData.bps";
        final String translatedFilename = "src/test/resources/testrigData.bps.bpt";
        try {
            final ExecutionResults results = runAndJoin(command);
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            final List<String> lines = results.stdoutLines();
            assertTrue(lines.get(lines.size() - 1).endsWith(translatedFilename));
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }

    @Test @Timeout(20) @Order(11)
    public void bprWorks() throws IOException {
        log.info("In noSubCommandTest");
        Assumptions.assumeTrue(bprDeployed);

        // run with our local (not installed) bpr
        final String command = "bin/bpr src/test/resources/scripts/bprShebang.bps";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
    }

    @Test @Timeout(10) @Order(20)
    public void noSubCommandWithNoExtensionTranspiles() throws IOException {
        log.info("In noSubCommandWithNoExtensionTranspiles");
        Assumptions.assumeTrue(bprDeployed);

        final String command = "bin/bpr bin/bpc src/test/resources/testrigData";
        final String translatedFilename = "src/test/resources/testrigData.bpt";
        final ExecutionResults results = runAndJoin(command);
        try {
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            final List<String> lines = results.stdoutLines();
            assertTrue(lines.get(lines.size() - 1).endsWith(translatedFilename));
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }

    @Test @Timeout(10) @Order(30)
    public void noSubCommandWithMissingFileFails() throws IOException {
        log.info("In noSubCommandWithMissingFileFails");
        Assumptions.assumeTrue(bprDeployed);

        final String command = "bin/bpr bin/bpc src/test/resources/testrigData.fileDoesNotExist";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertFailedExitCode(results);
        assertFalse(Files.exists(Path.of("output.txt")));
    }

    @Test @Timeout(30) @Order(40)
    public void noSubCommandWithOutputSpecifiedTranspiles() throws IOException {
        log.info("In noSubCommandWithNoExtensionTranspiles");
        Assumptions.assumeTrue(bprDeployed);

        final String translatedFilename = "src/test/resources/testrigData.example.bps";
        final String command = "bin/bpr bin/bpc src/test/resources/testrigData --outputFile " + translatedFilename;
        ExecutionResults results = runAndJoin(command);
        try {
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            List<String> lines = results.stdoutLines();
            assertTrue(lines.get(lines.size() - 1).endsWith(translatedFilename));

            // 2nd run to verify overwrites OK
            results = runAndJoin(command);
            assertSuccessfulExitCode(results);
            lines = results.stdoutLines();
            assertTrue(lines.get(lines.size() - 1).endsWith(translatedFilename));
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }

    @Test @Timeout(20) @Order(50)
    public void bprCreateErrorMessagesPropagate() throws IOException {
        log.info("In bprCreateErrorMessagesPropagate");
        Assumptions.assumeTrue(bprDeployed);

        final String bashpileFilename = "src/test/resources/scripts/bprShebang.bps";
        final Path generatedFile = Path.of(bashpileFilename + ".bpt");
        Files.writeString(generatedFile, "Captain James Kirk");
        try {
            final String command = "bin/bpr " + bashpileFilename;
            final ExecutionResults results = runAndJoin(command);
            log.debug("Output text:\n{}", results.stdout());

            assertFailedExitCode(results);
            final List<String> lines = results.stdoutLines();
            assertTrue(lines.size() >= 2);
        } finally {
            Files.deleteIfExists(generatedFile);
        }
    }

    @Test @Timeout(20) @Order(50)
    public void bprDashCWorks() throws IOException {
        log.info("In bpr -c works");
        Assumptions.assumeTrue(bprDeployed);

        // run with our local (not installed) bpr
        final String command = "bin/bpr -c \"print('Hello World')\"";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        assertEquals("Hello World\n", results.stdout());
        assertFalse(Files.exists(Path.of("command.bps")));
    }
}
