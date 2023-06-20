package com.bashpile.exceptions;

public class BashpileUncheckedException extends RuntimeException {
    public BashpileUncheckedException(final String message) {
        super(message);
    }
    public BashpileUncheckedException(final Throwable e) {
        super(e);
    }

    public BashpileUncheckedException(final String message, final Throwable e) {
        super(message, e);
    }
}
