package com.bashpile;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandLineExecutorTest {

    @Test
    void runTest() throws IOException {
        assertEquals("hello world", CommandLineExecutor.run("echo hello world").getLeft());
    }

    @Test
    void runWithLessThanTest() throws IOException {
        assertEquals("<<<", CommandLineExecutor.run("echo \"<<<\"").getLeft());
    }

    @Test
    void runWithFullCalcTest() throws IOException {
        assertEquals("2", CommandLineExecutor.run("bc <<< \"(1+1)\"").getLeft());
    }

    @Test
    void runWithEchoETest() throws IOException {
//        String ret = Shell.run("bash --version");
        String ret = CommandLineExecutor.run("echo -e").getLeft();
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    void runWithSetETest() throws IOException {
//        String ret = Shell.run("bash --version");
        String ret = CommandLineExecutor.run("set -e").getLeft();
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }
}