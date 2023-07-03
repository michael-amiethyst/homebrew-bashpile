package com.bashpile;

import com.bashpile.commandline.BashExecutor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BashExecutorTest {

    @Test
    void runTest() throws IOException {
        ExecutionResults executionResults = BashExecutor.run("echo hello world");
        Asserts.assertExecutionSuccess(executionResults);
        assertEquals("hello world", executionResults.stdout());
    }

    @Test
    void runWithLessThanTest() throws IOException {
        ExecutionResults executionResults = BashExecutor.run("echo \"<<<\"");
        Asserts.assertExecutionSuccess(executionResults);
        assertEquals("<<<", executionResults.stdout());
    }

    @Test
    void runWithFullCalcTest() throws IOException {
        ExecutionResults executionResults = BashExecutor.run("bc <<< \"(1+1)\"");
        Asserts.assertExecutionSuccess(executionResults);
        assertEquals("2", executionResults.stdout());
    }

    @Test
    void runWithEchoETest() throws IOException {
        ExecutionResults executionResults = BashExecutor.run("echo -e");
        Asserts.assertExecutionSuccess(executionResults);
        String ret = executionResults.stdout();
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    void runWithSetETest() throws IOException {
        ExecutionResults executionResults = BashExecutor.run("set -e");
        Asserts.assertExecutionSuccess(executionResults);
        String ret = executionResults.stdout();
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }
}