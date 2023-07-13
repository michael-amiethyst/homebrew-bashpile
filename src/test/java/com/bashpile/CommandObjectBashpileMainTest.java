package com.bashpile;

import com.bashpile.commandline.ExecutionResults;
import com.bashpile.testhelper.BashpileMainTest;
import org.junit.jupiter.api.*;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(50)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommandObjectBashpileMainTest extends BashpileMainTest {
    @Nonnull
    @Override
    protected String getDirectoryName() {
        return "50-commandObjects";
    }

    @Test @Order(10)
    public void runEchoWorks() {
        final ExecutionResults results = runFile("0010-runEcho.bashpile");
        assertEquals("hello command object\n", results.stdout());
    }
}
