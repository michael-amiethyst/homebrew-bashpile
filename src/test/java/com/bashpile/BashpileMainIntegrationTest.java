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
 * More of a System test
 * <br>
 * The first deploy methods are really like a CI/CD pipeline to a local deploy.
 * We may want to refactor to Jenkins or break into its own class.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BashpileMainIntegrationTest extends BashpileTest {

    private static final Logger log = LogManager.getLogger(BashpileMainIntegrationTest.class);

    @Test
    @Timeout(10)
    @Order(10)
    public void bpcTranspiles() throws IOException {
        log.info("In bpcTranspiles test");

        final String translatedFilename = "src/test/resources/testrigData";
        final String command = "target/bpc src/test/resources/testrigData.bps";
        try {
            final ExecutionResults results = runAndJoin(command);
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            final List<String> lines = results.stdoutLines();
            final String lastLine = lines.get(lines.size() - 1);
            assertTrue(lastLine.endsWith(translatedFilename),
                    "Expected last line to end with %s but was %s".formatted(translatedFilename, lastLine));
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }

    @Test
    @Timeout(20)
    @Order(11)
    public void bprWorks() throws IOException {
        log.info("In bprWorks");

        // run with our local (not installed) bpr
        final String command = "target/bpr src/test/resources/scripts/bprShebang.bps";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
    }

    @Test
    @Timeout(10)
    @Order(20)
    public void noSubCommandWithNoExtensionTranspiles() throws IOException {
        log.info("In noSubCommandWithNoExtensionTranspiles");

        final String translatedFilename = "src/test/resources/testrigData2";
        final String command = "target/bpc src/test/resources/testrigData2.bps";
        Path translatedPath = Path.of(translatedFilename);
        Files.deleteIfExists(translatedPath);
        final ExecutionResults results = runAndJoin(command);
        try {
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            final List<String> lines = results.stdoutLines();
            assertTrue(lines.get(lines.size() - 1).endsWith(translatedFilename));
        } finally {
            Files.deleteIfExists(translatedPath);
        }
    }

    @Test
    @Timeout(10)
    @Order(30)
    public void noSubCommandWithMissingFileFails() throws IOException {
        log.info("In noSubCommandWithMissingFileFails");

        final String command = "target/bpc src/test/resources/testrigData.fileDoesNotExist";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertFailedExitCode(results);
        assertFalse(Files.exists(Path.of("output.txt")));
    }

    @Test
    @Timeout(30)
    @Order(40)
    public void outputFileFlagWithDoubleRunWorks() throws IOException {
        log.info("In outputFileFlagWithDoubleRunWorks");

        final String translatedFilename = "src/test/resources/testrigData2.example.bps";
        final String command = "target/bpc src/test/resources/testrigData2.bps --outputFile " + translatedFilename;
        ExecutionResults results = runAndJoin(command);
        try {
            log.debug("Output text:\n{}", results.stdout());

            assertSuccessfulExitCode(results);
            List<String> lines = results.stdoutLines();
            assertTrue(lines.get(lines.size() - 1).endsWith(translatedFilename));

            // 2nd run to verify overwrites OK
            results = runAndJoin(command);
            assertSuccessfulExitCode(results);
        } finally {
            Files.deleteIfExists(Path.of(translatedFilename));
        }
    }

    @Test
    @Timeout(20)
    @Order(50)
    public void bprCreateErrorMessagesPropagate() throws IOException {
        log.info("In bprCreateErrorMessagesPropagate");

        final String bashpileFilename = "src/test/resources/scripts/overwriteCheck.bps";
        final Path defaultOutputFile = Path.of("src/test/resources/scripts/overwriteCheck");
        assertTrue(Files.exists(defaultOutputFile), "Default output file does not exist");
        final String command = "target/bpr " + bashpileFilename;
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertFailedExitCode(results);
    }

    @Test
    @Timeout(20)
    @Order(50)
    public void bprDashCWorks() throws IOException {
        log.info("In bpr -c works");

        // run with our local (not installed) bpr
        final String command = "target/bpr -c \"print('Hello World')\"";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        assertEquals("Hello World\n", results.stdout());
        assertFalse(Files.exists(Path.of("command.bps")));
    }

    @Test
    @Timeout(21)
    @Order(60)
    public void bpcDashCWithStdinWorks() throws IOException {
        log.info("In bpc -c with stdin works");

        // run with our local (not installed) bpr
        final String command = "echo \"print('Hello World')\" | target/bpc -c";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        // last line is the created filename
        final String filename = results.stdoutLines().get(results.stdoutLines().size() - 1);
        Files.deleteIfExists(Path.of(filename));
    }

    @Test
    @Timeout(20)
    @Order(70)
    public void bprDashCWithStdinWorks() throws IOException {
        log.info("In bpr -c with stdin works");

        // run with our local (not installed) bpr
        final String command = "echo \"print('Hello World')\" | target/bpr -c";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        assertEquals("Hello World\n", results.stdout());
        assertFalse(Files.exists(Path.of("command.bps")));
    }

    @Test
    @Timeout(20)
    @Order(80)
    public void bprDashCWithStdinFromDifferentDirectoryWorks() throws IOException {
        log.info("In bpr -c with stdin works");
        Assumptions.assumeTrue(Files.exists(Path.of("../homebrew-bashpile")));

        // run with our local (not installed) bpr
        final String command = "cd ..; echo \"print('Hello World')\" | homebrew-bashpile/target/bpr -c";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        assertEquals("Hello World\n", results.stdout());
        assertFalse(Files.exists(Path.of("command.bps")));
        assertFalse(Files.exists(Path.of("../command.bps")));
    }

    @Test
    @Timeout(20)
    @Order(81)
    public void bprDashCWithStdinFromDifferentDirectoryWithOutputFileWorks() throws IOException {
        log.info("In bpr -c with stdin and outputfile works");
        Assumptions.assumeTrue(Files.exists(Path.of("../homebrew-bashpile")));

        // run with our local (not installed) bpr
        final String command =
                "cd ..; echo \"print('Hello World')\" | homebrew-bashpile/target/bpr --outputFile command81 -c";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        assertEquals("Hello World\n", results.stdout());
        assertFalse(Files.exists(Path.of("command81")));
        assertFalse(Files.exists(Path.of("../command81")));
    }

    @Test
    @Timeout(20)
    @Order(81)
    public void bpcDashWithStdinWorks() throws IOException {
        log.info("In bpc - with stdin works");
        Assumptions.assumeTrue(Files.exists(Path.of("../homebrew-bashpile")));

        // run with our local (not installed) bpr
        final String command =
                "cd ..; echo \"print('Hello World')\" | homebrew-bashpile/target/bpc -";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        assertFalse(Files.exists(Path.of("command.bps")));
        assertFalse(Files.exists(Path.of("../command.bps")));
    }

    @Test
    @Timeout(20)
    @Order(82)
    public void bpcDashWithOutputFileWorks() throws IOException {
        log.info("In bpc - with outputFile Works");
        Assumptions.assumeTrue(Files.exists(Path.of("../homebrew-bashpile")));

        // run with our local (not installed) bpr
        final String command =
                "cd ..; echo \"print('Hello World')\" | homebrew-bashpile/target/bpc --outputFile dashOutput.bash -";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        Path dashOutput = Path.of("../dashOutput.bash");
        assertTrue(Files.exists(dashOutput));

        Files.deleteIfExists(dashOutput);
        assertFalse(Files.exists(dashOutput));
    }

    @Test
    @Timeout(20)
    @Order(84)
    public void bprDashWithStdinWorks() throws IOException {
        log.info("In bpr - with stdin works");
        Assumptions.assumeTrue(Files.exists(Path.of("../homebrew-bashpile")));

        // run with our local (not installed) bpr
        final String command =
                "cd ..; echo \"print('Hello World')\" | homebrew-bashpile/target/bpr -";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        assertEquals("Hello World\n", results.stdout());
        assertFalse(Files.exists(Path.of("command.bps")));
    }

    @Test
    @Timeout(20)
    @Order(90)
    public void bprWithNoArgumentsPrintsHelp() throws IOException {
        log.info("In bpr with no arguments prints help");

        // run with our local (not installed) bpr
        final String command = "target/bpr";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertFailedExitCode(results);
        assertTrue(results.stdout().contains("Usage: "));
        assertFalse(Files.exists(Path.of("command.bps")));
    }

    @Test
    @Timeout(20)
    @Order(100)
    public void bprDashCFailsGracefullyOnBadCompile() throws IOException {
        log.info("In bpr -c fails gracefully");

        final String command = "target/bpr -c \"# echo\"";
        final ExecutionResults results = runAndJoin(command);

        assertFailedExitCode(results);
        assertFalse(results.stdout().contains("No such file or directory"));
        // set -e text
        assertFalse(results.stdout().contains("Error (exit code 1) found on line "));
    }
}
