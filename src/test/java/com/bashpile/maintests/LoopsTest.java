package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(70)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoopsTest extends BashpileTest {
    @Test @Order(10)
    public void whileLoopWorks() {
        final ExecutionResults results = runText("""
                i: int = 0
                while i < 5:
                    print(i)
                    i = i + 1
                """);
        assertCorrectFormatting(results);
        assertTrue(results.stdin().contains("# while statement"));
        assertSuccessfulExitCode(results);
        assertEquals("0\n1\n2\n3\n4\n", results.stdout());
    }

    @Test @Order(20)
    public void whileLoopWithShellStringWorks() {
        final ExecutionResults results = runText("""
                i: int = 0
                while #([ $i -lt 5 ]):
                    print(i)
                    i = i + 1
                """);
        assertCorrectFormatting(results);
        assertTrue(results.stdin().contains("# while statement"));
        assertSuccessfulExitCode(results);
        assertEquals("0\n1\n2\n3\n4\n", results.stdout());
    }

}
