package com.bashpile.exceptions;

import javax.annotation.Nonnull;

/** Base class of our application specific exceptions */
public class BashpileUncheckedException extends RuntimeException {
    public BashpileUncheckedException(@Nonnull final String message) {
        super(message);
    }

    public BashpileUncheckedException(@Nonnull final Throwable e) {
        super(e);
    }

    public BashpileUncheckedException(@Nonnull final String message, @Nonnull final Throwable e) {
        super(message, e);
    }
}
