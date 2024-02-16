package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void printingAnEmptyListSucceeds() {
        final ExecutionResults results = runText("""
                emptyList: list<str> = listOf()
                print(emptyList)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("\n", results.stdout());
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
        assertEquals("1\n", results.stdout());
    }

    /** Multistring list */
    @Test
    @Order(40)
    public void multiItemListDeclarationWorks() {
        final ExecutionResults results = runText("""
                intList: list<str> = listOf("one", "two", "three")
                print(intList)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("one two three\n", results.stdout());
    }

    @Test
    @Order(50)
    public void addItemToListWorks() {
        final ExecutionResults results = runText("""
                strList: list<str> = listOf("one", "two", "three")
                strList += "four"
                print(strList)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("one two three four\n", results.stdout());
    }

    @Test
    @Order(60)
    public void addListToListWorks() {
        final ExecutionResults results = runText("""
                strList: list<str> = listOf("one", "two", "three")
                strList += listOf("four", "five")
                print(strList)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("one two three four five\n", results.stdout());
    }

    @Test
    @Order(70)
    public void mutateIndexOnListWorks() {
        final ExecutionResults results = runText("""
                strList: list<str> = listOf("one", "two", "three")
                strList[0] = "1"
                print(strList)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("1 two three\n", results.stdout());
    }

    // TODO add wrong type (item and list)

    // TODO typecasts from list to int?, different list types (e.g. list<string> to list<int>)
}