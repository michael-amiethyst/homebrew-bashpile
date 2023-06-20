package com.bashpile;

import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.stream.Streams;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Applies consumer to each inputStream line.  Exceptions are hanled both in the stream and the consumer.
 * <br>
 * Adapted from <a href="https://www.baeldung.com/run-shell-command-in-java">a tutorial</a>.
 */
public class FailableStreamConsumer implements Runnable {
    private final InputStream inputStream;
    private final FailableConsumer<String, Exception> consumer;

    public FailableStreamConsumer(final InputStream inputStream, final FailableConsumer<String, Exception> consumer) {
        this.inputStream = inputStream;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        Streams.stream(bufferedReader.lines()).forEach(consumer);
    }
}
