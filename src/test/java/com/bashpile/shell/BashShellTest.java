package com.bashpile.shell;

import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.maintests.BashpileTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;

import static com.bashpile.shell.BashShell.runAndJoin;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(5)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BashShellTest extends BashpileTest {

    @Test @Order(10)
    void echoRunsSuccessfully() throws IOException {
        final ExecutionResults executionResults = runAndJoin("echo hello world");
        assertSuccessfulExitCode(executionResults);
        assertEquals("hello world\n", executionResults.stdout());
    }

    @Test @Order(20)
    void runWithLessThanRunsSuccessfully() throws IOException {
        final ExecutionResults executionResults = runAndJoin("echo \"<<<\"");
        assertSuccessfulExitCode(executionResults);
        assertEquals("<<<\n", executionResults.stdout());
    }

    @Test @Order(30)
    void runWithFullCalcRunsSuccessfully() throws IOException {
        final ExecutionResults executionResults = runAndJoin("bc <<< \"(1+1)\"");
        if (executionResults.exitCode() == ExecutionResults.COMMAND_NOT_FOUND) {
            throw new BashpileUncheckedException(
                    "Could not find 'bc' in PATH:\n" + runAndJoin("echo $PATH").stdout());
        }
        assertSuccessfulExitCode(executionResults);
        assertEquals("2\n", executionResults.stdout());
    }

    @Test @Order(40)
    void runWithEchoERunsSuccessfully() throws IOException {
        final ExecutionResults executionResults = runAndJoin("echo -e");
        assertSuccessfulExitCode(executionResults);
        String ret = executionResults.stdout();
        assertEquals("\n", ret, "Unexpected output: " + ret);
    }

    @Test @Order(50)
    void runWithSetERunsSuccessfully() throws IOException {
        final ExecutionResults executionResults = runAndJoin("set -e");
        assertSuccessfulExitCode(executionResults);
        String ret = executionResults.stdout();
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test @Order(60)
    void shellcheckRunsSuccessfully() throws IOException {
        final ExecutionResults executionResults = runAndJoin("shellcheck --help");
        assertSuccessfulExitCode(executionResults);
    }
}