package com.bashpile;

public class BashpileUncheckedException extends RuntimeException {
    public BashpileUncheckedException() {
        super();
    }
    public BashpileUncheckedException(Exception e) {
        super(e);
    }
}
