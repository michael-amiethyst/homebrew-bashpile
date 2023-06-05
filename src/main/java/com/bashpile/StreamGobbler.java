package com.bashpile;

import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.stream.Streams;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/** Adapted from https://www.baeldung.com/run-shell-command-in-java */
public class StreamGobbler implements Runnable {
    private final InputStream inputStream;
    private final FailableConsumer<String, Exception> consumer;

    public StreamGobbler(InputStream inputStream, FailableConsumer<String, Exception> consumer) {
        this.inputStream = inputStream;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        Streams.stream(new BufferedReader(new InputStreamReader(inputStream)).lines())
                .forEach(consumer);
    }
}
