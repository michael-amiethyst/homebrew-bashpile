package com.bashpile.testhelper;

import com.bashpile.BashpileMain;
import com.bashpile.commandline.ExecutionResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.nio.file.Path;

abstract public class BashpileMainTest {

    private static final Logger log = LogManager.getLogger(BashpileMainTest.class);

    protected abstract @Nonnull String getDirectoryName();

    protected @Nonnull ExecutionResults runText(@Nonnull final String bashText) {
        log.debug("Start of {}", bashText);
        BashpileMain bashpile = new BashpileMain(bashText);
        return bashpile.execute();
    }

    protected @Nonnull ExecutionResults runPath(@Nonnull final Path file) {
        final Path filename = file.isAbsolute()
                ? file
                : Path.of("src/test/resources/%s/%s".formatted(getDirectoryName(), file));
        log.debug("Start of {}", filename);
        final BashpileMain bashpile = new BashpileMain(filename);
        return bashpile.execute();
    }

    @Deprecated
    protected @Nonnull ExecutionResults runFile(String file) {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/%s/%s".formatted(getDirectoryName(), file);
        BashpileMain bashpile = new BashpileMain(Path.of(filename));
        return bashpile.execute();
    }
}
