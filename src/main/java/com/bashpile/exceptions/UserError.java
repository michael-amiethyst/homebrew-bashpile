package com.bashpile.exceptions;

public class UserError extends BashpileUncheckedException {
    public UserError(String message) {
        super(message);
    }

    public UserError(Throwable e) {
        super(e);
    }

    public UserError(String message, Throwable e) {
        super(message, e);
    }
}
