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
import static org.junit.jupiter.api.Assertions.*;

// TODO test very large numbers (e.g. MAX - 1)
// TODO test overly large numbers (e.g. MAX + 1)
@Order(20)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExpressionBashpileMainTest extends BashpileMainTest {

    @Nonnull
    protected String getDirectoryName() {
        return "20-expressions";
    }

    @Test
    @Order(10)
    public void printCalcWorks() {
        List<String> ret = runFile("0010-printCalc.bashpile").stdoutLines();
        assertNotNull(ret);
        final int expectedLines = 1;
        assertEquals(expectedLines, ret.size(), "Unexpected output length, expected %d lines but found: %s"
                .formatted(expectedLines, String.join("\n", ret)));
        assertEquals("2", ret.get(0), "Unexpected output: %s".formatted(String.join("\n", ret)));
    }

    @Test
    @Order(20)
    public void multilineCalcWorks() {
        List<String> ret = runFile("0020-multilinePrintCalc.bashpile").stdoutLines();
        assertNotNull(ret);
        int expected = 2;
        assertEquals(expected, ret.size(), "Expected %d lines but got %d".formatted(expected, ret.size()));
        assertEquals("2", ret.get(0));
        assertEquals("0", ret.get(1));
    }

    @Test
    @Order(30)
    public void stringConcatWorks() {
        var runResult = runFile("0030-stringConcat.bashpile");
        assertExecutionSuccess(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("hello world", getLast(outLines));
    }

    @Test
    @Order(40)
    public void stringBadOperatorThrows() {
        assertThrows(AssertionError.class, () -> runFile("0040-stringBadOperator.bashpile"));
    }

    @Test
    @Order(50)
    public void parenStringWorks() {
        List<String> ret = runFile("0050-parenString.bashpile").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("hello world, you good?", ret.get(0));
    }

    @Test
    @Order(80)
    public void intExpressionsWork() {
        String filename = "0080-intExpressions.bashpile";
        List<String> executionResults = runFile(filename).stdoutLines();
        List<String> expected = List.of("24", "64000", "128");
        assertEquals(3, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test
    @Order(90)
    public void parenIntExpressionsWork() {
        List<String> ret = runFile("0090-parenIntExpressions.bashpile").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("21", ret.get(0));
    }

    @Test
    @Order(100)
    public void floatExpressionsWork() {
        List<String> executionResults = runFile("0100-floatExpressions.bashpile").stdoutLines();
        List<String> expected = List.of("21.0", "7.0");
        assertEquals(2, executionResults.size());
        assertEquals(expected, executionResults);
    }
}
