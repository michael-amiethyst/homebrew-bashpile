package com.bashpile.maintests;

import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static com.bashpile.StringUtils.join;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Order(40)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionBashpileTest extends BashpileTest {

    @Test
    @Order(10)
    public void functionDeclarationWorks() {
        List<String> executionResults = runText("""
                (38. + 5) * .3
                function functionName: int ():
                    x: float = 5.5
                    return x * x
                x: float = 7.7
                x * x
                """).stdoutLines();
        assertEquals(2, executionResults.size(),
                "Unexpected line length, was:\n" + join(executionResults));
    }

    @Test
    @Order(20)
    public void functionDeclarationParamsWork() {
        List<String> executionResults = runText("""
                (38. + 5) * .3
                function functionName:int(x:int, y:int):
                    return x * y
                x:float = 7.7
                x * x
                """).stdoutLines();
        assertEquals(2, executionResults.size());
    }

    @Test
    @Order(30)
    public void functionDeclarationBadParamsThrows() {
        assertThrows(TypeError.class, () -> runText("""
                (38. + 5) * .3
                function functionName:int(x:int, y:int):
                    return x * y
                x:float = 7.
                functionName(x,x)
                x * x"""));
    }

    @Test
    @Order(40)
    public void functionDeclarationDoubleDeclThrows() {
        assertThrows(UserError.class, () -> runText("""
                (38. + 5) * .3
                function functionName:int(x1:int, y2:int):
                    return x * y
                x:int = 7
                functionName(x,x)
                function functionName: float (z: float) ["double declaration not allowed"]:
                    return z * 3
                x * x"""));
    }

    @Test
    @Order(50)
    public void functionCallWorks() {
        List<String> executionResults = runText("""
                function circleArea:float(r:float):
                    return 3.14 * r * r
                print(circleArea(1))
                print(circleArea(-1))""").stdoutLines();
        assertEquals(2, executionResults.size());
        assertEquals("3.14", executionResults.get(0));
        assertEquals("3.14", executionResults.get(1));
    }

    @Test
    @Order(60)
    public void functionCallMultipleParamsWorks() {
        var executionResults = runText("""
                function rectArea: float (w: float, l: float):
                    return w * l
                print(rectArea(3, 4))
                """);
        assertExecutionSuccess(executionResults);
        assertEquals(1, executionResults.stdoutLines().size());
        assertEquals("12", executionResults.stdoutLines().get(0));
    }

    @Test
    @Order(70)
    public void functionCallReturnStringWorks() {
        var executionResults = runText("""
                function world: str ():
                    return "hello world"
                print(world())""");
        assertExecutionSuccess(executionResults);
        assertEquals(1, executionResults.stdoutLines().size());
        assertEquals("hello world", executionResults.stdoutLines().get(0));
    }

    @Test
    @Order(71)
    public void functionCallIgnoreReturnStringWorks() {
        var executionResults = runText("""
                function world: str ():
                    return "hello world"
                world()""");
        assertExecutionSuccess(executionResults);
        assertEquals("", executionResults.stdout());
    }

    @Test
    @Order(80)
    public void functionCallTagsWork() {
        var executionResults = runText("""
                function circleArea: float (r: float) ["math" "test" "function test"]:
                    return 3.14 * r * r
                print(circleArea(1))
                print(circleArea(-1))""");
        assertExecutionSuccess(executionResults);
        assertEquals(2, executionResults.stdoutLines().size());
        assertEquals("3.14", executionResults.stdoutLines().get(0));
    }

    @Test
    @Order(90)
    public void functionCallReturnStringBadTypeThrows() {
        assertThrows(TypeError.class, () -> runText("""
                function world: empty ():
                    return "hello world"
                print(world())"""));
    }

    @Test
    @Order(100)
    public void functionCallReturnEmptyBadTypeThrows() {
        assertThrows(TypeError.class, () -> runText("""
                function world: str ():
                    return
                print(world())"""));
    }

    @Test
    @Order(130)
    public void functionForwardDeclarationWorks() {
        var executionResults = runText("""
                function circleArea: float (r: float)
                                
                function twoCircleArea: float (r1: float, r2: float):
                    return circleArea(r1) + circleArea(r2)
                                
                function circleArea: float (r:float) ["helper"]:
                    return 3.14 * r * r
                                
                print(twoCircleArea(1, -1))""");
        assertExecutionSuccess(executionResults);
        assertEquals(1, executionResults.stdoutLines().size()
                , "Wrong length, was: " + join(executionResults.stdoutLines()));
        assertEquals(1,
                executionResults.stdinLines().stream().filter(x -> x.startsWith("circleArea")).count(),
                "Wrong circleArea count");
        assertEquals("6.28", executionResults.stdoutLines().get(0), "Wrong return");
    }

    @Test
    @Order(140)
    public void stringTypeWorks() {
        var executionResults = runText("""
                born: str = "to be wild"
                print(born)""");
        assertExecutionSuccess(executionResults);
        assertEquals(1, executionResults.stdoutLines().size()
                , "Wrong length, was: " + join(executionResults.stdoutLines()));
        assertEquals("to be wild", executionResults.stdoutLines().get(0),
                "Wrong return");
    }

    @Test
    @Order(150)
    public void functionDeclTypesWork() {
        List<String> executionResults = runText("""
                function circleArea: float(r: float) ["need to remove the quotes"]:
                    return 3.14 * r * r
                print(circleArea(1))
                print(circleArea(-1))""").stdoutLines();
        assertEquals(2, executionResults.size());
        assertEquals("3.14", executionResults.get(0));
        assertEquals("3.14", executionResults.get(1));
    }

    @Test
    @Order(160)
    public void badFunctionDeclTypesThrow() {
        assertThrows(TypeError.class, () -> runText("""
                function circleArea: float(r: float) ["need to remove the quotes"]:
                    return 3.14 * r * r
                print(circleArea(1))
                print(circleArea("Hello World"))"""));
    }

    @Test
    @Order(170)
    public void functionDeclTypesCalcExpressionsWork() {
        var executionResults = runText("""
                function circleArea: float(r: float) ["need to remove the quotes"]:
                    return 3.14 * r * r
                print(circleArea(.5 + .5))""");
        List<String> lines = executionResults.stdoutLines();
        assertExecutionSuccess(executionResults);
        assertEquals(1, lines.size(), "Wrong length, was: " + join(lines));
        assertEquals("3.14", lines.get(0));
    }

    @Test
    @Order(180)
    public void functionDeclTypesBadCalcExpressionThrows() {
        assertThrows(UserError.class, () -> runText("""
                function circleArea: float(r: float) ["need to remove the quotes"]:
                    return 3.14 * r * r
                print(circleArea(.5 + x))"""));
    }

    @Test
    @Order(190)
    public void functionDeclTypesBadCalcExpressionNestedThrows() {
        assertThrows(TypeError.class, () -> runText("""
                function circleArea: float(r: int) ["need to remove the quotes"]:
                    function circleAreaHelper: float(r: float):
                        return 3.14 * r * r
                    r = circleAreaHelper(r)
                    return r
                print(circleArea(.5 + 0.5))"""));
    }
}
