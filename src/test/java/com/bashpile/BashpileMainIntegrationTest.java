package com.bashpile;

import com.bashpile.maintests.BashpileTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.List;

import static com.bashpile.shell.BashShell.runAndJoin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BashpileMainIntegrationTest extends BashpileTest {

    private static final Logger log = LogManager.getLogger(BashpileMainIntegrationTest.class);

    @Test @Order(10)
    public void noSubCommandTest() throws IOException {
        log.debug("In noSubCommandTest");
        String command = "bin/bashpile src/test/resources/testrigData.bashpile";
        var results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());
        assertEquals(0, results.exitCode());
        assertTrue(results.stdoutLines().size() > 0,
                "No output for `bashpile` command");
        assertTrue(results.stdoutLines().get(0).contains(" testrigData"),
                "Unexpected output for `bashpile` command: " + results.stdout());
    }

    @Test @Order(20)
    public void transpileCommandTest() throws IOException {
        log.debug("In transpileTest");
        String command = "bin/bashpile -c \"print()\" transpile";
        var executionResults = runAndJoin(command);
        String outputText = executionResults.stdout();
        log.debug("Output text:\n{}", outputText);
        List<String> lines = executionResults.stdoutLines();
        assertSuccessfulExitCode(executionResults);
        assertTrue(lines.size() > 0, "No output");
        int lastLineIndex = lines.size() - 1;
        assertEquals("printf \"\\n\"", lines.get(lastLineIndex),
                "Unexpected output: %s".formatted(outputText));
    }
}
