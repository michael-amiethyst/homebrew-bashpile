package com.bashpile;

import com.bashpile.exceptions.UserError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static com.bashpile.Asserts.assertExecutionSuccess;
import static org.junit.jupiter.api.Assertions.*;

@Order(10)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BashpileMainTest {

    private static final Logger log = LogManager.getLogger(BashpileMainTest.class);

    @Test
    @Order(10)
    public void printCalcWorks() {
        String[] ret = runFile("0010-printCalc.bashpile").stdoutLines();
        assertNotNull(ret);
        final int expectedLines = 1;
        assertEquals(expectedLines, ret.length, "Unexpected output length, expected %d lines but found: %s"
                .formatted(expectedLines, String.join("\n", ret)));
        assertEquals("2", ret[0], "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    @Order(20)
    public void multilineTest() {
        String[] ret = runFile("0020-multiline.bashpile").stdoutLines();
        assertNotNull(ret);
        int expected = 2;
        assertEquals(expected, ret.length, "Expected %d lines but got %d".formatted(expected, ret.length));
        assertEquals("2", ret[0]);
        assertEquals("0", ret[1]);
    }

    @Test
    @Order(30)
    public void assignTest() {
        String[] ret = runFile("0030-assign.bashpile").stdoutLines();
        assertEquals("4", ret[0]);
    }

    @Test
    @Order(31)
    public void reassignTest() {
        String[] ret = runFile("0031-reassign.bashpile").stdoutLines();
        assertEquals("5", ret[0]);
    }

    /**
     * References an undeclared variable.
     */
    @Test
    @Order(40)
    public void unassignedVariableReferenceCausesError() {
        assertThrows(UserError.class, () -> runFile("0040-unassigned.bashpile"));
    }

    @Test
    @Order(41)
    public void declaredTwiceCausesError() {
        assertThrows(UserError.class, () -> runFile("0041-declaredTwice.bashpile"));
    }

    @Test
    @Order(50)
    public void parenTest() {
        String[] ret = runFile("0050-paren.bashpile").stdoutLines();
        assertEquals(1, ret.length, "Unexpected number of lines");
        assertEquals("21", ret[0]);
    }

    @Test
    @Order(51)
    public void parenStringTest() {
        String[] ret = runFile("0051-parenString.bashpile").stdoutLines();
        assertEquals(1, ret.length, "Unexpected number of lines");
        assertEquals("hello world, you good?", ret[0]);
    }

    @Test
    @Order(60)
    public void idTest() {
        String[] outLines = runFile("0060-id.bashpile").stdoutLines();
        assertEquals("6", outLines[0]);
    }

    @Test
    @Order(61)
    public void boolTest() {
        String[] outLines = runFile("0061-bool.bashpile").stdoutLines();
        assertEquals("false", outLines[0]);
    }

    @Test
    @Order(70)
    public void intTest() {
        String[] bashLines = runFile("0070-int.bashpile").stdinLines();
        assertEquals("42", bashLines[bashLines.length - 1]);
    }

    @Test
    @Order(71)
    public void stringTest() {
        var runResult = runFile("0071-string.bashpile");
        assertExecutionSuccess(runResult);
        String[] outLines = runResult.stdoutLines();
        assertEquals("world", outLines[outLines.length - 1]);
    }

    @Test
    @Order(72)
    public void stringConcatTest() {
        var runResult = runFile("0072-stringConcat.bashpile");
        assertExecutionSuccess(runResult);
        String[] outLines = runResult.stdoutLines();
        assertEquals("hello world", outLines[outLines.length - 1]);
    }

    @Test
    @Order(73)
    public void stringBadOperatorTest() {
        assertThrows(AssertionError.class, () -> runFile("0073-stringBadOperator.bashpile"));
    }

    @Test
    @Order(80)
    public void blockTest() {
        String filename = "0080-block.bashpile";
        String[] executionResults = runFile(filename).stdoutLines();
        String[] expected = {"24", "64000", "128"};
        assertEquals(3, executionResults.length);
        assertArrayEquals(expected, executionResults);
    }

    @Test
    @Order(90)
    public void lexicalScopingWorks() {
        assertThrows(UserError.class, () -> runFile("0090-lexicalScoping.bashpile"));
    }

    @Test
    @Order(100)
    public void floatsTest() {
        String[] executionResults = runFile("0100-floats.bashpile").stdoutLines();
        String[] expected = {"21.0", "11.0", "7.0"};
        assertEquals(3, executionResults.length);
        assertArrayEquals(expected, executionResults);
    }

    @Test
    @Order(110)
    public void commentsTest() {
        String[] executionResults = runFile("0110-comments.bashpile").stdoutLines();
        String[] expected = {"21.0", "11.0", "7.0"};
        assertEquals(3, executionResults.length);
        assertArrayEquals(expected, executionResults);
    }

    // helpers

    private ExecutionResults runFile(String file) {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/10-base/%s".formatted(file);
        BashpileMain bashpile = new BashpileMain(filename);
        return bashpile.execute();
    }
}
