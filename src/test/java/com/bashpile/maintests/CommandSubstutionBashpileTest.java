package com.bashpile.maintests;

import com.bashpile.commandline.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(60)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommandSubstutionBashpileTest extends BashpileTest {

    /** Simple one word command */
    @Test @Order(10)
    public void commandSubstitutionWorks() {
        final ExecutionResults results = runText("""
                fileContents: str = $(cat src/test/resources/testdata.txt)
                print(fileContents)""");
        assertEquals("test\n", results.stdout());
    }

    @Test @Order(20)
    public void shellStringCommandSubstitutionWorks() {
        final ExecutionResults results = runText("""
                #(echo $(cat src/test/resources/testdata.txt))""");
        assertEquals("test\n", results.stdout());
    }

    // TODO test
    // #(export filename=src/test/resources/testdata.txt)
    // var: str = $(cat $(filename))

    // TODO test
    // #(export filename=src/test/resources/testdata.txt)
    // var: str = #($(cat $(filename)))

    // TODO test in calc
}
