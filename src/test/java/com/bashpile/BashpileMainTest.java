package com.bashpile;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BashpileMainTest {

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

    private String[] runFile(String file) throws IOException {
        System.out.printf("Start of %s%n", file);
        String filename = "src/test/resources/%s".formatted(file);
        return BashpileMain.processArg(filename);

    }
}
