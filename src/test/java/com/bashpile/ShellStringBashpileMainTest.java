package com.bashpile;

import com.bashpile.commandline.ExecutionResults;
import com.bashpile.testhelper.BashpileMainTest;
import org.junit.jupiter.api.*;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO test parenthesis -- $(echo ())
// TODO test nested -- see https://github.com/sepp2k/antlr4-string-interpolation-examples
// TODO implement '.runOrThrow()'
// TODO implement redirects
@Order(50)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShellStringBashpileMainTest extends BashpileMainTest {
    @Nonnull
    @Override
    protected String getDirectoryName() {
        return "50-commandObjects";
    }

    /** Simple one word command */
    @Test @Order(10)
    public void runLsWorks() {
        final ExecutionResults results = runFile("0010-runLs.bashpile");
        assertTrue(results.stdout().contains("pom.xml\n"));
    }

    /** Command with arguments */
    @Test @Order(20)
    public void runEchoWorks() {
        final ExecutionResults results = runFile("0020-runEcho.bashpile");
        assertEquals("hello command object\n", results.stdout());
    }

    @Test @Order(30)
    public void runInvalidCommandHadBadExitCode() {
        final ExecutionResults results = runFile("0030-runInvalidCommand.bashpile");
        assertTrue(results.exitCode() != ExecutionResults.SUCCESS);
    }
}
