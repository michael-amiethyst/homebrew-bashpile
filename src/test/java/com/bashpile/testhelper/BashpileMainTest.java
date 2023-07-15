package com.bashpile.testhelper;

import com.bashpile.BashpileMain;
import com.bashpile.commandline.ExecutionResults;
import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

abstract public class BashpileMainTest {

    private static final Logger log = LogManager.getLogger(BashpileMainTest.class);

    protected abstract @Nonnull String getDirectoryName();

    protected @Nonnull ExecutionResults runText(@Nonnull final String bashText) {
        try {
            final Path temp = Files.createTempFile("", ".bashpile");
            Files.writeString(temp, bashText);
            return runFile(temp.toAbsolutePath());
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    protected @Nonnull ExecutionResults runFile(@Nonnull final Path file) {
        final String filename = file.isAbsolute()
                ? file.toString()
                : "src/test/resources/%s/%s".formatted(getDirectoryName(), file);
        return runLiteralFile(filename);
    }

    protected @Nonnull ExecutionResults runLiteralFile(@Nonnull final String filename) {
        log.debug("Start of {}", filename);
        final BashpileMain bashpile = new BashpileMain(filename);
        return bashpile.execute();
    }

    @Deprecated
    protected @Nonnull ExecutionResults runFile(String file) {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/%s/%s".formatted(getDirectoryName(), file);
        BashpileMain bashpile = new BashpileMain(filename);
        return bashpile.execute();
    }
}
