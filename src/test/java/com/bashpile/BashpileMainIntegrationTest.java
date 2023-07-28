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
        String command = "bin/bashpile";
        var executionResults = runAndJoin(command);
        log.debug("Output text:\n{}", executionResults.stdout());
        assertEquals(1, executionResults.exitCode());
        assertTrue(executionResults.stdoutLines().size() > 0,
                "No output for `bashpile` command");
        assertTrue(executionResults.stdoutLines().get(0).startsWith("Usage"),
                "Unexpected output for `bashpile` command");
    }

    @Test @Order(20)
    public void executeTest() throws IOException {
        log.debug("In executeTest");
        String command = "bin/bashpile -c=\"print()\" execute";
        var executionResults = runAndJoin(command);
        String outputText = executionResults.stdout();
        log.debug("Output text:\n{}", outputText);
        assertTrue(outputText.endsWith("\n\n"));
    }

    @Test @Order(30)
    public void executePathTest() throws IOException {
        log.debug("In executeTest");
        String command = "bin/bashpile -i=src/test/resources/scripts/escapedString.bashpile execute";
        var executionResults = runAndJoin(command);
        String outputText = executionResults.stdout();
        log.debug("Output text:\n{}", outputText);
        List<String> stdoutLines = executionResults.stdoutLines();
        assertEquals("\"hello\"", ListUtils.getLast(stdoutLines));
    }

    @Test @Order(40)
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
        assertEquals("echo", lines.get(lastLineIndex),
                "Unexpected output: %s".formatted(outputText));
    }
}
