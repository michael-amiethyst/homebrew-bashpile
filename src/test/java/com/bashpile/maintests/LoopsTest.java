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
    public void emptyListDeclarationWorks() {
        final ExecutionResults results = runText("""
                i: int = 0
                while i < 5:
                    print(i)
                    i = i + 1
                """);
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("", results.stdout());
    }

}
