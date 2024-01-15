package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(60)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ListTest extends BashpileTest {
    /** Empty list */
    @Test
    @Order(10)
    public void emptyListDeclarationWorks() {
        // TODO add print statement
        final ExecutionResults results = runText("emptyList: list<str> = listOf()");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
    }

    /** Single int list */
    @Test
    @Order(20)
    public void singleItemListDeclarationWorks() {
        // TODO add print statement
        final ExecutionResults results = runText("""
                intList: list<int> = listOf(1)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
    }

    // TODO many item'd list, add to list with memory left, add to list with no memory left, add wrong type
}