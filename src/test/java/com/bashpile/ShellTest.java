package com.bashpile;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellTest {

    @Test
    void runTest() throws IOException {
        assertEquals("hello world", CommandLineExecutor.run("echo hello world"));
    }

    @Test
    void runWithLessThanTest() throws IOException {
        assertEquals("<<<", CommandLineExecutor.run("echo \"<<<\""));
    }

    @Test
    void runWithFullCalcTest() throws IOException {
        assertEquals("2", CommandLineExecutor.run("bc <<< \"(1+1)\""));
    }

    @Test
    void runWithEchoETest() throws IOException {
//        String ret = Shell.run("bash --version");
        String ret = CommandLineExecutor.run("echo -e");
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    void runWithSetETest() throws IOException {
//        String ret = Shell.run("bash --version");
        String ret = CommandLineExecutor.run("set -e");
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }
}