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

@Order(40)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionBashpileMainTest extends BashpileMainTest {

    @Nonnull
    @Override
    protected String getDirectoryName() {
        return "40-functions";
    }

    @Test
    @Order(10)
    public void functionDeclarationWorks() {
        List<String> executionResults = runFile("0010-functionDeclaration.bashpile").stdoutLines();
        assertEquals(2, executionResults.size(),
                "Unexpected line length, was:\n" + join(executionResults));
    }

    @Test
    @Order(20)
    public void functionDeclarationParamsWork() {
        List<String> executionResults = runFile("0020-functionDeclaration-params.bashpile").stdoutLines();
        assertEquals(2, executionResults.size());
    }

    @Test
    @Order(30)
    public void functionDeclarationBadParamsThrows() {
        assertThrows(TypeError.class, () -> runFile("0030-functionDeclaration-badParams.bashpile"));
    }

    @Test
    @Order(40)
    public void functionDeclarationDoubleDeclThrows() {
        assertThrows(UserError.class, () -> runFile("0040-functionDeclaration-doubleDecl.bashpile"));
    }

    @Test
    @Order(50)
    public void functionCallWorks() {
        List<String> executionResults = runFile("0050-functionCall.bashpile").stdoutLines();
        assertEquals(2, executionResults.size());
        assertEquals("3.14", executionResults.get(0));
        assertEquals("3.14", executionResults.get(1));
    }

    @Test
    @Order(60)
    public void functionCallMultipleParamsWorks() {
        var executionResults = runFile("0060-functionCall-multipleParams.bashpile");
        assertExecutionSuccess(executionResults);
        assertEquals(1, executionResults.stdoutLines().size());
        assertEquals("12", executionResults.stdoutLines().get(0));
    }

    @Test
    @Order(70)
    public void functionCallReturnStringWorks() {
        var executionResults = runFile("0070-functionCall-returnString.bashpile");
        assertExecutionSuccess(executionResults);
        assertEquals(1, executionResults.stdoutLines().size());
        assertEquals("hello world", executionResults.stdoutLines().get(0));
    }

    @Test
    @Order(71)
    public void functionCallIgnoreReturnStringWorks() {
        var executionResults = runFile("0071-functionCall-ignoreReturnString.bashpile");
        assertExecutionSuccess(executionResults);
        assertEquals("", executionResults.stdout());
    }

    @Test
    @Order(80)
    public void functionCallTagsWork() {
        var executionResults = runFile("0080-functionCall-tags.bashpile");
        assertExecutionSuccess(executionResults);
        assertEquals(2, executionResults.stdoutLines().size());
        assertEquals("3.14", executionResults.stdoutLines().get(0));
    }

    @Test
    @Order(90)
    public void functionCallReturnStringBadTypeThrows() {
        assertThrows(TypeError.class, () -> runFile("0090-functionCall-returnStringBadType.bashpile"));
    }

    @Test
    @Order(100)
    public void functionCallReturnEmptyBadTypeThrows() {
        assertThrows(TypeError.class, () -> runFile("0100-functionCall-returnEmptyBadType.bashpile"));
    }

    @Test
    @Order(130)
    public void functionForwardDeclarationWorks() {
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
    public void stringTypeWorks() {
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
    public void functionDeclTypesWork() {
        List<String> executionResults = runFile("0150-functionDeclTypes.bashpile").stdoutLines();
        assertEquals(2, executionResults.size());
        assertEquals("3.14", executionResults.get(0));
        assertEquals("3.14", executionResults.get(1));
    }

    @Test
    @Order(160)
    public void badFunctionDeclTypesThrow() {
        assertThrows(TypeError.class, () -> runFile("0160-functionDeclTypesEnforced.bashpile"));
    }

    @Test
    @Order(170)
    public void functionDeclTypesCalcExpressionsWork() {
        var executionResults = runFile("0170-functionDeclTypesEnforced-calcExpr.bashpile");
        List<String> lines = executionResults.stdoutLines();
        assertExecutionSuccess(executionResults);
        assertEquals(1, lines.size(), "Wrong length, was: " + join(lines));
        assertEquals("3.14", lines.get(0));
    }

    @Test
    @Order(180)
    public void functionDeclTypesBadCalcExpressionThrows() {
        assertThrows(UserError.class, () -> runFile("0180-functionDeclTypesEnforced-badCalcExpr.bashpile"));
    }

    @Test
    @Order(190)
    public void functionDeclTypesBadCalcExpressionNestedThrows() {
        assertThrows(TypeError.class, () -> runFile("0190-functionDeclTypesEnforced-badCalcExprNested.bashpile"));
    }
}
