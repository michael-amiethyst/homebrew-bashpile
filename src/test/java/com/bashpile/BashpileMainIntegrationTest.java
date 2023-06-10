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
        String command = "bin/bashpile";
        Pair<String, Integer> executionResults = CommandLineExecutor.failableRunInPlace(command);
        String outputText = executionResults.getLeft();
        log.debug("Output text:\n{}", outputText);
        String[] lines = BashpileMainTest.lines.split(outputText);
        assertEquals(1, executionResults.getRight());
        assertTrue(lines.length > 0, "No output for `bashpile` command");
        assertTrue(lines[0].startsWith("Usage"), "Unexpected output for `bashpile` command");
    }
}
