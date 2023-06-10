package com.bashpile;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BashpileMainIntegrationTest {

    private static final Logger log = LogManager.getLogger(BashpileMainIntegrationTest.class);

    @Test
    public void noSubCommandTest() throws IOException {
        log.debug("In noSubCommandTest");
        String command = "bin/bashpile";
        Pair<String, Integer> executionResults = CommandLineExecutor.failableRunInPlace(command);
        String outputText = executionResults.getLeft();
        log.debug("Output text:\n{}", outputText);
        String[] lines = BashpileMainTest.lines.split(outputText);
        assertEquals(1, executionResults.getRight());
        assertTrue(lines.length > 0, "No output for `bashpile` command");
        assertTrue(lines[0].startsWith("Usage"), "Unexpected output for `bashpile` command");
    }

    @Test
    public void executeTest() throws IOException {
        log.debug("In executeTest");
        String command = "bin/bashpile -i=src/test/resources/0001-simple.bashpile execute";
        Pair<String, Integer> executionResults = CommandLineExecutor.failableRunInPlace(command);
        String outputText = executionResults.getLeft();
        log.debug("Output text:\n{}", outputText);
        String[] lines = BashpileMainTest.lines.split(outputText);
        assertEquals(0, executionResults.getRight());
        assertTrue(lines.length > 0, "No output");
        int lastLineIndex = lines.length - 1;
        assertEquals("2", lines[lastLineIndex], "Unexpected output: %s".formatted(outputText));
    }

    @Test
    public void transpileTest() throws IOException {
        log.debug("In transpileTest");
        String command = "bin/bashpile -i src/test/resources/0001-simple.bashpile transpile";
        Pair<String, Integer> executionResults = CommandLineExecutor.failableRunInPlace(command);
        String outputText = executionResults.getLeft();
        log.debug("Output text:\n{}", outputText);
        String[] lines = BashpileMainTest.lines.split(outputText);
        assertEquals(0, executionResults.getRight());
        assertTrue(lines.length > 0, "No output");
        int lastLineIndex = lines.length - 1;
        assertEquals("bc <<< \"1+1\"", lines[lastLineIndex], "Unexpected output: %s".formatted(outputText));
    }
}
