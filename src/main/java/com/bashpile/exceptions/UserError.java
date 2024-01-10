package com.bashpile.exceptions;

import javax.annotation.Nonnull;

/** Generic error indicating that the Bashpile end-user made a mistake */
public class UserError extends BashpileUncheckedException {
    protected UserError(@Nonnull final String message) {
        super(message);
    }

    public UserError(@Nonnull final String message, int lineNumber) {
        // convert lineNumber from 0 to 1 based
        super("Syntax error on line %d: %s".formatted(++lineNumber, message));
    }
}
