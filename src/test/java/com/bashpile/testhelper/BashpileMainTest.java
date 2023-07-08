package com.bashpile.testhelper;

import com.bashpile.BashpileMain;
import com.bashpile.ExecutionResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

abstract public class BashpileMainTest {

    private static final Logger log = LogManager.getLogger(BashpileMainTest.class);

    protected abstract @Nonnull String getDirectoryName();

    protected @Nonnull ExecutionResults runFile(String file) {
        log.debug("Start of {}", file);
        String filename = "src/test/resources/%s/%s".formatted(getDirectoryName(), file);
        BashpileMain bashpile = new BashpileMain(filename);
        return bashpile.execute();
    }
}
