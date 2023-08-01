package com.bashpile;

import com.bashpile.maintests.BashpileTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.bashpile.shell.BashShell.runAndJoin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BashpileMainIntegrationTest extends BashpileTest {

    private static final Logger log = LogManager.getLogger(BashpileMainIntegrationTest.class);

    @Test @Order(10)
    public void noSubCommandTranspiles() throws IOException {
        log.debug("In noSubCommandTest");

        // transpile
        String command = "bin/bashpile src/test/resources/testrigData.bashpile";
        var results = runAndJoin(command);
        log.debug("Output text:\n{}", results.stdout());

        // verify transpile worked
        assertSuccessfulExitCode(results);
        assertTrue(results.stdoutLines().size() > 0,
                "No output for `bashpile` command");
        assertTrue(results.stdoutLines().get(0).contains(" testrigData"),
                "Unexpected output for `bashpile` command: " + results.stdout());
        final Path bashScript = Path.of("testRigData");
        assertTrue(Files.exists(bashScript));

        // verify generated script works
        var bashResults = runAndJoin("./" + bashScript);
        assertSuccessfulExitCode(bashResults);
        assertEquals("test\n", bashResults.stdout());

        // cleanup
        Files.deleteIfExists(bashScript);
    }
}
