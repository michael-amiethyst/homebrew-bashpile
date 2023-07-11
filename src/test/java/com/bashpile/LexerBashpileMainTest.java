package com.bashpile;

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

// TODO move paren tests down, add 6f syntax

/**
 * Technically "print()" is a statement but we need it to get any output at all.
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
        List<String> ret = runFile("0010-print.bashpile").stdoutLines();
        assertNotNull(ret);
        final int expectedLines = 1;
        assertEquals(expectedLines, ret.size(), "Unexpected output length, expected %d lines but found: %s"
                .formatted(expectedLines, String.join("\n", ret)));
        assertEquals("", ret.get(0), "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    @Order(20)
    public void multilineTest() {
        List<String> translatedLines = runFile("0020-multiline.bashpile").stdinLines();
        assertNotNull(translatedLines);
        final int endIndex = translatedLines.size() - 1;
        assertEquals("echo", translatedLines.get(endIndex - 1));
        assertEquals("echo", translatedLines.get(endIndex));
    }

    @Test
    @Order(50)
    public void parenTest() {
        List<String> ret = runFile("0050-paren.bashpile").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("21", ret.get(0));
    }

    @Test
    @Order(51)
    public void parenStringTest() {
        List<String> ret = runFile("0051-parenString.bashpile").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("hello", ret.get(0));
    }

    @Test
    @Order(61)
    public void boolTest() {
        List<String> outLines = runFile("0061-bool.bashpile").stdoutLines();
        assertEquals("false", outLines.get(0));
    }

    @Test
    @Order(70)
    public void intTest() {
        List<String> bashLines = runFile("0070-int.bashpile").stdinLines();
        assertEquals("echo 42", getLast(bashLines));
    }

    @Test
    @Order(71)
    public void stringTest() {
        var runResult = runFile("0071-string.bashpile");
        assertExecutionSuccess(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("world", getLast(outLines));
    }

    @Test
    @Order(100)
    public void floatsTest() {
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
        ExecutionResults executionResults = runFile("0130-bashpileDocs.bashpile");
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of(".5", "1.7");
        assertEquals(2, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(130)
    public void bashpileDocsWork() {
        ExecutionResults executionResults = runFile("0120-commentBlocks.bashpile");
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "11.0", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }
}
