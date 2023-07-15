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
        var ret = runText("print(1 + 1)");
        assertNotNull(ret);
        assertEquals("2\n", ret.stdout());
    }

    @Test
    @Order(20)
    public void multilineCalcWorks() {
        List<String> ret = runText("""
                print(1 + 1)
                print(1-1)""").stdoutLines();
        assertNotNull(ret);
        int expected = 2;
        assertEquals(expected, ret.size(), "Expected %d lines but got %d".formatted(expected, ret.size()));
        assertEquals("2", ret.get(0));
        assertEquals("0", ret.get(1));
    }

    @Test
    @Order(30)
    public void stringConcatWorks() {
        var runResult = runText("""
                print("hello" + " " + "world")""");
        assertExecutionSuccess(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("hello world", getLast(outLines));
    }

    @Test
    @Order(40)
    public void stringBadOperatorThrows() {
        assertThrows(AssertionError.class, () -> runText("""
                print("hello " * "world")"""));
    }

    @Test
    @Order(50)
    public void parenStringWorks() {
        List<String> ret = runText("""
                print((("hello" + " world") + (", you" + " good?")))""").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("hello world, you good?", ret.get(0));
    }

    @Test
    @Order(80)
    public void intExpressionsWork() {
        String bashpile = """
                print((3 + 5) * 3)
                print(32000 + 32000)
                print(64 + 64)""";
        List<String> executionResults = runText(bashpile).stdoutLines();
        List<String> expected = List.of("24", "64000", "128");
        assertEquals(3, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test
    @Order(90)
    public void parenIntExpressionsWork() {
        List<String> ret = runText("print(((1 + 2) * (3 + 4)))").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("21", ret.get(0));
    }

    @Test
    @Order(100)
    public void floatExpressionsWork() {
        List<String> executionResults = runText("""
                print((38. + 4) * .5)
                print(7.7 - 0.7)""").stdoutLines();
        List<String> expected = List.of("21.0", "7.0");
        assertEquals(2, executionResults.size());
        assertEquals(expected, executionResults);
    }
}
