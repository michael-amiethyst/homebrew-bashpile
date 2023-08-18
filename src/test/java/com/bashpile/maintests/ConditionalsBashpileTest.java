package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO test with function calls, other expressions
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
}
