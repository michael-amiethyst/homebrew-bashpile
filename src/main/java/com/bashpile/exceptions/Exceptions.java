package com.bashpile.exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Utility class related to Exceptions and Throwables */
public class Exceptions {

    /**
     * Example usage: <code>asUnchecked(() -> Files.deleteIfExists(tempFile))</code>.
     *
     * @param throwingSupplier The lambda to run.  Return must be @Nonnull.
     * @param <T> The type the supplier returns.
     * @return The result of the lambda.
     */
    public static <T> @Nonnull T asUnchecked(final @Nonnull ThrowingSupplier<T, Exception> throwingSupplier) {
        try {
            return throwingSupplier.get();
        } catch (BashpileUncheckedAssertionException e) {
            throw e;
        } catch (Exception ex) {
            throw new BashpileUncheckedException(ex);
        }
    }

    /**
     * Like {@link #asUnchecked(ThrowingSupplier)} but ignores "Stream closed" exceptions.
     *
     * @param throwingSupplier The lambda to run.  Return must be @Nonnull.
     * @param <T> The type the supplier returns.
     * @return The result of the lambda.
     */
    public static <T> @Nullable T asUncheckedIgnoreClosedStreams(
            final @Nonnull ThrowingSupplier<T, Exception> throwingSupplier) {
        try {
            return throwingSupplier.get();
        } catch (Exception ex) {
            if (!ex.getMessage().contains("Stream closed")) {
                throw new BashpileUncheckedException(ex);
            } // else ignore
            return null;
        }
    }

    /**
     * Ignores "Stream closed" exceptions.
     *
     * @param throwingSupplier The lambda to run.  Return must be @Nonnull.
     * @param <T> The type the supplier returns.
     * @return The result of the lambda.
     * @throws Exception A generic exception
     */
    public static <T> @Nullable T ignoreClosedStreams(
            final @Nonnull ThrowingSupplier<T, Exception> throwingSupplier) throws Exception {
        try {
            return throwingSupplier.get();
        } catch (Exception ex) {
            if (!ex.getMessage().contains("Stream closed")) {
                throw ex;
            } // else ignore
            return null;
        }
    }
}
