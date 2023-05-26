package com.bashpile;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class BashpileMainTest {

    @Test
    public void mainTest() throws IOException {
        BashpileMain.main("src/test/resources/test.bashpile".split(" "));
    }
}