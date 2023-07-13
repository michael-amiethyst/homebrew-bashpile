package com.bashpile.exceptions;

import javax.annotation.Nonnull;

/** For mis-matched or incorrect types */
public class TypeError extends UserError {
    public TypeError(@Nonnull final String message, final int lineNumber) {
        super("Type mismatch on line %d -- %s".formatted(lineNumber, message));
    }
}
