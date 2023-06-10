package com.bashpile;

public class BashpileUncheckedException extends RuntimeException {
    public BashpileUncheckedException(String message) {
        super(message);
    }
    public BashpileUncheckedException(Throwable e) {
        super(e);
    }

    public BashpileUncheckedException(Throwable e, String message) {
        super(message, e);
    }
}
