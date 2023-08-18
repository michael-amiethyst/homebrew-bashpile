package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO test with function calls, other expressions.  parens, nested inlines
@Order(70)
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
}
