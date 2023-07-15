package com.bashpile;

import com.bashpile.commandline.ExecutionResults;
import com.bashpile.testhelper.BashpileMainTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.annotation.Nonnull;
import java.util.List;

import static com.bashpile.Asserts.assertExecutionSuccess;
import static com.bashpile.ListUtils.getLast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// TODO inline file contents
/**
 * Technically "print()" is a statement, but we need it to get any output at all.
 */
@Order(10)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LexerBashpileMainTest extends BashpileMainTest {

    @Nonnull
    protected String getDirectoryName() {
        return "10-lexer";
    }

    @Test
    @Order(10)
    public void printWorks() {
        String ret = runFile("0010-print.bashpile").stdout();
        assertNotNull(ret);
        assertEquals("\n", ret);
    }

    @Test
    @Order(20)
    public void multilinePrintWorks() {
        String translatedLines = runFile("0020-multiline.bashpile").stdout();
        assertNotNull(translatedLines);
        assertEquals("\n\n", translatedLines);
    }

    @Test
    @Order(30)
    public void boolWorks() {
        List<String> outLines = runFile("0030-bool.bashpile").stdoutLines();
        assertEquals("false", outLines.get(0));
    }

    @Test
    @Order(40)
    public void intWorks() {
        List<String> bashLines = runFile("0040-int.bashpile").stdinLines();
        assertEquals("echo 42", getLast(bashLines));
    }

    @Test
    @Order(50)
    public void parenIntWorks() {
        List<String> ret = runFile("0050-parenInt.bashpile").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("21", ret.get(0));
    }

    @Test
    @Order(60)
    public void stringWorks() {
        var runResult = runFile("0060-string.bashpile");
        assertExecutionSuccess(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("world", getLast(outLines));
    }

    @Test
    @Order(70)
    public void parenStringWorks() {
        List<String> ret = runFile("0070-parenString.bashpile").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("hello", ret.get(0));
    }

    @Test
    @Order(80)
    public void escapedStringWorks() {
        List<String> ret = runFile("0080-escapedString.bashpile").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("\"hello\"", ret.get(0));
    }

    @Test
    @Order(100)
    public void floatsWork() {
        List<String> executionResults = runFile("0100-floats.bashpile").stdoutLines();
        List<String> expected = List.of(".5", "0.7");
        assertEquals(2, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test
    @Order(110)
    public void commentsWork() {
        List<String> executionResults = runFile("0110-comments.bashpile").stdoutLines();
        List<String> expected = List.of(".5", "1.7");
        assertEquals(2, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test @Order(120)
    public void blockCommentsWork() {
        ExecutionResults executionResults = runFile("0120-commentBlocks.bashpile");
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "11.0", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(130)
    public void bashpileDocsWork() {
        ExecutionResults executionResults = runFile("0130-bashpileDocs.bashpile");
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of(".5", "1.7");
        assertEquals(2, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }
}
