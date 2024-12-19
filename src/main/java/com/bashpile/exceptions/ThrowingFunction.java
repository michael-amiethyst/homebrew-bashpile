package com.bashpile.exceptions;

@FunctionalInterface
public interface ThrowingFunction<E extends Exception> {
    void apply() throws E;
}
