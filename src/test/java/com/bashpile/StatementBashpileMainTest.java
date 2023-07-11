package com.bashpile;

import com.bashpile.commandline.ExecutionResults;
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

@Order(30)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatementBashpileMainTest extends BashpileMainTest {

    @Nonnull
    protected String getDirectoryName() {
        return "30-statements";
    }

    @Test
    @Order(10)
    public void assignBoolWorks() {
        List<String> outLines = runFile("0010-assignBool.bashpile").stdoutLines();
        assertEquals("false", outLines.get(0));
    }

    @Test
    @Order(20)
    public void assignIntWorks() {
        List<String> outLines = runFile("0020-assignInt.bashpile").stdoutLines();
        assertEquals("42", getLast(outLines));
    }

    @Test
    @Order(30)
    public void assignIntExpressionWorks() {
        List<String> ret = runFile("0030-assignIntExpression.bashpile").stdoutLines();
        assertEquals("4", ret.get(0));
    }

    /**
     * References an undeclared variable.
     */
    @Test
    @Order(40)
    public void duplicateIntAssignmentThrows() {
        assertThrows(UserError.class, () -> runFile("0040-duplicateIntAssignment.bashpile"));
    }

    @Test
    @Order(50)
    public void unassignedIntExpressionThrows() {
        assertThrows(UserError.class, () -> runFile("0050-unassignedIntExpression.bashpile"));
    }

    @Test
    @Order(60)
    public void reassignIntExpressionWorks() {
        List<String> ret = runFile("0060-reassignIntExpression.bashpile").stdoutLines();
        assertEquals("5", ret.get(0));
    }

    @Test
    @Order(70)
    public void floatWorks() {
        List<String> outLines = runFile("0070-float.bashpile").stdoutLines();
        assertEquals("4000000.999", getLast(outLines));
    }

    @Test
    @Order(80)
    public void stringWorks() {
        var runResult = runFile("0080-string.bashpile");
        assertExecutionSuccess(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("world", getLast(outLines));
    }

    @Test
    @Order(81)
    public void stringConcatWorks() {
        var runResult = runFile("0081-stringConcat.bashpile");
        assertExecutionSuccess(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("hello world", getLast(outLines));
    }

    @Test
    @Order(82)
    public void stringBadOperatorWorks() {
        assertThrows(AssertionError.class, () -> runFile("0082-stringBadOperator.bashpile"));
    }

    @Test
    @Order(90)
    public void blockWorks() {
        String filename = "0090-block.bashpile";
        List<String> executionResults = runFile(filename).stdoutLines();
        List<String> expected = List.of("24", "64000", "128");
        assertEquals(3, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test
    @Order(100)
    public void lexicalScopingWorks() {
        assertThrows(UserError.class, () -> runFile("0100-lexicalScoping.bashpile"));
    }

    @Test
    @Order(110)
    public void commentsWork() {
        ExecutionResults executionResults = runFile("0110-comments.bashpile");
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "16.5", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
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
        List<String> expected = List.of("21.0", "0", "7.0", "To boldly go");
        assertEquals(4, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }
}
