package com.bashpile.exceptions;

import javax.annotation.Nonnull;

public class TypeError extends UserError {
    public TypeError(@Nonnull final String message) {
        super(message);
    }

    public TypeError(@Nonnull final Throwable e) {
        super(e);
    }

    public TypeError(@Nonnull final String message, Throwable e) {
        super(message, e);
    }
}
