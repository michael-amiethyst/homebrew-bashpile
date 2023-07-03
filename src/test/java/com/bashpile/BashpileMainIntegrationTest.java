package com.bashpile;

import com.bashpile.commandline.BashExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.bashpile.Asserts.assertExecutionSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BashpileMainIntegrationTest {

    private static final Logger log = LogManager.getLogger(BashpileMainIntegrationTest.class);

    @Test
    public void noSubCommandTest() throws IOException {
        log.debug("In noSubCommandTest");
        String command = "bin/bashpile";
        var executionResults = BashExecutor.run(command);
        log.debug("Output text:\n{}", executionResults.stdout());
        assertEquals(1, executionResults.exitCode());
        assertTrue(executionResults.stdoutLines().length > 0,
                "No output for `bashpile` command");
        assertTrue(executionResults.stdoutLines()[0].startsWith("Usage"),
                "Unexpected output for `bashpile` command");
    }

    @Test
    public void executeTest() throws IOException {
        log.debug("In executeTest");
        String command = "bin/bashpile -i=src/test/resources/10-base/0010-simple.bashpile execute";
        var executionResults = BashExecutor.run(command);
        String outputText = executionResults.stdout();
        log.debug("Output text:\n{}", outputText);
        String[] lines = executionResults.stdoutLines();
        assertExecutionSuccess(executionResults);
        assertTrue(lines.length > 0, "No output");
        int lastLineIndex = lines.length - 1;
        assertEquals("2", lines[lastLineIndex], "Unexpected output: %s".formatted(outputText));
    }

    @Test
    public void transpileTest() throws IOException {
        log.debug("In transpileTest");
        String command = "bin/bashpile -i src/test/resources/10-base/0010-simple.bashpile transpile";
        var executionResults = BashExecutor.run(command);
        String outputText = executionResults.stdout();
        log.debug("Output text:\n{}", outputText);
        String[] lines = executionResults.stdoutLines();
        assertExecutionSuccess(executionResults);
        assertTrue(lines.length > 0, "No output");
        int lastLineIndex = lines.length - 1;
        assertEquals("echo \"$__bp_textReturn\";", lines[lastLineIndex],
                "Unexpected output: %s".formatted(outputText));
    }
}
