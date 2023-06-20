package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.junit.jupiter.api.Test;

import static com.bashpile.Asserts.assertTextBlock;
import static com.bashpile.Asserts.assertTextLine;
import static org.junit.jupiter.api.Assertions.*;

class AssertsTest {

    @Test
    void assertTextBlockCanPass() {
        final String test = """
                something
                with a
                trailing
                newline
                """;
        assertTextBlock(test);
    }

    @Test
    void assertTextBlockCanFail() {
        final String test = """
                something
                without a
                trailing
                newline""";
        assertThrows(BashpileUncheckedException.class, () -> assertTextBlock(test));
    }

    @Test
    void assertTextLineCanPass() {
        final String test = "one line with newline\n";
        assertTextLine(test);
    }

    @Test
    void assertTextLineCanFail() {
        final String test = "no newline";
        assertThrows(BashpileUncheckedException.class, () -> assertTextLine(test));
    }

    @Test
    void assertTextLineCanFailTwoLines() {
        final String test = "line one\nline two\n";
        assertThrows(BashpileUncheckedException.class, () -> assertTextLine(test));
    }

    @Test
    void assertMatches() {
    }
}