package com.bashpile.maintests;

import com.bashpile.BashpileMain;
import com.bashpile.commandline.ExecutionResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.nio.file.Path;

abstract public class BashpileTest {

    private static final Logger log = LogManager.getLogger(BashpileTest.class);

    protected @Nonnull ExecutionResults runText(@Nonnull final String bashText) {
        log.debug("Start of:\n{}", bashText);
        BashpileMain bashpile = new BashpileMain(bashText);
        return bashpile.execute();
    }

    protected @Nonnull ExecutionResults runPath(@Nonnull final Path file) {
        final Path filename = file.isAbsolute()
                ? file
                : Path.of("src/test/resources/scripts/" + file);
        log.debug("Start of {}", filename);
        final BashpileMain bashpile = new BashpileMain(filename);
        return bashpile.execute();
    }
}
