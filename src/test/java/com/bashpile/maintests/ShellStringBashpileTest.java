package com.bashpile.maintests;

import com.bashpile.commandline.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Path;

import static com.bashpile.commandline.ExecutionResults.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

@Order(50)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShellStringBashpileTest extends BashpileTest {

    /** Simple one word command */
    @Test @Order(10)
    public void runLsWorks() {
        final ExecutionResults results = runText("#(ls)");
        assertTrue(results.stdout().contains("pom.xml\n"));
    }

    /** Command with arguments */
    @Test @Order(20)
    public void runEchoWorks() {
        final ExecutionResults results = runText("#(echo hello command object)");
        assertEquals("hello command object\n", results.stdout());
    }

    @Test @Order(30)
    public void runInvalidCommandHadBadExitCode() {
        final ExecutionResults results = runText("#(invalid_command_example_for_testing)");
        assertFailedExitCode(results);
    }

    @Test @Order(31)
    public void explicitErrorExitCodePropagates() {
        final ExecutionResults results = runText("""
                #(
                    if false; then
                        return 0
                    else
                        echo "Failed to create captainsLog.txt."
                        return 1
                    fi
                    exitCode=$?
                    if [ "$exitCode" -ne 0 ]; then exit "$exitCode"; fi
                    echo ${contents}
                )""");
        assertFailedExitCode(results);
    }

    @Test @Order(40)
    public void runEchoParenthesisWorks() {
        final ExecutionResults results = runPath(Path.of("runEchoParenthesis.bashpile"));
        assertSuccessfulExitCode(results);
        assertEquals("()\n", results.stdout());
    }

    @Test @Order(50)
    public void nestedShellStringsWork() {
        final ExecutionResults results = runText("#(cat #(src/test/resources/testdata.txt))");
        assertSuccessfulExitCode(results);
        assertEquals("test\n", results.stdout());
    }

    @Test @Order(60)
    public void shellStringsWithHashWork() {
        final ExecutionResults results = runText("#(echo '#')");
        assertSuccessfulExitCode(results);
        assertEquals("#\n", results.stdout());
    }

    @Test @Order(70)
    public void shellStringInCalcWorks() {
        final String bashpile = """
                print(1 + #(expr 1 + 1):int)
                """;
        assertEquals("3\n", runText(bashpile).stdout());
    }
}
