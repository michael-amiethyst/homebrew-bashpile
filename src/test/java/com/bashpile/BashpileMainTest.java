package com.bashpile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BashpileMainTest {

    @Test
    public void mainTest() throws IOException {
        String[] filename = "src/test/resources/test.bashpile".split(" ");
        String[] ret = BashpileMain.processArgs(filename);
        assertNotNull(ret);
        assertEquals(1, ret.length);
        assertEquals("2", ret[0]);
    }
}
