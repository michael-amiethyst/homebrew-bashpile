package com.bashpile;

import com.bashpile.exceptions.UserError;
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

// TODO make floats test (like int test)
@Order(30)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatementBashpileMainTest extends BashpileMainTest {

    @Nonnull
    protected String getDirectoryName() {
        return "30-statement";
    }

    @Test
    @Order(30)
    public void assignTest() {
        List<String> ret = runFile("0030-assign.bashpile").stdoutLines();
        assertEquals("4", ret.get(0));
    }

    @Test
    @Order(31)
    public void reassignTest() {
        List<String> ret = runFile("0031-reassign.bashpile").stdoutLines();
        assertEquals("5", ret.get(0));
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
    @Order(61)
    public void boolTest() {
        List<String> outLines = runFile("0061-bool.bashpile").stdoutLines();
        assertEquals("false", outLines.get(0));
    }

    @Test
    @Order(70)
    public void intTest() {
        List<String> bashLines = runFile("0070-int.bashpile").stdoutLines();
        assertEquals("42", getLast(bashLines));
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
    @Order(72)
    public void stringConcatTest() {
        var runResult = runFile("0072-stringConcat.bashpile");
        assertExecutionSuccess(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("hello world", getLast(outLines));
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
        List<String> executionResults = runFile(filename).stdoutLines();
        List<String> expected = List.of("24", "64000", "128");
        assertEquals(3, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test
    @Order(90)
    public void lexicalScopingWorks() {
        assertThrows(UserError.class, () -> runFile("0090-lexicalScoping.bashpile"));
    }

    @Test
    @Order(110)
    public void commentsWork() {
        List<String> executionResults = runFile("0110-comments.bashpile").stdoutLines();
        List<String> expected = List.of("21.0", "11.0", "7.0");
        assertEquals(3, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test @Order(120)
    public void blockCommentsWork() {
        ExecutionResults executionResults = runFile("0120-blockComments.bashpile");
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
        List<String> expected = List.of("21.0", "11.0", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }
}
