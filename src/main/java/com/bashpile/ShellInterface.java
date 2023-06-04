package com.bashpile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShellInterface {

    private static final Logger log = LogManager.getLogger();

    public static String run(String bashText) {
        // TODO implement stub
        log.debug(System.getProperty("os.name"));
        return "2";
    }
}
