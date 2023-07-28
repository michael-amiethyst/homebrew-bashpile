package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import com.bashpile.exceptions.TypeError;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static com.bashpile.ListUtils.getLast;
import static org.junit.jupiter.api.Assertions.*;

@Order(20)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExpressionBashpileTest extends BashpileTest {

    @Test @Order(10)
    public void printCalcWorks() {
        var ret = runText("print(1 + 1)");
        assertNotNull(ret);
        assertEquals("2\n", ret.stdout());
    }

    @Test @Order(20)
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

    @Test @Order(30)
    public void stringConcatWorks() {
        var runResult = runText("""
                print("hello" + " " + "world")""");
        assertSuccessfulExitCode(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("hello world", getLast(outLines));
    }

    @Test @Order(40)
    public void stringBadOperatorThrows() {
        assertThrows(AssertionError.class, () -> runText("""
                print("hello " * "world")"""));
    }

    @Test @Order(50)
    public void parenStringWorks() {
        List<String> ret = runText("""
                print((("hello" + " world") + (", you" + " good?")))""").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("hello world, you good?", ret.get(0));
    }

    @Test @Order(80)
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

    @Test @Order(90)
    public void parenIntExpressionsWork() {
        List<String> ret = runText("print(((1 + 2) * (3 + 4)))").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("21", ret.get(0));
    }

    @Test @Order(100)
    public void floatExpressionsWork() {
        List<String> executionResults = runText("""
                print((38. + 4) * .5)
                print(7.7 - 0.7)""").stdoutLines();
        List<String> expected = List.of("21.0", "7.0");
        assertEquals(2, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test @Order(110)
    public void numberAndStringExpressionsWork() {
        ExecutionResults executionResults = runText("""
                i: int
                j: float
                i = 4
                j = .5
                print((38. + i) * j)
                print(7.7 - ".7":float)""");
        // confirm string is unquoted on typecast
        assertFalse(executionResults.stdin().contains("\".7\""));
        // header has 1 export, definitions of i and j have one export each.  Reassigns should not have exports
        assertEquals(3, executionResults.stdinLines().stream().filter(x -> x.contains("export")).count());
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "7.0");
        assertEquals(2, stdoutLines.size());
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(120)
    public void boolTypecastsWork() {
        final String bashpile = """
                function times2point5:float(x:float):
                    return x * 2.5
                print(false:int * 38)
                print(times2point5(true: float))
                print("Genre: " + true:str + " crime")""";
        final ExecutionResults result = runText(bashpile);
        final List<String> lines = result.stdoutLines();
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
        final ExecutionResults result = runText(bashpile);
        final List<String> lines = result.stdoutLines();
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
        final ExecutionResults result = runText(bashpile);
        final List<String> lines = result.stdoutLines();
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
        final ExecutionResults result = runText(bashpile);
        final List<String> lines = result.stdoutLines();
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
        final ExecutionResults result = runText(bashpile);
        final List<String> lines = result.stdoutLines();
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
