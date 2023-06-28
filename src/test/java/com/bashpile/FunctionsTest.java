package com.bashpile;

import com.bashpile.exceptions.TypeError;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.join;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionsTest {

    public static final Pattern lines = Pattern.compile("\r?\n");

    private static final Logger log = LogManager.getLogger(FunctionsTest.class);

    @Test
    @Order(110)
    public void functionDeclarationTest() {
        String[] executionResults = runFile("0110-functionDeclaration.bashpile").getLeft();
        assertEquals(2, executionResults.length,
                "Unexpected line length, was:\n" + join(executionResults));
    }

    @Test
    @Order(111)
    public void functionDeclarationParamsTest() {
        String[] executionResults = runFile("0111-functionDeclaration-params.bashpile").getLeft();
        assertEquals(2, executionResults.length);
    }

    @Test
    @Order(120)
    public void functionCallTest() {
        String[] executionResults = runFile("0120-functionCall.bashpile").getLeft();
        assertEquals(2, executionResults.length);
        assertEquals("3.14", executionResults[0]);
        assertEquals("3.14", executionResults[1]);
    }

    @Test
    @Order(121)
    public void functionCallMultipleParamsTest() {
        Pair<String[], Integer> executionResults = runFile("0121-functionCall-multipleParams.bashpile");
        assertEquals(0, executionResults.getRight());
        assertEquals(1, executionResults.getLeft().length);
        assertEquals("12", executionResults.getLeft()[0]);
    }

    @Test
    @Order(122)
    public void functionCallReturnStringTest() {
        Pair<String[], Integer> executionResults = runFile("0122-functionCall-returnString.bashpile");
        assertEquals(0, executionResults.getRight());
        assertEquals(1, executionResults.getLeft().length);
        assertEquals("hello world", executionResults.getLeft()[0]);
    }

    @Test
    @Order(123)
    public void functionCallTagsTest() {
        Pair<String[], Integer> executionResults = runFile("0123-functionCall-tags.bashpile");
        assertEquals(0, executionResults.getRight());
        assertEquals(2, executionResults.getLeft().length);
        assertEquals("3.14", executionResults.getLeft()[0]);
    }

    @Test
    @Order(130)
    public void functionForwardDeclarationTest() throws IOException {
        String filename = "0130-functionForwardDecl.bashpile";
        String[] bashLines = transpileFile(filename);
        Pair<String[], Integer> executionResults = runFile(filename);
        assertEquals(0, executionResults.getRight(), "Bad exit code");
        assertEquals(1, executionResults.getLeft().length
                , "Wrong length, was: " + join(executionResults.getLeft()));
        assertEquals(1, Arrays.stream(
                bashLines).filter(x -> x.startsWith("circleArea")).count(),
                "Wrong circleArea count");
        assertEquals("6.28", executionResults.getLeft()[0], "Wrong return");
    }

    @Test
    @Order(140)
    public void stringTypeTest() {
        String filename = "0140-stringType.bashpile";
        Pair<String[], Integer> executionResults = runFile(filename);
        assertEquals(0, executionResults.getRight(), "Bad exit code");
        assertEquals(1, executionResults.getLeft().length
                , "Wrong length, was: " + join(executionResults.getLeft()));
        assertEquals("to be wild", executionResults.getLeft()[0],
                "Wrong return");
    }

    @Test
    @Order(150)
    public void functionDeclTypesTest() {
        String[] executionResults = runFile("0150-functionDeclTypes.bashpile").getLeft();
        assertEquals(2, executionResults.length);
        assertEquals("3.14", executionResults[0]);
        assertEquals("3.14", executionResults[1]);
    }

    @Test
    @Order(160)
    public void functionDeclTypesEnforcedTest() {
        assertThrows(TypeError.class, () -> runFile("0160-functionDeclTypesEnforced.bashpile"));
    }

    // helpers

    /**
     * Compiles the file into the target shell language.
     *
     * @param file The Bashpile file.
     * @return An array of strings where each string is a compiled line of the target language (e.g. Bash5).
     * @throws IOException on file read error.
     */
    private String[] transpileFile(String file) throws IOException {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/functions/%s".formatted(file);
        BashpileMain bashpile = new BashpileMain(filename);
        return lines.split(bashpile.transpile());
    }

    private Pair<String[], Integer> runFile(String file) {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/functions/%s".formatted(file);
        BashpileMain bashpile = new BashpileMain(filename);
        Pair<String, Integer> runResults = bashpile.execute();
        return Pair.of(lines.split(runResults.getLeft()), runResults.getRight());
    }
}
