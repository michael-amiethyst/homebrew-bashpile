package com.bashpile.exceptions;

import javax.annotation.Nonnull;

/** Generic error indicating that the Bashpile end-user made a mistake */
public class UserError extends BashpileUncheckedException {
    public UserError(@Nonnull final String message) {
        super(message);
    }

    public UserError(@Nonnull final Throwable e) {
        super(e);
    }

    public UserError(@Nonnull final String message, @Nonnull final Throwable e) {
        super(message, e);
    }
}
