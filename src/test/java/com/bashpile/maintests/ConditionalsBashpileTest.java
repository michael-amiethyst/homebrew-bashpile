package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(70)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConditionalsBashpileTest extends BashpileTest {

    // TODO test with if and function calls
    // TODO add relationals, equality
    // TODO add unset arguments[1]
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
}
