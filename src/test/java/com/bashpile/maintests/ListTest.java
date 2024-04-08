package com.bashpile.maintests;

import com.bashpile.exceptions.TypeError;
import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

@Order(60)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ListTest extends BashpileTest {
    /**
     * Empty list
     */
    @Test
    @Order(10)
    public void emptyListDeclarationWorks() {
        final ExecutionResults results = runText("emptyList: list<str> = listOf()");
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
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("\n", results.stdout());
    }

    /**
     * Single int list
     */
    @Test
    @Order(30)
    public void singleItemListDeclarationWorks() {
        final ExecutionResults results = runText("""
                intList: list<int> = listOf(1)
                print(intList[0])""");
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("1\n", results.stdout());
    }

    /**
     * Multistring list
     */
    @Test
    @Order(40)
    public void multiItemListDeclarationWorks() {
        final ExecutionResults results = runText("""
                intList: list<str> = listOf("one", "two", "three")
                print(intList)""");
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
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("1 two three\n", results.stdout());
    }

    @Test
    @Order(80)
    public void newIndexOnListWorks() {
        final ExecutionResults results = runText("""
                strList: list<str> = listOf("one", "two", "three")
                strList[3] = "four"
                print(strList)""");
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("one two three four\n", results.stdout());
    }

    @Test
    @Order(90)
    public void negativeIndexOnListWorks() {
        final ExecutionResults results = runText("""
                strList: list<str> = listOf("one", "two", "three")
                strList[-1] = "3"
                print(strList)""");
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertEquals("one two 3\n", results.stdout());
    }

    @Test
    @Order(100)
    public void largeNegativeIndexOnListFails() {
        final ExecutionResults results = runText("""
                strList: list<str> = listOf("one", "two", "three")
                strList[-10] = "3"
                print(strList)""");
        assertSuccessfulExitCode(results);
        assertTrue(results.stdin().contains("declare -a"));
        assertTrue(results.stdout().contains("bad"));
    }

    @Test
    @Order(110)
    public void addWrongItemTypeOnListFails() {
        assertThrows(TypeError.class, () -> runText("""
                strList: list<str> = listOf("one", "two", "three")
                strList[0] = 1
                print(strList)"""));
    }

    @Test
    @Order(120)
    public void addWrongListTypeOnListFails() {
        assertThrows(TypeError.class, () -> runText("""
                strList: list<str> = listOf("one", "two", "three")
                strList += listOf(1, 2)
                print(strList)"""));
    }

    @Test
    @Order(130)
    public void assignFromListAccessWorks() {
        final ExecutionResults results = runText("""
                strList: list<str> = listOf("one", "two", "three")
                indexed: str = strList[-1]
                print(indexed)""");
        assertSuccessfulExitCode(results);
        assertEquals("three\n", results.stdout());
    }

    @Test
    @Order(140)
    public void assignFromDifferentListTypeThrows() {
        assertThrows(TypeError.class, () -> runText("""
                strList: list<str> = listOf("one", "two", "three")
                intList: list<int> = strList
                print(strList)"""));
    }

    @Test
    @Order(150)
    public void assignFromDifferentListTypeWithCastWorks() {
        var results = runText("""
                strList: list<str> = listOf("1", "2", "3")
                intList: list<int> = strList: list<int>
                print(intList)""");
        assertSuccessfulExitCode(results);
        assertEquals("1 2 3\n", results.stdout());
    }

    @Test
    @Order(160)
    public void assignFromDifferentListTypeWithCastThenAddWorks() {
        var results = runText("""
                strList: list<str> = listOf("1", "2", "3")
                intList: list<int> = strList: list<int>
                print(intList[0] + intList[1])""");
        assertSuccessfulExitCode(results);
        assertEquals("3\n", results.stdout());
    }

    @Test
    @Order(170)
    public void additionOfStringElementsWorks() {
        var results = runText("""
                strList: list<str> = listOf("1", "2", "3")
                print(strList[0] + strList[1])""");
        assertSuccessfulExitCode(results);
        assertEquals("12\n", results.stdout());
    }

    @Test
    @Order(180)
    public void additionOfIntElementsWorks() {
        var results = runText("""
                strList: list<int> = listOf(1, 2, 3)
                print(strList[0] + strList[1])""");
        assertSuccessfulExitCode(results);
        assertEquals("3\n", results.stdout());
    }

    @Test
    @Order(190)
    public void reassignFromDifferentListTypeWithCastThenAddWorks() {
        var results = runText("""
                strList: list<str> = listOf("1", "2", "3")
                intList: list<int> = listOf(1,2,3)
                intList = strList: list<int>
                print(intList[0] + intList[1])""");
        assertSuccessfulExitCode(results);
        assertEquals("3\n", results.stdout());
    }
}