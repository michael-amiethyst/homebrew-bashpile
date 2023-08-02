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


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BashpileMainIntegrationTest extends BashpileTest {

    private static final Logger log = LogManager.getLogger(BashpileMainIntegrationTest.class);

    @Test @Order(10)
    public void noSubCommandTranspiles() throws IOException {
        log.debug("In noSubCommandTest");

        String command = "bin/bashpile src/test/resources/testrigData.bps";
        var results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        var lastLines = lines.subList(lines.size() - 3 , lines.size());
        assertEquals("testrigData", lastLines.get(0));
        assertEquals("Start of testrigData", lastLines.get(1));
        assertEquals("test", lastLines.get(2));
    }
}
