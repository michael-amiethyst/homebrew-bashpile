package com.bashpile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BashpileMainIntegrationTest {

    @Test
    public void failTest() throws IOException {
        // TODO `runHere` that doesn't cd
        String command = "cd %s; bin/bashpile".formatted(System.getProperty("user.dir"));
        // TODO `failableRun` that returns text and return code
        assertThrows(BashpileUncheckedException.class, () -> CommandLineExecutor.run(command));
//        String outputText = CommandLineExecutor.run(command);
//        String[] lines = BashpileMainTest.lines.split(outputText);
//        assertTrue(lines.length > 0, "No output for `bashpile` command");
//        assertTrue(lines[0].startsWith("Usage"), "Unexpected output for `bashpile` command");
    }
}
