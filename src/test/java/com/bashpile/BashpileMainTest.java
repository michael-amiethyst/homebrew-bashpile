package com.bashpile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BashpileMainTest {

    private static final Logger log = LogManager.getLogger(BashpileMainTest.class);

    @Test
    @Order(10)
    public void simpleTest() {
        String[] ret = runFile("0010-simple.bashpile").stdoutLines();
        assertNotNull(ret);
        final int expectedLines = 1;
        assertEquals(expectedLines, ret.length, "Unexpected output length, expected %d lines but found: %s"
                .formatted(expectedLines, String.join("\n", ret)));
        assertEquals("2", ret[0], "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    @Order(20)
    public void multilineTest() {
        String[] ret = runFile("0020-multiline.bashpile").stdoutLines();
        assertNotNull(ret);
        int expected = 2;
        assertEquals(expected, ret.length, "Expected %d lines but got %d".formatted(expected, ret.length));
        assertEquals("2", ret[0]);
        assertEquals("0", ret[1]);
    }

    @Test
    @Order(30)
    public void assignTest() {
        String[] ret = runFile("0030-assign.bashpile").stdoutLines();
        assertEquals("4", ret[0]);
    }

    @Test
    @Order(31)
    public void reassignTest() {
        String[] ret = runFile("0031-reassign.bashpile").stdoutLines();
        assertEquals("5", ret[0]);
    }

    /**
     * References an undeclared variable.
     */
    @Test
    @Order(40)
    public void badAssign() {
        ExecutionResults runResults = runFile("0040-badAssign.bashpile");
        assertEquals(1, runResults.exitCode());
        String errorLine = runResults.stdoutLines()[0];
        assertTrue(errorLine.endsWith("unbound variable"), "Unexpected error line: " + errorLine);
    }

    @Test
    @Order(50)
    public void parenTest() {
        String[] ret = runFile("0050-paren.bashpile").stdoutLines();
        assertEquals(1, ret.length, "Unexpected number of lines");
        assertEquals("21", ret[0]);
    }

    @Test
    @Order(60)
    public void idTest() {
        String[] bashLines = runFile("0060-id.bashpile").stdinLines();
        assertEquals("var", bashLines[bashLines.length - 1]);
    }

    @Test
    @Order(70)
    public void intTest() {
        String[] bashLines = runFile("0070-int.bashpile").stdinLines();
        assertEquals("42", bashLines[bashLines.length - 1]);
    }

    @Test
    @Order(71)
    public void stringTest() {
        var runResult = runFile("0071-string.bashpile");
        assertEquals(0, runResult.exitCode());
        String[] outLines = runResult.stdoutLines();
        assertEquals("world", outLines[outLines.length - 1]);
    }

    @Test
    @Order(80)
    public void blockTest() {
        String filename = "0080-block.bashpile";
        String[] executionResults = runFile(filename).stdoutLines();
        String[] expected = {"24", "64000", "128"};
        assertEquals(3, executionResults.length);
        assertArrayEquals(expected, executionResults);
    }

    @Test
    @Order(90)
    public void lexicalScopingTest() {
        String filename = "0090-lexicalscoping.bashpile";
        var ret = runFile(filename);
        assertEquals(1, ret.exitCode(),
                "Unexpected exit code.  Return text was:\n" + String.join("\n", ret.stdoutLines()));
        String line = ret.stdoutLines()[ret.stdoutLines().length - 1];
        assertTrue(line.endsWith("unbound variable"), "Unexpected error line: " + line);
    }

    @Test
    @Order(100)
    public void floatsTest() {
        String[] executionResults = runFile("0100-floats.bashpile").stdoutLines();
        String[] expected = {"21.0", "11.0", "7.0"};
        assertEquals(3, executionResults.length);
        assertArrayEquals(expected, executionResults);
    }

    // helpers

    private ExecutionResults runFile(String file) {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/%s".formatted(file);
        BashpileMain bashpile = new BashpileMain(filename);
        return bashpile.execute();
    }
}
