package com.bashpile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.regex.Pattern;

import static com.bashpile.engine.BashTranslationEngine.TAB;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BashpileMainTest {

    public static final Pattern lines = Pattern.compile("\r?\n");

    private static final Logger log = LogManager.getLogger(BashpileMainTest.class);

    @Test
    @Order(10)
    public void simpleTest() {
        String[] ret = runFile("0010-simple.bashpile");
        assertNotNull(ret);
        final int expectedLines = 1;
        assertEquals(expectedLines, ret.length, "Unexpected output length, expected %d lines but found: %s"
                .formatted(expectedLines, String.join("\n", ret)));
        assertEquals("2", ret[0], "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    @Order(20)
    public void multilineTest() {
        String[] ret = runFile("0020-multiline.bashpile");
        assertNotNull(ret);
        int expected = 2;
        assertEquals(expected, ret.length, "Expected %d lines but got %d".formatted(expected, ret.length));
        assertEquals("2", ret[0]);
        assertEquals("0", ret[1]);
    }

    @Test
    @Order(30)
    public void assignTest() {
        String[] ret = runFile("0030-assign.bashpile");
        assertEquals("4", ret[0]);
    }

    /**
     * References an undeclared variable.
     */
    @Test
    @Order(40)
    public void badAssign() {
        assertThrows(BashpileUncheckedException.class, () -> runFile("0040-badAssign.bashpile"));
    }

    @Test
    @Order(50)
    public void parenTest() {
        String[] ret = runFile("0050-paren.bashpile");
        assertEquals(1, ret.length, "Unexpected number of lines");
        assertEquals("4", ret[0]);
    }

    @Test
    @Order(60)
    public void idTest() throws IOException {
        String[] bashLines = transpileFile("0060-id.bashpile");
        assertEquals("var", bashLines[bashLines.length - 1]);
    }

    @Test
    @Order(70)
    public void intTest() throws IOException {
        String[] bashLines = transpileFile("0070-int.bashpile");
        assertEquals("42", bashLines[bashLines.length - 1]);
    }

    @Test
    @Order(80)
    public void blockTest() throws IOException {
        String filename = "0080-block.bashpile";
        String[] bashLines = transpileFile(filename);
        assertTrue(bashLines[6].contains("64+64"), "Wrong line");
        assertTrue(bashLines[6].startsWith("        "), "Wrong indention");
        String[] executionResults = runFile(filename);
        String[] expected = {"18", "64000", "128"};
        assertEquals(3, executionResults.length);
        assertArrayEquals(expected, executionResults);
    }

    @Test
    @Order(90)
    public void lexicalScopingTest() throws IOException {
        String filename = "0090-lexicalscoping.bashpile";
        String[] bashLines = transpileFile(filename);
        assertEquals(10, bashLines.length);
        assertTrue(bashLines[4].startsWith(TAB + "local"), "No local decl, line 5");
        assertTrue(bashLines[6].startsWith(TAB + TAB + "local"), "No local decl, line 6");
        assertThrows(BashpileUncheckedException.class, () -> runFile(filename));
    }

    @Test
    @Order(100)
    public void floatsTest() {
        String[] executionResults = runFile("0100-floats.bashpile");
        String[] expected = {"40.0", "11.0", "7.0"};
        assertEquals(3, executionResults.length);
        assertArrayEquals(expected, executionResults);
    }

    @Test
    @Order(110)
    public void functionDeclarationTest() {
        String[] executionResults = runFile("0110-functionDeclaration.bashpile");
        assertEquals(2, executionResults.length);
    }

    @Test
    @Order(120)
    public void functionCallTest() {
        String[] executionResults = runFile("0120-functionCall.bashpile");
        assertEquals(2, executionResults.length);
        assertEquals("3.14", executionResults[0]);
        assertEquals("3.14", executionResults[1]);
    }

    // helpers

    private String[] transpileFile(String file) throws IOException {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/%s".formatted(file);
        BashpileMain bashpile = new BashpileMain(filename);
        return lines.split(bashpile.transpile());
    }

    private String[] runFile(String file) {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/%s".formatted(file);
        BashpileMain bashpile = new BashpileMain(filename);
        return lines.split(bashpile.execute());
    }
}
