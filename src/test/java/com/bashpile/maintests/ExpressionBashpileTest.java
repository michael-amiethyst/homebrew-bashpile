package com.bashpile.maintests;

import com.bashpile.exceptions.TypeError;
import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Order(20)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExpressionBashpileTest extends BashpileTest {

    @Test @Order(10)
    public void printCalcWorks() {
        final ExecutionResults results = runText("print(1 + 1)");
        assertNotNull(results);
        assertSuccessfulExitCode(results);
        assertEquals("2\n", results.stdout());
    }

    @Test @Order(20)
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

    @Test @Order(30)
    public void stringConcatWorks() {
        final ExecutionResults results = runText("""
                print("hello" + " " + "world")""");
        assertSuccessfulExitCode(results);
        assertEquals("hello world\n", results.stdout());
    }

    @Test @Order(40)
    public void stringBadOperatorThrows() {
        assertThrows(AssertionError.class, () -> runText("""
                print("hello " * "world")"""));
    }

    @Test @Order(50)
    public void parenStringWorks() {
        final ExecutionResults results = runText("""
                print((("hello" + " world") + (", you" + " good?")))""");
        assertSuccessfulExitCode(results);
        assertEquals("hello world, you good?\n", results.stdout());
    }

    @Test @Order(80)
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

    @Test @Order(90)
    public void parenIntExpressionsWork() {
        final ExecutionResults results = runText("print(((1 + 2) * (3 + 4)))");
        assertSuccessfulExitCode(results);
        assertEquals("21\n", results.stdout());
    }

    @Test @Order(100)
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

    @Test @Order(110)
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

        // header has 1 export, definitions of i and j have one export each.  Reassigns should not have exports
        assertEquals(3, results.stdinLines().stream().filter(x -> x.contains("export")).count());
        final List<String> lines = results.stdoutLines();
        final List<String> expected = List.of("21.0", "7.0");
        assertEquals(2, lines.size());
        assertEquals(expected, lines);
    }

    @Test @Order(120)
    public void boolTypecastsWork() {
        final String bashpile = """
                function times2point5:float(x:float):
                    return x * 2.5
                print(false:int * 38)
                print(times2point5(true: float))
                print("Genre: " + true:str + " crime")""";
        final ExecutionResults results = runText(bashpile);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals("0", lines.get(0));
        assertEquals("2.5", lines.get(1));
        assertEquals("Genre: true crime", lines.get(2));
    }

    @Test @Order(130)
    public void intTypecastsWork() {
        final String bashpile = """
                function times2point5:float(x:float):
                    return x * 2.5
                b1: bool = 8000000000 : bool
                b2: bool = -1 : bool
                b3: bool = 0 : bool
                print(#(if [ "$b1" = true ] && [ "$b2" = true ]; then echo "true"; else echo "false"; fi))
                print(#(if [ "$b1" = true ] && [ "$b2" = true ] && [ "$b3" = true ]; then
                    echo "true"
                else
                    echo "false"
                fi))
                print(times2point5(8000000000000: float))
                print("NCC-" + 1701:str + "-D")""";
        final ExecutionResults results = runText(bashpile);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals("true", lines.get(0));
        assertEquals("false", lines.get(1));
        assertEquals("20000000000000.0", lines.get(2));
        assertEquals("NCC-1701-D", lines.get(3));
    }

    @Test @Order(140)
    public void floatTypecastsWork() {
        final String bashpile = """
                function times2point5:float(x:int):
                    return x * 2.5
                b1: bool = 8000000000.9999 : bool
                b2: bool = -1.0 : bool
                b3: bool = 0.0 : bool
                print(#(if [ "$b1" = true ] && [ "$b2" = true ]; then echo "true"; else echo "false"; fi))
                print(#(if [ "$b1" = true ] && [ "$b2" = true ] && [ "$b3" = true ]; then
                    echo "true"
                else
                    echo "false"
                fi))
                print(times2point5(2.5 : int))
                print(1701.0:str + 1.0:str)""";
        final ExecutionResults results = runText(bashpile);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals("true", lines.get(0));
        assertEquals("false", lines.get(1));
        assertEquals("5.0", lines.get(2));
        assertEquals("1701.01.0", lines.get(3));
    }

    @Test @Order(150)
    public void strTypecastsWork() {
        final String bashpile = """
                function times2point5:float(x:int):
                    return x * 2.5
                function times2point5ForFloats:float(x:float):
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

    @Test @Order(150)
    public void numericStrTypecastsWork() {
        final String bashpile = """
                b1: bool = "-1." : bool
                b2: bool = ".000001" : bool
                b3: bool = "0" : bool
                print(#(if [ "$b1" = true ] && [ "$b2" = true ]; then echo "true"; else echo "false"; fi))
                print(#(if [ "$b1" = true ] && [ "$b2" = true ] && [ "$b3" = true ]; then
                    echo "true"
                else
                    echo "false"
                fi))""";
        final ExecutionResults results = runText(bashpile);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals("true", lines.get(0));
        assertEquals("false", lines.get(1));
    }

    @Test @Order(150)
    public void badStrTypecastsFloatToIntThrow() {
        final String bashpile = """
                function times2point5:float(x:int):
                    return x * 2.5
                print(times2point5("2.5" : int))""";
        assertThrows(TypeError.class, () -> runText(bashpile));
    }

    @Test @Order(160)
    public void badStrTypecastsTextToFloatThrow() {
        final String bashpile = """
                function times2point5:float(x:float):
                    return x * 2.5
                print(times2point5("NCC-1701" : float))""";
        assertThrows(TypeError.class, () -> runText(bashpile));
    }

    @Test @Order(170)
    public void numberTypecastsWork() {
        final String bashpile = """
                b1: bool = (1 + 2 + 3) : bool""";
        assertThrows(TypeError.class, () -> runText(bashpile));
    }
}
