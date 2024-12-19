package com.bashpile.exceptions;

/** Used with {@link Exceptions#asUncheckedSupplier(ThrowingSupplier)}. */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;
}
