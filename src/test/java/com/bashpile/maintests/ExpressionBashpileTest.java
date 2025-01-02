package com.bashpile.maintests;

import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.TypeError;
import com.bashpile.shell.ExecutionResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Order(20)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExpressionBashpileTest extends BashpileTest {

    private static final Logger LOG = LogManager.getLogger(ExpressionBashpileTest.class);

    @Test
    @Order(10)
    public void printCalcWithIntsWorks() {
        final ExecutionResults results = runText("print(1 + 1)");
        assertSuccessfulExitCode(results);
        assertEquals("2\n", results.stdout());
    }

    @Test
    @Order(11)
    public void printCalcWithFloatsWorks() {
        final ExecutionResults results = runText("print(.1 + .1)");
        LOG.debug("Translated Bash was:\n{}", results.stdin());
        assertSuccessfulExitCode(results);
        assertEquals(".2\n", results.stdout());
    }

    @Test
    @Order(11)
    public void printCalcWithMixedWorks() {
        final ExecutionResults results = runText("print(.1 + 1)");
        assertSuccessfulExitCode(results);
        assertEquals("1.1\n", results.stdout());
    }

    @Test
    @Order(20)
    public void multilineCalcWorks() {
        final ExecutionResults results = runText("""
                print(1 + 1)
                print(1-1)""");
        assertSuccessfulExitCode(results);

        final List<String> lines = results.stdoutLines();
        assertNotNull(lines);
        final int expected = 2;

        assertEquals(expected, lines.size(), "Expected %d lines but got %d".formatted(expected, lines.size()));
        assertEquals("2", lines.get(0));
        assertEquals("0", lines.get(1));
    }

    @Test
    @Order(30)
    public void stringConcatWorks() {
        final ExecutionResults results = runText("""
                print("hello" + " " + "world")""");
        assertSuccessfulExitCode(results);
        assertEquals("hello world\n", results.stdout());
    }

    @Test
    @Order(31)
    public void stringConcatWithNewlineWorks() {
        final ExecutionResults results = runText("""
                print("hello" + "\\n" + "world")""");
        assertSuccessfulExitCode(results);
        assertEquals("hello\nworld\n", results.stdout());
    }

    @Test
    @Order(40)
    public void stringBadOperatorThrows() {
        assertThrows(BashpileUncheckedException.class, () -> runText("""
                print("hello " * "world")"""));
    }

    @Test
    @Order(50)
    public void parenStringWorks() {
        final ExecutionResults results = runText("""
                print((("hello" + " world") + (", you" + " good?")))""");
        assertSuccessfulExitCode(results);
        assertEquals("hello world, you good?\n", results.stdout());
    }

    @Test
    @Order(80)
    public void intExpressionsWork() {
        final String bashpileScript = """
                print((3 + 5) * 3)
                print(32000 + 32000)
                print(64 + 64)""";
        final ExecutionResults results = runText(bashpileScript);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        final List<String> expected = List.of("24", "64000", "128");
        assertEquals(3, lines.size());
        assertEquals(expected, lines);
    }

    @Test
    @Order(90)
    public void parenIntExpressionsWork() {
        final ExecutionResults results = runText("print(((1 + 2) * (3 + 4)))");
        assertSuccessfulExitCode(results);
        assertEquals("21\n", results.stdout());
    }

    @Test
    @Order(100)
    public void floatExpressionsWork() {
        final ExecutionResults results = runText("""
                print((38. + 4) * .5)
                print(7.7 - 0.7)""");
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        final List<String> expected = List.of("21.0", "7.0");
        assertEquals(2, lines.size());
        assertEquals(expected, lines);
    }

    @Test
    @Order(110)
    public void numberAndStringExpressionsWork() {
        final ExecutionResults results = runText("""
                i: int
                j: float
                i = 4
                j = .5
                print((38. + i) * j)
                print(7.7 - ".7":float)""");
        assertSuccessfulExitCode(results);

        // confirm string is unquoted on typecast
        assertFalse(results.stdin().contains("\".7\""));

        // two declares in the header, definitions of i and j have one declare each
        // reassigns should not have declares
        final List<String> stdinLines = results.stdinLines();
        assertEquals(4, stdinLines.stream().filter(x -> x.contains("declare")).count());
        final List<String> lines = results.stdoutLines();
        final List<String> expected = List.of("21.0", "7.0");
        assertEquals(2, lines.size());
        assertEquals(expected, lines);
    }

    @Test
    @Order(120)
    public void boolToStringTypecastsWork() {
        final String bashpile = """
                function times2point5(x:float) -> float:
                    return x * 2.5
                print(0:int * 38)
                print(times2point5(1: float))
                print("Genre: " + true:str + " crime")""";
        final ExecutionResults results = runText(bashpile);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals("0", lines.get(0));
        assertEquals("2.5", lines.get(1));
        assertEquals("Genre: true crime", lines.get(2));
    }

    @Test
    @Order(121)
    public void boolToIntTypecastsFail() {
        final String bashpile = """
                print(false: int)""";
        assertThrows(TypeError.class, () -> runText(bashpile));
    }

    @Test
    @Order(122)
    public void boolToFloatTypecastsFail() {
        final String bashpile = """
                print(true: float)""";
        assertThrows(TypeError.class, () -> runText(bashpile));
    }

    @Test
    @Order(130)
    public void intToFloatOrStringTypecastsWork() {
        final String bashpile = """
                function times2point5(x:float) -> float:
                    return x * 2.5
                print(times2point5(8000000000000: float))
                print("NCC-" + 1701:str + "-D")""";
        final ExecutionResults results = runText(bashpile);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        // 8 trillion * 2.5 equals 20 trillion
        assertEquals("20000000000000.0", lines.get(0));
        assertEquals("NCC-1701-D", lines.get(1));
    }

    @Test
    @Order(131)
    public void intToBooleanTypecastsFail() {
        assertThrows(TypeError.class, () -> runText("""
                print(0: bool)"""));
    }

    @Test
    @Order(132)
    public void largeIntToBooleanTypecastsFail() {
        assertThrows(TypeError.class, () -> runText("""
                print(5000000000: bool)"""));
    }

    @Test
    @Order(133)
    public void intToIntListTypecastsFail() {
        assertThrows(TypeError.class, () -> runText("""
                print(5000000000: list<int>)"""));
    }

    @Test
    @Order(134)
    public void intVariableToFloatTypecastsFail() {
        assertThrows(TypeError.class, () -> runText("""
                x: int = 5000000000
                print(x: float)"""));
    }

    @Test
    @Order(140)
    public void floatToIntOrStringTypecastsWork() {
        final String bashpile = """
                function times2point5(x:int) -> float:
                    return x * 2.5: int
                print(times2point5(2.5 : int))
                print(1701.0:str + 1.0:str)""";
        final ExecutionResults results = runText(bashpile);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals("4", lines.get(0));
        assertEquals("1701.01.0", lines.get(1));
    }

    @Test
    @Order(150)
    public void strTypecastsWork() {
        final String bashpile = """
                function times2point5(x:int) -> float:
                    return x * 2.5
                function times2point5ForFloats(x:float) -> float:
                    return x * 2.5
                b1: bool = "true" : bool
                b2: bool = "TRUE" : bool
                b3: bool = "false" : bool
                print(#(if [ "$b1" = true ] && [ "$b2" = true ]; then echo "true"; else echo "false"; fi))
                print(#(if [ "$b1" = true ] && [ "$b2" = true ] && [ "$b3" = true ]; then
                    echo "true"
                else
                    echo "false"
                fi))
                print(times2point5("2.5" : float : int))
                print(times2point5ForFloats("2.50" : float))""";
        final ExecutionResults results = runText(bashpile);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals("true", lines.get(0));
        assertEquals("false", lines.get(1));
        assertEquals("5.0", lines.get(2));
        assertEquals("6.25", lines.get(3));
    }

    @Test
    @Order(151)
    public void negativeNumericStrTypecastsFail() {
        assertThrows(TypeError.class, () -> runText("""
                b1: bool = "-1." : bool"""));
    }

    @Test
    @Order(152)
    public void smallNumericStrTypecastsFail() {
        assertThrows(TypeError.class, () -> runText("""
                b2: bool = ".000001" : bool"""));
    }

    @Test
    @Order(153)
    public void zeroStrTypecastsFail() {
        assertThrows(TypeError.class, () -> runText("""
                b3: bool = "0" : bool"""));
    }

    @Test
    @Order(154)
    public void strTypecastsFloatToIntWorks() {
        final String bashpile = """
                function times2point5(x:int) -> float:
                    return x * 2.5
                print(times2point5("2.5" : int))""";
        final ExecutionResults result = runText(bashpile);
        assertSuccessfulExitCode(result);
        assertEquals("5.0\n", result.stdout());
    }

    @Test
    @Order(160)
    public void badStrTypecastsTextToFloatThrow() {
        final String bashpile = """
                function times2point5(x:float) -> float:
                    return x * 2.5
                print(times2point5("NCC-1701" : float))""";
        assertThrows(TypeError.class, () -> runText(bashpile));
    }

    @Test
    @Order(170)
    public void numberTypecastsFail() {
        assertThrows(TypeError.class, () -> runText("""
                b1: bool = (1 + 2 + 3) : bool"""));
    }

    @Test
    @Order(171)
    public void numberTypecastsWithParensFail() {
        assertThrows(TypeError.class, () -> runText("""
                b1: bool = (((2 - 3) * 2) + 2) : bool"""));
    }

    @Test
    @Order(180)
    public void argumentsExpresssionWorks() {
        final String bashpile = """
                print(arguments[1])""";
        final ExecutionResults results = runText(bashpile, "Hello", "World");
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals("Hello", lines.get(0));
    }

    @Test
    @Order(190)
    public void argumentsAllExpresssionWorks() {
        final String bashpile = """
                print(arguments[all])""";
        final ExecutionResults results = runText(bashpile, "Hello", "World");
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals("Hello World", lines.get(0));
    }

    @Test
    @Order(200)
    public void incrementWorks() {
        final String bashpile = """
                i: int = 0
                i++
                print(i)""";
        final ExecutionResults results = runText(bashpile);
        assertSuccessfulExitCode(results);
        assertEquals("1\n", results.stdout());
    }
}
