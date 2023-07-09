package com.bashpile;

import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import com.bashpile.testhelper.BashpileMainTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.annotation.Nonnull;
import java.util.List;

import static com.bashpile.Asserts.assertExecutionSuccess;
import static org.apache.commons.lang3.StringUtils.join;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Order(30)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionBashpileMainTest extends BashpileMainTest {

    @Nonnull
    @Override
    protected String getDirectoryName() {
        return "30-functions";
    }

    @Test
    @Order(110)
    public void functionDeclarationTest() {
        List<String> executionResults = runFile("0110-functionDeclaration.bashpile").stdoutLines();
        assertEquals(2, executionResults.size(),
                "Unexpected line length, was:\n" + join(executionResults));
    }

    @Test
    @Order(111)
    public void functionDeclarationParamsTest() {
        List<String> executionResults = runFile("0111-functionDeclaration-params.bashpile").stdoutLines();
        assertEquals(2, executionResults.size());
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
        List<String> executionResults = runFile("0120-functionCall.bashpile").stdoutLines();
        assertEquals(2, executionResults.size());
        assertEquals("3.14", executionResults.get(0));
        assertEquals("3.14", executionResults.get(1));
    }

    @Test
    @Order(121)
    public void functionCallMultipleParamsTest() {
        var executionResults = runFile("0121-functionCall-multipleParams.bashpile");
        assertExecutionSuccess(executionResults);
        assertEquals(1, executionResults.stdoutLines().size());
        assertEquals("12", executionResults.stdoutLines().get(0));
    }

    @Test
    @Order(122)
    public void functionCallReturnStringTest() {
        var executionResults = runFile("0122-functionCall-returnString.bashpile");
        assertExecutionSuccess(executionResults);
        assertEquals(1, executionResults.stdoutLines().size());
        assertEquals("hello world", executionResults.stdoutLines().get(0));
    }

    @Test
    @Order(123)
    public void functionCallTagsTest() {
        var executionResults = runFile("0123-functionCall-tags.bashpile");
        assertExecutionSuccess(executionResults);
        assertEquals(2, executionResults.stdoutLines().size());
        assertEquals("3.14", executionResults.stdoutLines().get(0));
    }

    @Test
    @Order(130)
    public void functionForwardDeclarationTest() {
        String filename = "0130-functionForwardDecl.bashpile";
        var executionResults = runFile(filename);
        assertExecutionSuccess(executionResults);
        assertEquals(1, executionResults.stdoutLines().size()
                , "Wrong length, was: " + join(executionResults.stdoutLines()));
        assertEquals(1,
                executionResults.stdinLines().stream().filter(x -> x.startsWith("circleArea")).count(),
                "Wrong circleArea count");
        assertEquals("6.28", executionResults.stdoutLines().get(0), "Wrong return");
    }

    @Test
    @Order(140)
    public void stringTypeTest() {
        String filename = "0140-stringType.bashpile";
        var executionResults = runFile(filename);
        assertExecutionSuccess(executionResults);
        assertEquals(1, executionResults.stdoutLines().size()
                , "Wrong length, was: " + join(executionResults.stdoutLines()));
        assertEquals("to be wild", executionResults.stdoutLines().get(0),
                "Wrong return");
    }

    @Test
    @Order(150)
    public void functionDeclTypesTest() {
        List<String> executionResults = runFile("0150-functionDeclTypes.bashpile").stdoutLines();
        assertEquals(2, executionResults.size());
        assertEquals("3.14", executionResults.get(0));
        assertEquals("3.14", executionResults.get(1));
    }

    @Test
    @Order(160)
    public void functionDeclTypesEnforcedTest() {
        assertThrows(TypeError.class, () -> runFile("0160-functionDeclTypesEnforced.bashpile"));
    }

    @Test
    @Order(170)
    public void functionDeclTypesCalcExprTest() {
        var executionResults = runFile("0170-functionDeclTypesEnforced-calcExpr.bashpile");
        List<String> lines = executionResults.stdoutLines();
        assertExecutionSuccess(executionResults);
        assertEquals(1, lines.size(), "Wrong length, was: " + join(lines));
        assertEquals("3.14", lines.get(0));
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
}
