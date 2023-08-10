package com.bashpile.exceptions;

import javax.annotation.Nonnull;

public class Exceptions {

    /**
     * Example usage: <code>asUnchecked(() -> Files.deleteIfExists(tempFile))</code>.
     *
     * @param throwingSupplier The lambda to run.
     * @param <T> The type the supplier returns.
     * @return The result of the lambda.
     */
    static public <T> T asUnchecked(final @Nonnull ThrowingSupplier<T, Exception> throwingSupplier) {
        try {
            return throwingSupplier.get();
        } catch (BashpileUncheckedAssertionException e) {
            throw e;
        } catch (Exception ex) {
            throw new BashpileUncheckedException(ex);
        }
    }
}
