package com.bashpile;

public class BashpileUncheckedException extends RuntimeException {
    public BashpileUncheckedException(String message) {
        super(message);
    }
    public BashpileUncheckedException(Exception e) {
        super(e);
    }
}
