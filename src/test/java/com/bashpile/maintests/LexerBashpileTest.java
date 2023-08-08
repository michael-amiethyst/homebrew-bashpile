package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Technically "print()" is a statement, but we need it to get any output at all.
 */
@Order(10)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LexerBashpileTest extends BashpileTest {

    @Test
    @Order(10)
    public void printWorks() {
        final ExecutionResults results = runText("print()");
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        assertEquals("\n", results.stdout());
    }

    @Test
    @Order(20)
    public void multilinePrintWorks() {
        final ExecutionResults results = runText("""
                print()
                print()""");
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        assertEquals("\n\n", results.stdout());
    }

    @Test
    @Order(30)
    public void boolWorks() {
        final ExecutionResults results = runText("""
                var: bool = false
                print(var)""");
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test
    @Order(40)
    public void intWorks() {
        final ExecutionResults results = runText("print(1701)");
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        assertEquals("1701\n", results.stdout());
    }

    @Test
    @Order(50)
    public void parenIntWorks() {
        final ExecutionResults results = runText("print(((1701)))");
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        assertEquals("1701\n", results.stdout());
    }

    @Test
    @Order(60)
    public void stringWorks() {
        final ExecutionResults results = runText("""
                print("NCC-1701")""");
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        assertEquals("NCC-1701\n", results.stdout());
    }

    @Test
    @Order(70)
    public void parenStringWorks() {
        final ExecutionResults results = runText("""
                print(((("hello"))))""");
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        assertEquals("hello\n", results.stdout());
    }

    @Test
    @Order(80)
    public void escapedStringWorks() {
        final ExecutionResults results = runPath(Path.of("escapedString.bashpile"));
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        assertEquals("\"hello\"\n", results.stdout());
    }

    @Test
    @Order(100)
    public void floatsWork() {
        final ExecutionResults results = runText("""
                print(.5)
                print(0.7)""");
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        final List<String> expected = List.of(".5", "0.7");
        assertEquals(2, lines.size());
        assertEquals(expected, lines);
    }

    @Test
    @Order(110)
    public void commentsWork() {
        final ExecutionResults results = runText("""
                // no leading 0
                print(.5)
                                
                // leading whole number
                print(1.7)""");
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        final List<String> expected = List.of(".5", "1.7");
        assertEquals(2, lines.size());
        assertEquals(expected, lines);
    }

    @Test @Order(120)
    public void blockCommentsWork() {
        final ExecutionResults results = runText("""
                /*
                    This language is
                    really starting to shape up.
                    It will replace Bash.
                                
                    /* Nested comment test */
                */
                                
                print((38. + 4) * .5)
                // anonymous block
                block:
                    x: float = 5.5 // lexical scoping
                    print(x + x)
                x: float = 7.7
                print(x - 0.7)
                """);
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        final List<String> stdoutLines = results.stdoutLines();
        final List<String> expected = List.of("21.0", "11.0", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(results.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(130)
    public void bashpileDocsWork() {
        final ExecutionResults results = runText("""
                /**
                    This language is
                    really starting to shape up.
                    It will replace Bash.
                */
                                
                // commented-out bashpileDoc
                /*
                /**
                From the ancient days
                Bash came to us, complex and
                tricky.  A new day dawns.
                */*/
                                
                // no leading 0
                print(.5)
                                
                // leading whole number
                print(1.7)
                """);
        assertCorrectFormatting(results);
        assertNoShellcheckWarnings(results);
        assertSuccessfulExitCode(results);
        final List<String> stdoutLines = results.stdoutLines();
        final List<String> expected = List.of(".5", "1.7");
        assertEquals(2, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(results.stdout()));
        assertEquals(expected, stdoutLines);
    }
}
