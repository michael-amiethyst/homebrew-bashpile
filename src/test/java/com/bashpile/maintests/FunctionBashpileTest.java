package com.bashpile.maintests;

import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static com.bashpile.Strings.join;
import static org.junit.jupiter.api.Assertions.*;

@Order(40)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionBashpileTest extends BashpileTest {

    @Test
    @Order(10)
    public void functionDeclarationWorks() {
        final ExecutionResults results = runText("""
                print((38. + 5) * .3)
                function functionName() -> int:
                    x: float = 5.5
                    return x * x
                x: float = 7.7
                print(x * x)
                """);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals(2, lines.size(),
                "Unexpected line length, was:\n" + join(lines));
        assertEquals("12.9", lines.get(0));
        assertEquals("59.2", lines.get(1));
    }

    @Test
    @Order(20)
    public void functionDeclarationParamsWork() {
        final ExecutionResults results = runText("""
                print((38. + 5) * .3)
                function functionName(x:int, y:int) -> int:
                    return x * y
                x:float = 7.7
                print(x * x)
                """);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals(2, lines.size(),
                "Unexpected line length, was:\n" + join(lines));
        assertEquals("12.9", lines.get(0));
        assertEquals("59.2", lines.get(1));
    }

    @Test
    @Order(30)
    public void functionDeclarationBadParamsThrows() {
        assertThrows(TypeError.class, () -> runText("""
                (38. + 5) * .3
                function functionName(x:int, y:int) -> int:
                    return x * y
                x:float = 7.
                functionName(x,x)
                x * x"""));
    }

    // TODO make float, list test.  Make test for optional 2nd argument, multiple optional arguments
    @Test
    @Order(31)
    public void functionDeclarationReturnUndeclaredIntThrows() {
        assertThrows(TypeError.class, () -> runText("""
                (38. + 5) * .3
                function functionName(x:int, y:int):
                    return x * y
                x:int = 7
                functionName(x,x)
                x * x"""));
    }

    @Test
    @Order(40)
    public void functionDeclarationDoubleDeclThrows() {
        assertThrows(UserError.class, () -> runText("""
                (38. + 5) * .3
                function functionName(x1:int, y2:int)->int:
                    return x * y
                x:int = 7
                functionName(x,x)
                function functionName(z: float) ["double declaration not allowed"] -> float:
                    return z * 3
                x * x"""));
    }

    @Test
    @Order(50)
    public void functionCallWorks() {
        final ExecutionResults results = runText("""
                function circleArea(r:float) -> float:
                    return 3.14 * r * r
                print(circleArea(1))
                print(circleArea(-1))""");
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals(2, lines.size());
        assertEquals("3.14", lines.get(0));
        assertEquals("3.14", lines.get(1));
    }

    @Test
    @Order(60)
    public void functionCallMultipleParamsWorks() {
        final ExecutionResults results = runText("""
                function rectArea (w: float, l: float) -> float:
                    return w * l
                print(rectArea(3, 4))
                """);
        assertSuccessfulExitCode(results);
        assertEquals("12\n", results.stdout());
    }

    @Test
    @Order(70)
    public void functionCallReturnStringWorks() {
        final ExecutionResults results = runText("""
                function world() -> str:
                    return "hello world"
                print(world())""");
        assertSuccessfulExitCode(results);
        assertEquals("hello world\n", results.stdout());
    }

    @Test
    @Order(71)
    public void functionCallIgnoreReturnStringWorks() {
        final ExecutionResults results = runText("""
                function world() -> str:
                    return "hello world"
                world()""");
        assertSuccessfulExitCode(results);
        assertEquals("", results.stdout());
    }

    @Test
    @Order(80)
    public void functionCallTagsWork() {
        final ExecutionResults results = runText("""
                function circleArea(r: float) ["math" "test" "function test"] -> float:
                    return 3.14 * r * r
                print(circleArea(1))
                print(circleArea(-1))""");
        assertSuccessfulExitCode(results);
        assertEquals("3.14\n3.14\n", results.stdout());
    }

    @Test
    @Order(90)
    public void functionCallReturnStringBadTypeThrows() {
        assertThrows(TypeError.class, () -> runText("""
                function world() -> empty:
                    return "hello world"
                print(world())"""));
    }

    @Test
    @Order(91)
    public void functionCallNoReturnWorks() {
        final ExecutionResults results = runText("""
                function noReturnFunction():
                    print("Side effects only; no return statement")
                noReturnFunction()""");
        assertSuccessfulExitCode(results);
        assertEquals("Side effects only; no return statement\n", results.stdout());
    }

    @Test
    @Order(92)
    public void functionCallOptionalArgumentWorks() {
        final ExecutionResults results = runText("""
                function square(first: int = 5) -> int:
                    ret: int
                    if isset first and isNotEmpty first:
                        ret = first * first
                    else:
                        // with default value we should never get here
                        ret = 42
                    return ret
                print(square())""");
        assertSuccessfulExitCode(results);
        assertEquals("25\n", results.stdout());
    }

    @Test
    @Order(92)
    public void functionCallOptionalArgumentWithEmptyWorks() {
        final ExecutionResults results = runText("""
                function square(first: int = empty) -> int:
                    ret: int
                    if isset first and isNotEmpty first:
                        ret = first * first
                    else:
                        ret = 42
                    return ret
                print(square())""");
        assertSuccessfulExitCode(results);
        assertEquals("42\n", results.stdout());
    }

    @Test
    @Order(93)
    public void functionCallOptionalArgumentWithStringWorks() {
        final ExecutionResults results = runText("""
                function hello(world: str = "World") -> str:
                    return "Hello " + world
                print(hello())""");
        assertSuccessfulExitCode(results);
        assertEquals("Hello World\n", results.stdout());
    }

    @Test
    @Order(94)
    public void functionCallOptionalArgumentsWorks() {
        final ExecutionResults results = runText("""
                function hello(world: str = "World", name: str = "James T. Kirk") -> str:
                    return "Hello " + world + ", and hello to " + name
                print(hello())
                print(hello("Earth"))
                print(hello("Ferenginar", "Rom"))""");
        assertSuccessfulExitCode(results);
        assertEquals("""
                Hello World, and hello to James T. Kirk
                Hello Earth, and hello to James T. Kirk
                Hello Ferenginar, and hello to Rom
                """, results.stdout());
    }

    @Test
    @Order(100)
    public void functionCallRegularAndDefaultedArgumentsWorks() {
        final ExecutionResults results = runText("""
                function hello(world: str, name: str = "James T. Kirk") -> str:
                    return "Hello " + world + ", and hello to " + name
                print(hello("Earth"))
                print(hello("Ferenginar", "Rom"))""");
        assertSuccessfulExitCode(results);
        assertEquals("""
                Hello Earth, and hello to James T. Kirk
                Hello Ferenginar, and hello to Rom
                """, results.stdout());
    }

    @Test
    @Order(110)
    public void assertGnuGetoptCallNoReturnWorks() {
        final ExecutionResults results = runText("""
                /**
                 * Checks for GNU getopt vs FreeBSD getopt.
                 * FreeBSD getopt cannot handle long options (e.g. --help) like we need.
                 */
                function assertGnuGetopt():
                    if #(getopt --help) == " --":
                        print("FreeBSD getopt found.  Please ensure that brew's gnu-getopt is first on the classpath.  Exiting...")
                        exit 2""");
        assertSuccessfulExitCode(results);
    }

    @Test
    @Order(120)
    public void functionCallReturnEmptyBadTypeThrows() {
        assertThrows(TypeError.class, () -> runText("""
                function world() -> str:
                    return
                print(world())"""));
    }

    @Test
    @Order(130)
    public void functionForwardDeclarationWorks() {
        final ExecutionResults results = runText("""
                function circleArea(r: float) -> float
                
                function twoCircleArea(r1: float, r2: float) -> float:
                    return circleArea(r1) + circleArea(r2)
                
                function circleArea(r:float) ["helper"] -> float:
                    return 3.14 * r * r
                
                print(twoCircleArea(1, -1))""");
        assertSuccessfulExitCode(results);
        assertEquals(1, results.stdoutLines().size()
                , "Wrong length, was: " + join(results.stdoutLines()));
        assertEquals(1,
                results.stdinLines().stream().map(String::trim).filter(x -> x.startsWith("circleArea")).count(),
                "Wrong circleArea count");
        assertEquals("6.28", results.stdoutLines().get(0), "Wrong return");
    }

    @Test
    @Order(131)
    public void functionForwardDeclarationNoReturnWorks() {
        final ExecutionResults results = runText("""
                function printCircleArea(r: float)
                
                function twoCircleArea(r1: float, r2: float) -> float:
                    // total: float = circleArea(r1) + circleArea(r2)
                    // print(total)
                    return 3.14
                
                function printCircleArea(r:float) ["helper"]:
                    print(3.14 * r * r)
                
                printCircleArea(1)""");
        assertSuccessfulExitCode(results);
        assertEquals(1, results.stdoutLines().size(), "Wrong length, was: " + join(results.stdoutLines()));
        assertEquals("3.14\n", results.stdout(), "Wrong return");
    }

    @Test
    @Order(132)
    public void functionDeclarationNoReturnWorks() {
        final ExecutionResults results = runText("""
                function circleArea(r: float) -> float

                function printTwoCircleArea(r1: float, r2: float):
                    total: float = circleArea(r1) + circleArea(r2)
                    print(total)

                function circleArea(r:float) ["helper"] -> float:
                    return 3.14 * r * r

                printTwoCircleArea(1, -1)""");
        assertSuccessfulExitCode(results);
        assertEquals(1, results.stdoutLines().size(),
                "Wrong length, was: " + join(results.stdoutLines()));
        assertEquals(1,
                results.stdinLines().stream().map(String::trim).filter(x -> x.startsWith("circleArea")).count(),
                "Wrong circleArea count");
        assertEquals("6.28", results.stdoutLines().get(0), "Wrong return");
    }

    @Test
    @Order(140)
    public void stringTypeWorks() {
        final ExecutionResults results = runText("""
                born: str = "to be wild"
                print(born)""");
        assertSuccessfulExitCode(results);
        assertEquals(1, results.stdoutLines().size(),
                "Wrong length, was: " + join(results.stdoutLines()));
        assertEquals("to be wild\n", results.stdout(), "Wrong return");
    }

    @Test
    @Order(150)
    public void functionDeclTypesWork() {
        final ExecutionResults results = runText("""
                function circleArea(r: float) ["example tag"] -> float:
                    return 3.14 * r * r
                print(circleArea(1))
                print(circleArea(-1))""");
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        assertEquals(2, lines.size());
        assertEquals("3.14", lines.get(0));
        assertEquals("3.14", lines.get(1));
    }

    @Test
    @Order(160)
    public void badFunctionDeclTypesThrow() {
        assertThrows(TypeError.class, () -> runText("""
                function circleArea(r: float) ["example tag"] -> float:
                    return 3.14 * r * r
                print(circleArea(1))
                print(circleArea("Hello World"))"""));
    }

    @Test
    @Order(170)
    public void functionDeclTypesCalcExpressionsWork() {
        final ExecutionResults results = runText("""
                function circleArea(r: float) ["example tag"] -> float:
                    return 3.14 * r * r
                print(circleArea(.5 + .5))""");
        assertSuccessfulExitCode(results);
        assertEquals("3.14\n", results.stdout());
    }

    @Test
    @Order(180)
    public void functionDeclTypesBadCalcExpressionThrows() {
        assertThrows(UserError.class, () -> runText("""
                function circleArea(r: float) ["example tag"] -> float:
                    return 3.14 * r * r
                print(circleArea(.5 + x))"""));
    }

    @Test
    @Order(190)
    public void functionDeclTypesBadCalcExpressionNestedThrows() {
        assertThrows(TypeError.class, () -> runText("""
                function circleArea(r: int) ["example tag"] -> float:
                    function circleAreaHelper(r: float) -> float:
                        return 3.14 * r * r
                    r = circleAreaHelper(r)
                    return r
                print(circleArea(.5))"""));
    }

    @Test
    @Order(200)
    public void functionDeclWithArgumentsAllExpressionWorks() {
        final ExecutionResults results = runText("""
                function circleArea(log: str, args: list<str>) ["example tag"] -> float:
                    shift
                    a1: list<str> = arguments[1]: list<str>
                    r: int = a1[0]: int
                    print(log)
                    return 3.14 * r * r
                print(circleArea("test", arguments[all]))""", "1");
        assertSuccessfulExitCode(results);
        assertEquals("test\n3.14\n", results.stdout());
    }

    @Test
    @Order(210)
    public void functionDeclWithArgumentsAllAtStartExpressionWorks() {
        final ExecutionResults results = runText("""
                function circleArea(args: list<str>, r: float) ["example tag"] -> float:
                    print(args[0])
                    print(args)
                    print(args[all])
                    return 3.14 * r * r
                print(circleArea(arguments[all], 1))""", "Hello World");
        assertSuccessfulExitCode(results);
        assertEquals("Hello\nHello World\nHello World\n3.14\n", results.stdout());
    }

}
