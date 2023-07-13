package com.bashpile.exceptions;

import javax.annotation.Nonnull;

/** Generic error indicating that the Bashpile end-user made a mistake */
public class UserError extends BashpileUncheckedException {
    protected UserError(@Nonnull final String message) {
        super(message);
    }

    public UserError(@Nonnull final String message, final int lineNumber) {
        super("Syntax error on line %d: %s".formatted(lineNumber, message));
    }
}
