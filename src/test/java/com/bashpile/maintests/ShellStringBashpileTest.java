package com.bashpile.maintests;

import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(50)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShellStringBashpileTest extends BashpileTest {

    /** Simple one word command */
    @Test @Order(10)
    public void runLsWorks() {
        final ExecutionResults results = runText("#(ls)");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertTrue(results.stdout().contains("pom.xml\n"));
    }

    /** Command with arguments */
    @Test @Order(20)
    public void runEchoWorks() {
        final ExecutionResults results = runText("#(echo hello command object)");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("hello command object\n", results.stdout());
    }

    @Test @Order(30)
    public void runInvalidCommandHadBadExitCode() {
        final ExecutionResults results = runText("#(invalid_command_example_for_testing)");
        assertFailedExitCode(results);
        assertCorrectFormatting(results);
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
                )""");
        assertCorrectFormatting(results);
        assertFailedExitCode(results);
    }

    @Test @Order(40)
    public void runEchoParenthesisWorks() {
        final ExecutionResults results = runPath(Path.of("runEchoParenthesis.bps"));
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("()\n", results.stdout());
    }

    @Test @Order(50)
    public void nestedShellStringsWork() {
        final ExecutionResults results = runText("#(cat #(src/test/resources/testdata.txt))");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("test\n", results.stdout());
    }

    @Test @Order(60)
    public void shellStringsWithHashWork() {
        final ExecutionResults results = runText("#(echo '#')");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("#\n", results.stdout());
    }

    @Test @Order(70)
    public void shellStringInCalcWorks() {
        final String bashpile = """
                print(1 + #(expr 1 + 1):int)
                """;
        final ExecutionResults results = runText(bashpile);
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("3\n", results.stdout());
    }

    @Test @Order(80)
    public void shellStringInCalcWithEscapesWorks() {
        final String bashpile = """
                print(#(printf "NCC-1701") + "\\n" + "\\n")
                """;
        final ExecutionResults results = runText(bashpile);
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("NCC-1701\n\n\n", results.stdout());
    }

    @Test @Order(90)
    public void shellStringWithEscapesInCalcWorks() {
        final String bashpile = """
                #(export IFS=$'\t')
                print(#(printf "NCC\\n1701"))
                """;
        final ExecutionResults results = runText(bashpile);
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("NCC\n1701\n", results.stdout());
    }

    @Test @Order(100)
    public void shellStringErrorExitCodesTriggerStrictModeTrap() {
        final ExecutionResults results = runText("""
                #(pwd)
                #(ls non_existent_file)""");
        assertFailedExitCode(results);
        assertCorrectFormatting(results);
        final List<String> lines = results.stdoutLines();
        assertTrue(lines.get(lines.size() - 1).contains("ls non_existent_file"));
    }
}
