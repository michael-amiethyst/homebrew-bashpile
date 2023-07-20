package com.bashpile.maintests;

import com.bashpile.commandline.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(60)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InlineBashpileTest extends BashpileTest {

    /** Simple one word command */
    @Test @Order(10)
    public void inlineWorks() {
        final ExecutionResults results = runText("""
                fileContents: str = $(cat src/test/resources/testdata.txt)
                print(fileContents)""");
        assertEquals("test\n", results.stdout());
    }

    @Test @Order(20)
    public void shellStringInlineWorks() {
        final ExecutionResults results = runText("""
                #(echo $(cat src/test/resources/testdata.txt))""");
        assertEquals("test\n", results.stdout());
    }

    @Test @Order(30)
    public void nestedInlineWorks() {
        final ExecutionResults results = runText("""
                #(export filename=src/test/resources/testdata.txt)
                contents: str = $(cat $(echo $filename))
                print(contents)""");
        assertEquals("test\n", results.stdout());
    }

    @Test @Order(40)
    public void nestedInlineInShellScriptWorks() {
        final ExecutionResults results = runText("""
                #(export filename=src/test/resources/testdata.txt)
                contents: str = #($(cat $(echo $filename)))
                print(contents)""");
        assertEquals("test\n", results.stdout());
    }

    @Test @Order(50)
    public void nestedInlineWithCalcWorks() {
        final ExecutionResults results = runText("""
                result: int = $(expr 2 - $(expr 3 + 4)) + 5
                print(result)""");
        assertEquals("0\n", results.stdout());
    }

    @Test @Order(60)
    public void nestedInlineWithCalcReassignmentWorks() {
        final ExecutionResults results = runText("""
                result: int = 5
                result = $(expr 2 - $(expr 3 + 4)) + 5
                print(result)""");
        assertEquals("0\n", results.stdout());
    }

    // TODO test all statements with nested subshells
}
