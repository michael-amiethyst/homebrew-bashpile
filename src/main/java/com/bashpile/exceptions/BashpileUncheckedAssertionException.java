package com.bashpile.exceptions;

public class BashpileUncheckedAssertionException extends BashpileUncheckedException {
    public BashpileUncheckedAssertionException(String message) {
        super(message);
    }
    public BashpileUncheckedAssertionException(Throwable e) {
        super(e);
    }

    public BashpileUncheckedAssertionException(String message, Throwable e) {
        super(message, e);
    }
}
