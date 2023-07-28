package com.bashpile.maintests;

import com.bashpile.commandline.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Path;
import java.util.List;

import static com.bashpile.ListUtils.getLast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Technically "print()" is a statement, but we need it to get any output at all.
 */
@Order(10)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LexerBashpileTest extends BashpileTest {

    @Test
    @Order(10)
    public void printWorks() {
        String ret = runText("print()").stdout();
        assertNotNull(ret);
        assertEquals("\n", ret);
    }

    @Test
    @Order(20)
    public void multilinePrintWorks() {
        String translatedLines = runText("""
                print()
                print()""").stdout();
        assertNotNull(translatedLines);
        assertEquals("\n\n", translatedLines);
    }

    @Test
    @Order(30)
    public void boolWorks() {
        List<String> outLines = runText("""
                var: bool = false
                print(var)""").stdoutLines();
        assertEquals("false", outLines.get(0));
    }

    @Test
    @Order(40)
    public void intWorks() {
        List<String> bashLines = runText("print(42)").stdinLines();
        assertEquals("echo 42", getLast(bashLines));
    }

    @Test
    @Order(50)
    public void parenIntWorks() {
        List<String> ret = runText("print(((21)))").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("21", ret.get(0));
    }

    @Test
    @Order(60)
    public void stringWorks() {
        var runResult = runText("""
                print("world")""");
        assertSuccessfulExitCode(runResult);
        List<String> outLines = runResult.stdoutLines();
        assertEquals("world", getLast(outLines));
    }

    @Test
    @Order(70)
    public void parenStringWorks() {
        List<String> ret = runText("""
                print(((("hello"))))""").stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("hello", ret.get(0));
    }

    @Test
    @Order(80)
    public void escapedStringWorks() {
        List<String> ret = runPath(Path.of("escapedString.bashpile")).stdoutLines();
        assertEquals(1, ret.size(), "Unexpected number of lines");
        assertEquals("\"hello\"", ret.get(0));
    }

    @Test
    @Order(100)
    public void floatsWork() {
        List<String> executionResults = runText("""
                print(.5)
                print(0.7)""").stdoutLines();
        List<String> expected = List.of(".5", "0.7");
        assertEquals(2, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test
    @Order(110)
    public void commentsWork() {
        List<String> executionResults = runText("""
                // no leading 0
                print(.5)
                                
                // leading whole number
                print(1.7)""").stdoutLines();
        List<String> expected = List.of(".5", "1.7");
        assertEquals(2, executionResults.size());
        assertEquals(expected, executionResults);
    }

    @Test @Order(120)
    public void blockCommentsWork() {
        ExecutionResults executionResults = runText("""
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
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of("21.0", "11.0", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(130)
    public void bashpileDocsWork() {
        ExecutionResults executionResults = runText("""
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
        List<String> stdoutLines = executionResults.stdoutLines();
        List<String> expected = List.of(".5", "1.7");
        assertEquals(2, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(executionResults.stdout()));
        assertEquals(expected, stdoutLines);
    }
}
