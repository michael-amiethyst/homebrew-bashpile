package com.bashpile;

import org.apache.commons.lang3.tuple.Pair;
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
        String[] ret = runFile("0010-simple.bashpile").getLeft();
        assertNotNull(ret);
        final int expectedLines = 1;
        assertEquals(expectedLines, ret.length, "Unexpected output length, expected %d lines but found: %s"
                .formatted(expectedLines, String.join("\n", ret)));
        assertEquals("2", ret[0], "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    @Order(20)
    public void multilineTest() {
        String[] ret = runFile("0020-multiline.bashpile").getLeft();
        assertNotNull(ret);
        int expected = 2;
        assertEquals(expected, ret.length, "Expected %d lines but got %d".formatted(expected, ret.length));
        assertEquals("2", ret[0]);
        assertEquals("0", ret[1]);
    }

    @Test
    @Order(30)
    public void assignTest() {
        String[] ret = runFile("0030-assign.bashpile").getLeft();
        assertEquals("4", ret[0]);
    }

    /**
     * References an undeclared variable.
     */
    @Test
    @Order(40)
    public void badAssign() {
        Pair<String[], Integer> runResults = runFile("0040-badAssign.bashpile");
        assertEquals(1, runResults.getRight());
        String errorLine = runResults.getLeft()[0];
        assertTrue(errorLine.endsWith("unbound variable"), "Unexpected error line: " + errorLine);
    }

    @Test
    @Order(50)
    public void parenTest() {
        String[] ret = runFile("0050-paren.bashpile").getLeft();
        assertEquals(1, ret.length, "Unexpected number of lines");
        assertEquals("21", ret[0]);
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
    @Order(71)
    public void stringTest() {
        var runResult = runFile("0071-string.bashpile");
        assertEquals(0, runResult.getRight());
        String[] outLines = runResult.getLeft();
        assertEquals("world", outLines[outLines.length - 1]);
    }

    @Test
    @Order(80)
    public void blockTest() throws IOException {
        String filename = "0080-block.bashpile";
        String[] bashLines = transpileFile(filename);
        assertTrue(bashLines[6].contains("64+64"), "Wrong line");
        assertTrue(bashLines[6].startsWith("        "), "Wrong indention");
        String[] executionResults = runFile(filename).getLeft();
        String[] expected = {"24", "64000", "128"};
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
        var ret = runFile(filename);
        assertEquals(0, ret.getRight(), "Unexpected exit code");
        String line = ret.getLeft()[ret.getLeft().length - 1];
        assertTrue(line.endsWith("unbound variable"), "Unexpected error line: " + line);
    }

    @Test
    @Order(100)
    public void floatsTest() {
        String[] executionResults = runFile("0100-floats.bashpile").getLeft();
        String[] expected = {"21.0", "11.0", "7.0"};
        assertEquals(3, executionResults.length);
        assertArrayEquals(expected, executionResults);
    }

    @Test
    @Order(110)
    public void functionDeclarationTest() {
        String[] executionResults = runFile("0110-functionDeclaration.bashpile").getLeft();
        assertEquals(2, executionResults.length);
    }

    @Test
    @Order(120)
    public void functionCallTest() {
        String[] executionResults = runFile("0120-functionCall.bashpile").getLeft();
        assertEquals(2, executionResults.length);
        assertEquals("3.14", executionResults[0]);
        assertEquals("3.14", executionResults[1]);
    }

    @Test
    @Order(121)
    public void functionCallMultipleParamsTest() {
        Pair<String[], Integer> executionResults = runFile("0121-functionCall-multipleParams.bashpile");
        assertEquals(0, executionResults.getRight());
        assertEquals(1, executionResults.getLeft().length);
        assertEquals("12", executionResults.getLeft()[0]);
    }

    @Test
    @Order(122)
    public void functionCallReturnStringTest() {
        Pair<String[], Integer> executionResults = runFile("0122-functionCall-returnString.bashpile");
        assertEquals(0, executionResults.getRight());
        assertEquals(1, executionResults.getLeft().length);
        assertEquals("hello world", executionResults.getLeft()[0]);
    }

    // helpers

    private String[] transpileFile(String file) throws IOException {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/%s".formatted(file);
        BashpileMain bashpile = new BashpileMain(filename);
        return lines.split(bashpile.transpile());
    }

    private Pair<String[], Integer> runFile(String file) {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/%s".formatted(file);
        BashpileMain bashpile = new BashpileMain(filename);
        Pair<String, Integer> runResults = bashpile.execute();
        return Pair.of(lines.split(runResults.getLeft()), runResults.getRight());
    }
}
