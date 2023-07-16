package com.bashpile;

import com.bashpile.commandline.ExecutionResults;
import com.bashpile.exceptions.UserError;
import com.bashpile.testhelper.BashpileMainTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.annotation.Nonnull;
import java.nio.file.Path;
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
        List<String> outLines = runText("""
                var: bool = false
                print(var)""").stdoutLines();
        assertEquals("false", outLines.get(0));
    }

    @Test
    @Order(20)
    public void assignIntWorks() {
        List<String> outLines = runText("""
                var: int = 42
                print(var)""").stdoutLines();
        assertEquals("42", getLast(outLines));
    }

    @Test
    @Order(30)
    public void assignIntExpressionWorks() {
        List<String> ret = runText("""
                someVar: int = 1 + 1
                print(someVar + 2)""").stdoutLines();
        assertEquals("4", ret.get(0));
    }

    /**
     * References an undeclared variable.
     */
    @Test
    @Order(40)
    public void duplicateIntAssignmentThrows() {
        assertThrows(UserError.class, () -> runText("""
                someVar: int = 1 + 1
                someVar: str = "2"
                """));
    }

    @Test
    @Order(50)
    public void unassignedIntExpressionThrows() {
        assertThrows(UserError.class, () -> runText("""
                someVar: int = 1 + 1
                someOtherVar + 2"""));
    }

    @Test
    @Order(60)
    public void reassignIntExpressionWorks() {
        List<String> ret = runText("""
                someVar: int = 1 + 1
                someVar = 3
                print(someVar + 2)""").stdoutLines();
        assertEquals("5", ret.get(0));
    }

    @Test
    @Order(70)
    public void floatWorks() {
        List<String> outLines = runText("""
                var: float = 4000000.999
                print(var)""").stdoutLines();
        assertEquals("4000000.999", getLast(outLines));
    }

    @Test
    @Order(80)
    public void stringWorks() {
        var runResult = runText("""
                world:str="world"
                print(world)""");
        assertExecutionSuccess(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("world", getLast(outLines));
    }

    @Test
    @Order(81)
    public void stringConcatWorks() {
        var runResult = runText("""
                worldStr:str="world"
                print("hello " + worldStr)""");
        assertExecutionSuccess(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("hello world", getLast(outLines));
    }

    @Test
    @Order(82)
    public void stringBadOperatorWorks() {
        assertThrows(AssertionError.class, () -> runText("""
                worldStr:str="world"
                print("hello " * worldStr)"""));
    }

    @Test
    @Order(90)
    public void blockWorks() {
        String filename = "0090-block.bashpile";
        List<String> executionResults = runPath(Path.of(filename)).stdoutLines();
        List<String> expected = List.of("24", "64000", "128");
        assertEquals(3, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test
    @Order(100)
    public void lexicalScopingWorks() {
        assertThrows(UserError.class, () -> runPath(Path.of("0100-lexicalScoping.bashpile")));
    }

    @Test
    @Order(110)
    public void commentsWork() {
        ExecutionResults executionResults = runPath(Path.of("0110-comments.bashpile"));
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "16.5", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(120)
    public void blockCommentsWork() {
        ExecutionResults executionResults = runPath(Path.of("0120-blockComments.bashpile"));
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "11.0", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(130)
    public void bashpileDocsWork() {
        ExecutionResults executionResults = runPath(Path.of("0130-bashpileDocs.bashpile"));
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "0", "7.0", "To boldly go");
        assertEquals(4, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }
}
