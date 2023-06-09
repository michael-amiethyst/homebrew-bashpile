package com.bashpile;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellTest {

    @Test
    void runTest() throws IOException {
        assertEquals("hello world", CommandLine.run("echo hello world"));
    }

    @Test
    void runWithLessThanTest() throws IOException {
        assertEquals("<<<", CommandLine.run("echo \"<<<\""));
    }

    @Test
    void runWithFullCalcTest() throws IOException {
        assertEquals("2", CommandLine.run("bc <<< \"(1+1)\""));
    }

    @Test
    void runWithEchoETest() throws IOException {
//        String ret = Shell.run("bash --version");
        String ret = CommandLine.run("echo -e");
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    void runWithSetETest() throws IOException {
//        String ret = Shell.run("bash --version");
        String ret = CommandLine.run("set -e");
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }
}