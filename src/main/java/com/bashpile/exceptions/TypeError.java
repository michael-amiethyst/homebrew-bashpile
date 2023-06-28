package com.bashpile.exceptions;

public class TypeError extends UserError {
    public TypeError(String message) {
        super(message);
    }

    public TypeError(Throwable e) {
        super(e);
    }

    public TypeError(String message, Throwable e) {
        super(message, e);
    }
}
