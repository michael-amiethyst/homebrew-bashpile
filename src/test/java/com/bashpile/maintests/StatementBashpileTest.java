package com.bashpile.maintests;

import com.bashpile.shell.BashShell;
import com.bashpile.shell.ExecutionResults;
import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.bashpile.ListUtils.getLast;
import static org.junit.jupiter.api.Assertions.*;

@Order(30)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatementBashpileTest extends BashpileTest {

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
    @Order(41)
    public void assignFloatToIntThrows() {
        assertThrows(TypeError.class, () -> runText("""
                someVar: int = 2.2
                print(someVar + 2)"""));
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
        assertSuccessfulExitCode(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("world", getLast(outLines));
    }

    @Test
    @Order(81)
    public void stringConcatWorks() {
        var runResult = runText("""
                worldStr:str="world"
                print("hello " + worldStr)""");
        assertSuccessfulExitCode(runResult);
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
        List<String> executionResults = runText("""
                print((3 + 5) * 3)
                block:
                    print(32000 + 32000)
                    block:
                        print(64 + 64)""").stdoutLines();
        List<String> expected = List.of("24", "64000", "128");
        assertEquals(3, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test
    @Order(100)
    public void lexicalScopingWorks() {
        assertThrows(UserError.class, () -> runText("""
                print((38 + 5) * 3)
                block:
                    x: int = 5
                    block:
                        y: int = 7
                print(x * x)"""));
    }

    @Test
    @Order(110)
    public void commentsWork() {
        ExecutionResults executionResults = runText("""
                print((38. + 4) * .5)
                // anonymous block
                block:
                    x: float = 5.5 // lexical scoping
                    print(x * 3)
                x: float = 7.7
                print(x - 0.7)""");
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "16.5", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(120)
    public void blockCommentsWork() {
        ExecutionResults executionResults = runText("""
                print((38. + 4) * .5)
                /* anonymous block */
                block:
                    x: float = /* inside of a statement comment */ 5.5
                    /*
                     * extended comment on how fantastic this line is
                     */
                    print(x + x)
                x: float = 7.7
                print(x - 0.7)""");
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "11.0", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(130)
    public void bashpileDocsWork() {
        ExecutionResults executionResults = runText("""
                /**
                    This language is
                    really starting to shape up.
                    It will replace Bash.
                */
                                
                print((38. + 4) * .5)
                myString : str
                // anonymous block
                block:
                    /**
                     * More docs on this inner block
                     */
                    block:
                        x: str = "To boldly"
                        y: str = " go"
                        myString = x + y
                    x: float = 5.5 // lexical scoping
                    print(x - x)
                x: float = 7.7
                print(x - 0.7)
                print(myString)
                """);
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "0", "7.0", "To boldly go");
        assertEquals(4, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test
    @Order(140)
    public void createStatementsWork() {
        final ExecutionResults executionResults = runText("""
                #(rm -f captainsLog.txt || true)
                contents: str
                #(echo "Captain's log, stardate..." > captainsLog.txt) creates "captainsLog.txt":
                    contents = $(cat captainsLog.txt)
                print(contents)""");
        assertSuccessfulExitCode(executionResults);
        assertEquals("Captain's log, stardate...\n", executionResults.stdout());
        assertFalse(Files.exists(Path.of("captainsLog.txt")), "file not deleted");
    }

    @Test
    @Order(150)
    public void createStatementsCanFail() throws IOException {
        try {
            final ExecutionResults executionResults = runText("""
                    #(touch captainsLog.txt || true)
                    contents: str
                    #(echo "Captain's log, stardate..." > captainsLog.txt) creates "captainsLog.txt":
                        contents = $(cat captainsLog.txt)
                    print(contents)""");
            assertFailedExitCode(executionResults);
        } finally {
            Files.deleteIfExists(Path.of("captainsLog.txt"));
        }
    }

    @Test
    @Order(160)
    public void createStatementTrapsWorks() throws IOException {
        final String bashpileScript = """
                #(rm -f captainsLog.txt || true)
                contents: str
                #(echo "Captain's log, stardate..." > captainsLog.txt) creates "captainsLog.txt":
                    #(sleep 1)
                    contents = $(cat captainsLog.txt)
                print(contents)""";
        try(final BashShell shell = runTextAsync(bashpileScript)) {
            shell.sendTerminationSignal();
            final ExecutionResults executionResults = shell.join();
            assertFailedExitCode(executionResults);
            assertEquals("", executionResults.stdout());
            assertFalse(Files.exists(Path.of("captainsLog.txt")), "file not deleted");
        }
    }

    @Test
    @Order(170)
    public void createStatementsWithNestedInlinesWork() {
        final ExecutionResults executionResults = runText("""
                #(rm -f captainsLog.txt || true)
                contents: str
                #(echo $(echo $(echo "Captain's log, stardate...")) > captainsLog.txt) creates "captainsLog.txt":
                    contents = $(cat $(echo captainsLog.txt))
                print(contents)""");
        assertSuccessfulExitCode(executionResults);
        assertEquals("Captain's log, stardate...\n", executionResults.stdout());
        assertFalse(Files.exists(Path.of("captainsLog.txt")), "file not deleted");
    }

    @Test
    @Order(180)
    public void nestedCreateStatementsWithNestedInlinesWork() {
        // TODO undo 'unnested'/preambles for shell strings and command substitutions
        // TODO use subshells for nested create statements (for the traps)
        final ExecutionResults executionResults = runText("""
                #(rm -f captainsLog.txt || true)
                #(rm -f captainsLog2.txt || true)
                contents: str
                contents2: str
                #(echo $(echo $(echo "Captain's log, stardate...")) > captainsLog.txt) creates "captainsLog.txt":
                    contents = $(cat $(echo captainsLog.txt))
                    #(echo $(echo $(echo "Captain's log, stardate...")) > captainsLog2.txt) creates "captainsLog2.txt":
                        contents2 = $(cat $(echo captainsLog2.txt))
                print(contents)
                print(contents2)""");
        assertSuccessfulExitCode(executionResults);
        assertEquals("Captain's log, stardate...\nCaptain's log, stardate...\n", executionResults.stdout());
        assertFalse(Files.exists(Path.of("captainsLog.txt")), "file not deleted");
        assertFalse(Files.exists(Path.of("captainsLog2.txt")), "file2 not deleted");
        // TODO uncomment
//        assertFalse(executionResults.stdinLines().stream().anyMatch(str -> str.matches("^[^#]+#.*$")));
    }
}
