package com.bashpile.maintests;

import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Order(30)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatementBashpileTest extends BashpileTest {

    /** Add a stub 'bashpile-stdlib' file for testing the import statement */
    @BeforeAll
    public static void setup() {
        final ExecutionResults results = runText("touch ./target/bashpile-stdlib");
        assertSuccessfulExitCode(results);
    }

    /** Remove the stub for a real implementation to be generated later */
    @AfterAll
    public static void cleanup() {
        final ExecutionResults results = runText("rm ./target/bashpile-stdlib");
        assertSuccessfulExitCode(results);
    }

    // tests

    @Test
    @Order(10)
    public void assignBoolWorks() {
        final ExecutionResults results = runText("""
                var: bool = false
                print(var)""");
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(20)
    public void assignIntWorks() {
        final ExecutionResults results = runText("""
                var: int = 42
                print(var)""");
        assertSuccessfulExitCode(results);
        assertEquals("42\n", results.stdout());
    }

    @Test
    @Order(30)
    public void assignIntExpressionWorks() {
        final ExecutionResults results = runText("""
                someVar: int = 1 + 1
                print(someVar + 2)""");
        assertSuccessfulExitCode(results);
        assertEquals("4\n", results.stdout());
    }

    @Test
    @Order(31)
    public void assignReadonlyIntExpressionWorks() {
        final ExecutionResults results = runText("""
                someVar: readonly int = 1 + 1
                print(someVar + 2)""");
        assertTrue(results.stdin().contains("declare"));
        assertSuccessfulExitCode(results);
        assertEquals("4\n", results.stdout());
    }

    @Test
    @Order(32)
    public void assignExportedIntExpressionWorks() {
        final ExecutionResults results = runText("""
                someVar: exported int = 1 + 1
                print(someVar + 2)""");
        assertTrue(results.stdin().contains("declare -x someVar") || results.stdin().contains("declare -x  someVar"));
        assertSuccessfulExitCode(results);
        assertEquals("4\n", results.stdout());
    }

    @Test
    @Order(33)
    public void assignReadonlyExportedIntExpressionWorks() {
        final ExecutionResults results = runText("""
                someVar: readonly exported int = 1 + 1
                print(someVar + 2)""");
        assertTrue(results.stdin().contains("declare -x someVar") || results.stdin().contains("declare -x  someVar"));
        assertSuccessfulExitCode(results);
        assertEquals("4\n", results.stdout());
    }

    @Test
    @Order(34)
    public void assignExportedReadonlyIntExpressionWorks() {
        final ExecutionResults results = runText("""
                exportedFinal: exported readonly int = 1 + 1
                print(exportedFinal + 2)""");
        assertTrue(results.stdin().contains("declare -x exportedFinal") || results.stdin().contains("declare -x  exportedFinal"));
        assertSuccessfulExitCode(results);
        assertEquals("4\n", results.stdout());
    }

    @Test()
    @Order(35)
    public void assignReadonlyReadonlyExportedIntExpressionThrows() {
        Assertions.assertThrows(BashpileUncheckedException.class, () -> runText("""
                someVar: readonly readonly exported int = 1 + 1
                print(someVar + 2)"""));
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
        final ExecutionResults results = runText("""
                someVar: int = 1 + 1
                someVar = 3
                print(someVar + 2)""");
        assertSuccessfulExitCode(results);
        assertEquals("5\n", results.stdout());
    }

    @Test
    @Order(61)
    public void reassignPrimaryExpressionWorks() {
        final ExecutionResults results = runText("""
                someVar: bool = true
                someVar = "b" <= "a"
                print(someVar)""");
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(62)
    public void reassignIntWithIncrementWorks() {
        final ExecutionResults results = runText("""
                someVar: int = 0
                print(someVar++)
                print(someVar)""");
        assertSuccessfulExitCode(results);
        assertEquals("0\n1\n", results.stdout());
    }

    @Test
    @Order(63)
    public void reassignFloatWithIncrementFailsWithTypeError() {
        assertThrows(TypeError.class, () -> runText("""
                someVar: float = 0.1
                print(someVar++)
                print(someVar)"""));
    }

    @Test
    @Order(63)
    public void reassignFloatWithCastAndIncrementWorks() {
        final ExecutionResults results = runText("""
                someVar: float = 0.0
                print((someVar: int)++)
                print(someVar)""");
        assertSuccessfulExitCode(results);
        assertEquals("0\n1\n", results.stdout());
        assertEquals(results.stdout().indexOf("%d"), results.stdout().lastIndexOf("%d"), "Too many setups");
    }

    @Test
    @Order(64)
    public void reassignNumberWithCastAndIncrementWorks() {
        final ExecutionResults results = runText("""
                someVar: number = 0.0
                print((someVar: int)++)
                print(someVar)""");
        assertSuccessfulExitCode(results);
        assertEquals("0\n1\n", results.stdout());
    }

    @Test
    @Order(65)
    public void reassignFloatWithCastAndDecrementWorks() {
        final ExecutionResults results = runText("""
                someVar: float = 0.0
                print((someVar: int)--)
                print(someVar)""");
        assertSuccessfulExitCode(results);
        assertEquals("0\n-1\n", results.stdout());
    }

    @Test
    @Order(66)
    public void reassignStringWithFloatWithCastAndDecrementWorks() {
        final ExecutionResults results = runText("""
                someVar: str = "0.0"
                print((someVar: int)--)
                print(someVar)""");
        assertSuccessfulExitCode(results);
        assertEquals("0\n-1\n", results.stdout());
    }

    @Test
    @Order(67)
    public void decrementBoolThrows() {
        assertThrows(TypeError.class, () -> runText("""
                BOOL: bool = false
                BOOL--"""));
    }

    @Test
    @Order(68)
    public void whileWithIncrementWorks() {
        final ExecutionResults results = runText("""
                i: int = 0
                while i < 3:
                    print(i++)""");
        assertSuccessfulExitCode(results);
        assertEquals("0\n1\n2\n", results.stdout());
    }

    @Test
    @Order(69)
    public void whileWithDecrementWorks() {
        final ExecutionResults results = runText("""
                i: int = 3
                while i > 0:
                    print(i--)""");
        assertSuccessfulExitCode(results);
        assertEquals("3\n2\n1\n", results.stdout());
    }

    @Test
    @Order(70)
    public void floatWorks() {
        final ExecutionResults results = runText("""
                var: float = 4000000.999
                print(var)""");
        assertSuccessfulExitCode(results);
        assertEquals("4000000.999\n", results.stdout());
    }

    @Test
    @Order(80)
    public void stringWorks() {
        final ExecutionResults results = runText("""
                world:str="world"
                print(world)""");
        assertSuccessfulExitCode(results);
        assertEquals("world\n", results.stdout());
    }

    @Test
    @Order(81)
    public void stringConcatWorks() {
        final ExecutionResults results = runText("""
                world:str="world"
                print("hello " + world)""");
        assertSuccessfulExitCode(results);
        assertEquals("hello world\n", results.stdout());
    }

    @Test
    @Order(82)
    public void stringConcatInAssignWorks() {
        final ExecutionResults results = runText("""
                world:str="hello " + "world"
                print(world)""");
        assertSuccessfulExitCode(results);
        assertEquals("hello world\n", results.stdout());
    }

    // TODO write test for reassign and string concat, include arguments[]

    @Test
    @Order(82)
    public void stringBadOperatorFails() {
        assertThrows(BashpileUncheckedException.class, () -> runText("""
                worldStr:str="world"
                print("hello " * worldStr)"""));
    }

    @Test
    @Order(90)
    public void blockWorks() {
        final ExecutionResults results = runText("""
                print((3 + 5) * 3)
                block:
                    print(32000 + 32000)
                    block:
                        print(64 + 64)""");
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        final List<String> expected = List.of("24", "64000", "128");
        assertEquals(3, lines.size());
        assertEquals(expected, lines);
    }

    @Test
    @Order(91)
    public void blockWithNestedInlinesWork() {
        final ExecutionResults results = runText("""
                print((3 + 5) * 3)
                block:
                    print(#(echo "$(echo "Captain's log, stardate...")"))
                    block:
                        print(64 + 64)""");
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        final List<String> expected = List.of("24", "Captain's log, stardate...", "128");
        assertEquals(3, lines.size());
        assertEquals(expected, lines);
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
        final ExecutionResults results = runText("""
                print((38. + 4) * .5)
                // anonymous block
                block:
                    x: float = 5.5 // lexical scoping
                    print(x * 3)
                x: float = 7.7
                print(x - 0.7)""");
        assertSuccessfulExitCode(results);
        final List<String> stdoutLines = results.stdoutLines();
        final List<String> expected = List.of("21.0", "16.5", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(results.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test
    @Order(120)
    public void blockCommentsWork() {
        final ExecutionResults results = runText("""
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
        assertSuccessfulExitCode(results);
        final List<String> stdoutLines = results.stdoutLines();
        final List<String> expected = List.of("21.0", "11.0", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(results.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test
    @Order(130)
    public void bashpileDocsWork() {
        final ExecutionResults results = runText("""
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
        assertSuccessfulExitCode(results);
        final List<String> stdoutLines = results.stdoutLines();
        final List<String> expected = List.of("21.0", "0", "7.0", "To boldly go");
        assertEquals(4, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(results.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test
    @Order(140)
    public void simpleSwitchWorks() {
        ExecutionResults results = runText("""
                i: int = 1
                switch i:
                    case -1:
                        print('Neg')
                    case 0:
                        print('Zero')
                    case 1:
                        print('Positive')
                """);
        assertSuccessfulExitCode(results);
        assertEquals("Positive\n", results.stdout());
    }

    @Test
    @Order(150)
    public void orSwitchWorks() {
        ExecutionResults results = runText("""
                i: int = 2
                switch i:
                    case -1:
                        print('Neg')
                    case 0:
                        print('Zero')
                    case 1 or 2:
                        print('Positive')
                """);
        assertSuccessfulExitCode(results);
        assertEquals("Positive\n", results.stdout());
    }

    @Test
    @Order(160)
    public void catchAllSwitchWorks() {
        ExecutionResults results = runText("""
                i: int = 3
                switch i:
                    case -1:
                        print('Neg')
                    case 0:
                        print('Zero')
                    case 1 or 2:
                        print('Positive')
                    case "*":
                        print('Other')
                """);
        assertSuccessfulExitCode(results);
        assertEquals("Other\n", results.stdout());
    }

    @Test
    @Order(170)
    public void importWorks() {
        ExecutionResults results = runText("import 'bashpile-stdlib'");
        assertSuccessfulExitCode(results);
    }
}
