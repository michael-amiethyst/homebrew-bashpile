package com.bashpile.exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/** Utility class related to Exceptions and Throwables */
public class Exceptions {

    /**
     * Example usage: <code>asUnchecked(() -> Files.deleteIfExists(tempFile))</code>.
     *
     * @param throwingSupplier The lambda to run.  Return must be @Nonnull.
     * @param <T> The type the supplier returns.
     * @return The result of the lambda.
     */
    static public <T> @Nonnull T asUnchecked(final @Nonnull ThrowingSupplier<T, Exception> throwingSupplier) {
        try {
            return throwingSupplier.get();
        } catch (BashpileUncheckedAssertionException e) {
            throw e;
        } catch (Exception ex) {
            throw new BashpileUncheckedException(ex);
        }
    }

    static public <T> @Nullable T asUncheckedIgnoreClosedStreams(final @Nonnull ThrowingSupplier<T, Exception> throwingSupplier) {
        try {
            return throwingSupplier.get();
        } catch (Exception ex) {
            if (!(ex instanceof IOException) || !ex.getMessage().equalsIgnoreCase("stream closed")) {
                throw new BashpileUncheckedException(ex);
            } // else ignore
            return null;
        }
    }
}
