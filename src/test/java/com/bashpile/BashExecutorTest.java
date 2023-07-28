package com.bashpile;

import com.bashpile.commandline.BashExecutor;
import com.bashpile.commandline.ExecutionResults;
import com.bashpile.maintests.BashpileTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BashExecutorTest extends BashpileTest {

    @Test @Order(10)
    void runTest() throws IOException {
        ExecutionResults executionResults = BashExecutor.run("echo hello world");
        assertSuccessfulExitCode(executionResults);
        assertEquals("hello world\n", executionResults.stdout());
    }

    @Test @Order(20)
    void runWithLessThanTest() throws IOException {
        ExecutionResults executionResults = BashExecutor.run("echo \"<<<\"");
        assertSuccessfulExitCode(executionResults);
        assertEquals("<<<\n", executionResults.stdout());
    }

    @Test @Order(30)
    void runWithFullCalcTest() throws IOException {
        ExecutionResults executionResults = BashExecutor.run("bc <<< \"(1+1)\"");
        assertSuccessfulExitCode(executionResults);
        assertEquals("2\n", executionResults.stdout());
    }

    @Test @Order(40)
    void runWithEchoETest() throws IOException {
        ExecutionResults executionResults = BashExecutor.run("echo -e");
        assertSuccessfulExitCode(executionResults);
        String ret = executionResults.stdout();
        assertEquals("\n", ret, "Unexpected output: " + ret);
    }

    @Test @Order(50)
    void runWithSetETest() throws IOException {
        ExecutionResults executionResults = BashExecutor.run("set -e");
        assertSuccessfulExitCode(executionResults);
        String ret = executionResults.stdout();
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }
}