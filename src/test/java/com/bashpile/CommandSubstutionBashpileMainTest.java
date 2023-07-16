package com.bashpile;

import com.bashpile.commandline.ExecutionResults;
import com.bashpile.testhelper.BashpileMainTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(60)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommandSubstutionBashpileMainTest extends BashpileMainTest {

    /** Simple one word command */
    @Test @Order(10)
    public void commandSubstitutionWorks() {
        final ExecutionResults results = runText("""
                fileContents: str = $(cat src/test/resources/testdata.txt)
                print(fileContents)""");
        assertEquals("test\n", results.stdout());
    }

    // TODO test #(echo $(cat src/test/resources/testdata.txt))

    // TODO test
    // #(export filename=src/test/resources/testdata.txt)
    // var: str = $(cat $(filename))

    // TODO test
    // #(export filename=src/test/resources/testdata.txt)
    // var: str = #($(cat $(filename)))

    // TODO test in calc
}
