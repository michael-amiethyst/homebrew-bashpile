package com.bashpile.exceptions;

import javax.annotation.Nonnull;

/** For assertion failures */
public class BashpileUncheckedAssertionException extends BashpileUncheckedException {
    public BashpileUncheckedAssertionException(@Nonnull final String message) {
        super(message);
    }
}
