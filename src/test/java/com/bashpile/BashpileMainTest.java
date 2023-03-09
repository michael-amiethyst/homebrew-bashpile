package com.bashpile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BashpileMainTest {

    private static final Logger log = LogManager.getLogger(BashpileMainTest.class);

    @Test
    @Order(1)
    public void simpleTest() throws IOException {
        String[] ret = runFile("0001-simple.bashpile");
        assertNotNull(ret);
        assertEquals(1, ret.length);
        assertEquals("2", ret[0]);
    }

    @Test
    @Order(2)
    public void multilineTest() throws IOException {
        String[] ret = runFile("0002-multiline.bashpile");
        assertNotNull(ret);
        assertEquals(2, ret.length);
        assertEquals("2", ret[0]);
        assertEquals("0", ret[1]);
    }

    @Test
    @Order(3)
    public void assign() throws IOException {
        String[] ret = runFile("src/test/resources/assign.bashpile");
        assertEquals(2, ret[0]);
        assertEquals(4, ret[1]);
    }

    @Test
    @Order(4)
    public void badAssign() {
        assertThrows(RuntimeException.class, () -> runFile("src/test/resources/badAssign.bashpile"));
    }

    private String[] runFile(String file) throws IOException {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/%s".formatted(file);
        return BashpileMain.processArg(filename);

    }
}
