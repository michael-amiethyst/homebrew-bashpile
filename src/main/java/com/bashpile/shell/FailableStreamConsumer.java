package com.bashpile.shell;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.stream.Streams;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Applies consumer to each inputStream line.  Exceptions are handled both in the stream and the consumer.
 * <br>
 * Adapted from <a href="https://www.baeldung.com/run-shell-command-in-java">a tutorial</a>.
 */
/* package */ class FailableStreamConsumer implements Runnable {

    private final InputStream inputStream;

    private final FailableConsumer<String, Exception> consumer;

    public FailableStreamConsumer(@Nonnull final InputStream inputStream,
                                  @Nonnull final FailableConsumer<String, Exception> consumer) {
        this.inputStream = inputStream;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            Streams.stream(bufferedReader.lines()).forEach(consumer);
        } catch (Exception e) {
            throw new BashpileUncheckedException(e);
        }
    }
}
