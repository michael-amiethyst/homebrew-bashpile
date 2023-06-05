package com.bashpile;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class ShellTest {

    @Test
    void runTest() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        assertEquals("hello world", Shell.run("echo hello world"));
    }
    @Test
    void runWithLessThanTest() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        assertEquals("<<<", Shell.run("echo \"<<<\""));
    }
    @Test
    void runWithFullCalcTest() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        assertEquals("2", Shell.run("bc <<< \"(1+1)\""));
    }
}