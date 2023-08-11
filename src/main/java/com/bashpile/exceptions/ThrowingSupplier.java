package com.bashpile.exceptions;

/** Used with {@link Exceptions#asUnchecked(ThrowingSupplier)}. */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;
}
