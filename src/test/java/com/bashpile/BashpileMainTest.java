package com.bashpile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BashpileMainTest {

    private static final Logger log = LogManager.getLogger(BashpileMainTest.class);

    @Test
    @Order(1)
    public void simpleTest() throws IOException {
        String[] ret = runFile("0001-simple.bashpile");
        assertNotNull(ret);
        final int expectedLines = 1;
        assertEquals(expectedLines, ret.length, "Unexpected output length, expected %d lines but found: %s"
                .formatted(expectedLines, String.join("\n", ret)));
        assertEquals("2", ret[0], "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    @Order(2)
    public void multilineTest() throws IOException {
        String[] ret = runFile("0002-multiline.bashpile");
        assertNotNull(ret);
        int expected = 2;
        assertEquals(expected, ret.length, "Expected %d lines but got %d".formatted(expected, ret.length));
        assertEquals("2", ret[0]);
        assertEquals("0", ret[1]);
    }

    @Test
    @Order(3)
    public void assignTest() throws IOException {
        String[] ret = runFile("003-assign.bashpile");
        assertEquals("4", ret[0]);
    }

    /**
     * References an undeclared variable.
     */
    @Test
    @Order(4)
    public void badAssign() {
        assertThrows(BashpileUncheckedException.class, () -> runFile("004-badAssign.bashpile"));
    }

    @Test
    @Order(5)
    public void parenTest() throws IOException {
        String[] ret = runFile("005-paren.bashpile");
        assertEquals(1, ret.length, "Unexpected number of lines");
        assertEquals("4", ret[0]);
    }

    @Test
    @Order(6)
    public void idTest() {
        // outputs "var", a bad command, leading to an exception for a bad return value
        // TODO get just the bash translation and assert
        // TODO make exception for code 127 -- command not found
        assertThrows(BashpileUncheckedException.class, () -> runFile("006-id.bashpile"));
    }

    @Test
    @Order(7)
    public void intTest() {
        assertThrows(BashpileUncheckedException.class, () -> runFile("007-int.bashpile"));
    }

    // helpers

    private String[] runFile(String file) throws IOException {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/%s".formatted(file);
        BashpileMain bashpile = new BashpileMain(filename);
        return bashpile.execute().split("\r?\n");
    }
}
