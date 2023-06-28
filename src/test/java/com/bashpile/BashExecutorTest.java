package com.bashpile;

import com.bashpile.commandline.BashExecutor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BashExecutorTest {

    @Test
    void runTest() throws IOException {
        assertEquals("hello world", BashExecutor.run("echo hello world").getLeft());
    }

    @Test
    void runWithLessThanTest() throws IOException {
        assertEquals("<<<", BashExecutor.run("echo \"<<<\"").getLeft());
    }

    @Test
    void runWithFullCalcTest() throws IOException {
        assertEquals("2", BashExecutor.run("bc <<< \"(1+1)\"").getLeft());
    }

    @Test
    void runWithEchoETest() throws IOException {
//        String ret = Shell.run("bash --version");
        String ret = BashExecutor.run("echo -e").getLeft();
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    void runWithSetETest() throws IOException {
//        String ret = Shell.run("bash --version");
        String ret = BashExecutor.run("set -e").getLeft();
        assertEquals("", ret, "Unexpected output: %s".formatted(String.join("\n", ret)));
    }
}