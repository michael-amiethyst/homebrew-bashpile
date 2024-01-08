package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

@Order(60)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConditionalsBashpileTest extends BashpileTest {

    @Test
    @Order(10)
    public void ifTrueWorks() {
        final ExecutionResults results = runText("""
                if true:
                    print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(20)
    public void ifUnsetArgumentsWorks() {
        final ExecutionResults results = runText("""
                if unset arguments[1]:
                    print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(30)
    public void ifIsEmptyWorks() {
        final ExecutionResults results = runText("""
                test: str = ""
                if isEmpty test:
                    print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(40)
    public void ifIsEmptyCanFail() {
        final ExecutionResults results = runText("""
                test: str = "notEmpty"
                if isEmpty test:
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(50)
    public void ifIsNotEmptyWorks() {
        final ExecutionResults results = runText("""
                test: str = "notEmpty"
                if isNotEmpty test:
                    print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(51)
    public void ifFloatWorks() {
        final ExecutionResults results = runText("""
                testFloat: float = 0.00
                if testFloat:
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(52)
    public void ifFloatCanFail() {
        final ExecutionResults results = runText("""
                testFloat: float = 1.5
                if testFloat:
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(60)
    public void ifIsNotEmptyCanFail() {
        final ExecutionResults results = runText("""
                test: str = ""
                if isNotEmpty test:
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(70)
    public void ifIsEmptyOnStringWorks() {
        final ExecutionResults results = runText("""
                if isEmpty "":
                    print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(80)
    public void ifIsEmptyOnStringCanFail() {
        final ExecutionResults results = runText("""
                if isEmpty "test":
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(90)
    public void ifCommandWorks() {
        final ExecutionResults results = runText("""
                if #((which ls 1>/dev/null)):
                    print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(100)
    public void ifCommandCanFail() {
        final ExecutionResults results = runText("""
                if #((which not_real_command)):
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(110)
    public void ifFunctionWorks() {
        final ExecutionResults results = runText("""
                function retT() -> bool:
                    return true
                if retT():
                    print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(120)
    public void ifFunctionCanFail() {
        final ExecutionResults results = runText("""
                function retF() -> bool:
                    return false
                if retF():
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(121)
    public void ifFunctionWithIntWorks() {
        final ExecutionResults results = runText("""
                function ret0() -> int:
                    return 0
                if ret0():
                    print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(122)
    public void ifFunctionWithIntCanFail() {
        final ExecutionResults results = runText("""
                function ret1() -> int:
                    return 1
                if ret1():
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(123)
    public void ifFunctionWithParenthesisWorks() {
        final ExecutionResults results = runText("""
                function ret42() -> int:
                    return 42
                if (ret42() * 0):
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(124)
    public void ifFunctionWithParenthesisCanFail() {
        final ExecutionResults results = runText("""
                function ret1() -> int:
                    return 1
                if (ret1() + 0):
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(130)
    public void ifWithInlineWorks() {
        final ExecutionResults results = runText("""
                if isNotEmpty #(printf "notEmpty"):
                    print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(140)
    public void ifWithInlineCanFail() {
        final ExecutionResults results = runText("""
                if isNotEmpty #(printf ""):
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(150)
    public void ifWithInlineCanNotErrorOut() {
        final ExecutionResults results = runText("""
                // #(printf "";exit 1) evaluates to the literal string "1", which is not empty
                if isEmpty #(printf "";exit 1):
                    print("true")
                else:
                    print("false")""");
        assertCorrectFormatting(results);
        assertFailedExitCode(results);
    }

    @Test
    @Order(160)
    public void ifWithInlineCanRaiseError() {
        final ExecutionResults results = runText("""
                #(rm -f error.log)
                #(trap 'cat error.log; exit 1' INT)
                ret: str = #(printf "errorLog" > error.log; kill -INT $$) creates "error.log":
                    if isEmpty ret:
                        print("true")
                    else:
                        print("false")""");
        assertCorrectFormatting(results);
        assertFailedExitCode(results);
        assertEquals("errorLog\n", results.stdout());
    }

    @Test
    @Order(170)
    public void ifWithNestedInlineWorks() {
        final ExecutionResults results = runText("""
                if isNotEmpty #(printf "$(printf "$(printf "notEmpty")")"):
                    print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(180)
    public void ifNotWhichWorks() {
        final ExecutionResults results = runText("""
                #(# shellcheck source=/dev/null)
                if not #(which brew > /dev/null 2>&1 ):
                    print('no brew')
                else:
                    print('brew')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("# shellcheck"));
        // when running during a brew install `which brew` fails (not error out)
        assertTrue(results.stdout().contains("brew"));
    }

    @Test
    @Order(190)
    public void nestedIfsWithInlineWorks() {
        final ExecutionResults results = runText("""
                tested: float = 0
                if tested:
                    if isNotEmpty #(printf "notEmpty"):
                        print("true")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("true\n", results.stdout());
    }

    @Test
    @Order(200)
    public void nestedIfsWithInlineCanFail() {
        final ExecutionResults results = runText("""
                tested: float = 000000.00
                if tested:
                    if isNotEmpty #(printf ""):
                        print("true")
                    else:
                        print("false")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(210)
    public void ifStringsEqualWorks() {
        final ExecutionResults results = runText("""
                hello: str = "hello"
                if hello == "hello":
                    print('equals')
                else:
                    print('nah')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("equals\n", results.stdout());
    }

    @Test
    @Order(220)
    public void ifStringsEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: int = 1234
                if hello == "1234":
                    print('equals')
                else:
                    print('nah')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("equals\n", results.stdout());
    }

    @Test
    @Order(230)
    public void ifStringsStrictlyEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: int = 1234
                if hello === "1234":
                    print('equals')
                else:
                    print('nah')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("nah\n", results.stdout());
    }

    @Test
    @Order(240)
    public void ifStringsNotEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: int = 1234
                if hello == "1235":
                    print('equals')
                else:
                    print('nah')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("nah\n", results.stdout());
    }

    @Test
    @Order(250)
    public void ifStringsNotStrictlyEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: int = 1234
                if hello !== "1234":
                    print('not equal')
                else:
                    print('ya')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("not equal\n", results.stdout());
    }

    @Test
    @Order(260)
    public void ifIntsEqualWithWorks() {
        final ExecutionResults results = runText("""
                hello: int = 1234
                if hello == 1234:
                    print('equals')
                else:
                    print('nah')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("equals\n", results.stdout());
    }

    @Test
    @Order(270)
    public void ifIntsStrictlyEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: int = 1234
                if hello === 1234.0:
                    print('equals')
                else:
                    print('nah')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("nah\n", results.stdout());
        assertFalse(results.stdin().contains("== \"1234\""));
    }

    @Test
    @Order(280)
    public void ifIntsNotEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: str = "1234"
                if hello != 1234:
                    print('not equals')
                else:
                    print('equals')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("equals\n", results.stdout());
    }

    @Test
    @Order(281)
    public void ifIntAndFloatNotEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: str = "1234five"
                numbers: float = 1234.
                if hello != numbers:
                    print('not equals')
                else:
                    print('equals')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("not equals\n", results.stdout());
    }

    @Test
    @Order(290)
    public void ifIntsNotStrictlyEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: str = "1234"
                if hello !== 1234:
                    print('not equal')
                else:
                    print('ya')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("not equal\n", results.stdout());
    }

    @Test
    @Order(300)
    public void ifFloatsEqualWithWorks() {
        final ExecutionResults results = runText("""
                hello: float = 1234.
                if hello == 1234:
                    print('equals')
                else:
                    print('nah')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("equals\n", results.stdout());
    }

    @Test
    @Order(310)
    public void ifFloatsStrictlyEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: float = 1234.
                if hello === 1234:
                    print('equals')
                else:
                    print('nah')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("nah\n", results.stdout());
        assertFalse(results.stdin().contains("== \"1234\""));
    }

    @Test
    @Order(320)
    public void ifFloatsNotEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: str = "1234.0"
                if hello != 1234.0:
                    print('not equals')
                else:
                    print('equals')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("equals\n", results.stdout());
    }

    @Test
    @Order(330)
    public void ifFloatsNotStrictlyEqualWithBashTypecastWorks() {
        final ExecutionResults results = runText("""
                hello: str = "1234.0"
                if hello !== 1234.0:
                    print('not equal')
                else:
                    print('ya')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("not equal\n", results.stdout());
    }
}
