package com.bashpile.exceptions;

public class BashpileUncheckedException extends RuntimeException {
    public BashpileUncheckedException(String message) {
        super(message);
    }
    public BashpileUncheckedException(Throwable e) {
        super(e);
    }

    public BashpileUncheckedException(String message, Throwable e) {
        super(message, e);
    }
}
