package com.bashpile;

import com.bashpile.maintests.BashpileTest;
import com.bashpile.shell.ExecutionResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static com.bashpile.shell.BashShell.runAndJoin;
import static org.junit.jupiter.api.Assertions.assertEquals;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BashpileMainIntegrationTest extends BashpileTest {

    private static final Logger log = LogManager.getLogger(BashpileMainIntegrationTest.class);

    @Test @Timeout(8) @Order(10)
    public void noSubCommandTranspiles() throws IOException {
        log.debug("In noSubCommandTest");

        final String command = "bin/bashpile src/test/resources/testrigData.bps";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        final List<String> lastLines = lines.subList(lines.size() - 3 , lines.size());
        assertEquals("testrigData", lastLines.get(0));
        assertEquals("Start of testrigData", lastLines.get(1));
        assertEquals("test", lastLines.get(2));
    }

    @Test @Timeout(5) @Order(20)
    public void noSubCommandWithNoExtensionTranspiles() throws IOException {
        log.debug("In noSubCommandTest");

        final String command = "bin/bashpile src/test/resources/testrigData";
        final ExecutionResults results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        final List<String> lastLines = lines.subList(lines.size() - 3 , lines.size());
        assertEquals("testrigData.bpt", lastLines.get(0));
        assertEquals("Start of testrigData.bpt", lastLines.get(1));
        assertEquals("test", lastLines.get(2));
    }
}
