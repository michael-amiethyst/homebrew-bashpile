package com.bashpile;

import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.join;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Order(20)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionsTest {

    private static final Logger log = LogManager.getLogger(FunctionsTest.class);

    @Test
    @Order(110)
    public void functionDeclarationTest() {
        String[] executionResults = runFile("0110-functionDeclaration.bashpile").stdoutLines();
        assertEquals(2, executionResults.length,
                "Unexpected line length, was:\n" + join(executionResults));
    }

    @Test
    @Order(111)
    public void functionDeclarationParamsTest() {
        String[] executionResults = runFile("0111-functionDeclaration-params.bashpile").stdoutLines();
        assertEquals(2, executionResults.length);
    }

    @Test
    @Order(112)
    public void functionDeclarationBadParamsTest() {
        assertThrows(TypeError.class, () -> runFile("0112-functionDeclaration-badParams.bashpile"));
    }

    @Test
    @Order(113)
    public void functionDeclarationDoubleDeclTest() {
        assertThrows(UserError.class, () -> runFile("0113-functionDeclaration-doubleDecl.bashpile"));
    }

    @Test
    @Order(120)
    public void functionCallTest() {
        String[] executionResults = runFile("0120-functionCall.bashpile").stdoutLines();
        assertEquals(2, executionResults.length);
        assertEquals("3.14", executionResults[0]);
        assertEquals("3.14", executionResults[1]);
    }

    @Test
    @Order(121)
    public void functionCallMultipleParamsTest() {
        var executionResults = runFile("0121-functionCall-multipleParams.bashpile");
        assertEquals(0, executionResults.exitCode());
        assertEquals(1, executionResults.stdoutLines().length);
        assertEquals("12", executionResults.stdoutLines()[0]);
    }

    @Test
    @Order(122)
    public void functionCallReturnStringTest() {
        var executionResults = runFile("0122-functionCall-returnString.bashpile");
        assertEquals(0, executionResults.exitCode());
        assertEquals(1, executionResults.stdoutLines().length);
        assertEquals("hello world", executionResults.stdoutLines()[0]);
    }

    @Test
    @Order(123)
    public void functionCallTagsTest() {
        var executionResults = runFile("0123-functionCall-tags.bashpile");
        assertEquals(0, executionResults.exitCode());
        assertEquals(2, executionResults.stdoutLines().length);
        assertEquals("3.14", executionResults.stdoutLines()[0]);
    }

    @Test
    @Order(130)
    public void functionForwardDeclarationTest() {
        String filename = "0130-functionForwardDecl.bashpile";
        var executionResults = runFile(filename);
        assertEquals(0, executionResults.exitCode(), "Bad exit code");
        assertEquals(1, executionResults.stdoutLines().length
                , "Wrong length, was: " + join(executionResults.stdoutLines()));
        assertEquals(1,
                Arrays.stream(executionResults.stdinLines()).filter(x -> x.startsWith("circleArea")).count(),
                "Wrong circleArea count");
        assertEquals("6.28", executionResults.stdoutLines()[0], "Wrong return");
    }

    @Test
    @Order(140)
    public void stringTypeTest() {
        String filename = "0140-stringType.bashpile";
        var executionResults = runFile(filename);
        assertEquals(0, executionResults.exitCode(), "Bad exit code");
        assertEquals(1, executionResults.stdoutLines().length
                , "Wrong length, was: " + join(executionResults.stdoutLines()));
        assertEquals("to be wild", executionResults.stdoutLines()[0],
                "Wrong return");
    }

    @Test
    @Order(150)
    public void functionDeclTypesTest() {
        String[] executionResults = runFile("0150-functionDeclTypes.bashpile").stdoutLines();
        assertEquals(2, executionResults.length);
        assertEquals("3.14", executionResults[0]);
        assertEquals("3.14", executionResults[1]);
    }

    @Test
    @Order(160)
    public void functionDeclTypesEnforcedTest() {
        assertThrows(TypeError.class, () -> runFile("0160-functionDeclTypesEnforced.bashpile"));
    }

    @Test
    @Order(170)
    public void functionDeclTypesCalcExprTest() {
        var results = runFile("0170-functionDeclTypesEnforced-calcExpr.bashpile");
        String[] lines = results.stdoutLines();
        assertEquals(0, results.exitCode());
        assertEquals(1, lines.length, "Wrong length, was: " + join(lines));
        assertEquals("3.14", lines[0]);
    }

    @Test
    @Order(180)
    public void functionDeclTypesBadCalcExprTest() {
        assertThrows(UserError.class, () -> runFile("0180-functionDeclTypesEnforced-badCalcExpr.bashpile"));
    }

    @Test
    @Order(190)
    public void functionDeclTypesBadCalcExprNestedTest() {
        assertThrows(TypeError.class, () -> runFile("0190-functionDeclTypesEnforced-badCalcExprNested.bashpile"));
    }

    // helpers

    private ExecutionResults runFile(String file) {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/20-functions/%s".formatted(file);
        BashpileMain bashpile = new BashpileMain(filename);
        return bashpile.execute();
    }
}
