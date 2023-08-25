package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO test ids, parens, nested ifs to test expected stack
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
                function retT: bool():
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
                function retF: bool():
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
                function ret0: int():
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
                function ret1: int():
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
                function ret42: int():
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
                function ret1: int():
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
                    #(source ~/bash.bashrc)
                    print('no brew')
                else:
                    print('brew')""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("# shellcheck"));
        assertEquals("brew\n", results.stdout());
    }
}
