package com.bashpile.exceptions;

import javax.annotation.Nonnull;

public class BashpileUncheckedAssertionException extends BashpileUncheckedException {
    public BashpileUncheckedAssertionException(@Nonnull final String message) {
        super(message);
    }
    public BashpileUncheckedAssertionException(@Nonnull final Throwable e) {
        super(e);
    }

    public BashpileUncheckedAssertionException(@Nonnull final String message, @Nonnull final Throwable e) {
        super(message, e);
    }
}
