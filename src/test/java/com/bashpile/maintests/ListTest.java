package com.bashpile.maintests;

import com.bashpile.exceptions.BashpileUncheckedAssertionException;
import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

@Order(60)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ListTest extends BashpileTest {
    /** Empty list */
    @Test @Order(10)
    public void emptyListDeclarationWorks() {
        final ExecutionResults results = runText("emptyList: list<str> = listOf()");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("", results.stdout());
    }

    @Test
    @Order(20)
    public void printingAnEmptyListFails() {
        assertThrows(BashpileUncheckedAssertionException.class, () -> runText("""
                emptyList: list<str> = listOf()
                print(emptyList)"""));
    }

    /** Single int list */
    @Test
    @Order(30)
    public void singleItemListDeclarationWorks() {
        final ExecutionResults results = runText("""
                intList: list<int> = listOf(1)
                print(intList[0])""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        // TODO uncomment and implement
//        assertEquals(results.stdout(), "0\n");
    }

    // TODO many item'd list, add to list with memory left, add to list with no memory left, add wrong type
}