package com.bashpile.exceptions;

public class BashpileUncheckedAssertionException extends BashpileUncheckedException {
    public BashpileUncheckedAssertionException(final String message) {
        super(message);
    }
    public BashpileUncheckedAssertionException(final Throwable e) {
        super(e);
    }

    public BashpileUncheckedAssertionException(final String message, final Throwable e) {
        super(message, e);
    }
}
