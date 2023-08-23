package com.bashpile.shell;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.bashpile.Strings.appendIfMissing;
import static com.bashpile.exceptions.Exceptions.*;

/**
 * Handles I/O and closing resources on a running child {@link Process}.<br>
 * Spawns an additional thread to consume (read) the child process's STDOUT.<br>
 * <br>
 * Create with {@link #of(Process)}.  Write to the process's STDIN with {@link #writeLn(String)} and get results with
 * {@link #join()}.
 */
/* package */ class IoManager implements Closeable {

    private final static Logger LOG = LogManager.getLogger(IoManager.class);

    /** The wrapped child process */
    @Nonnull
    private final Process childProcess;

    /** This runs our child process's STDOUT stream reader */
    @Nonnull
    private final ExecutorService executorService;

    /**
     * This pipes output from the parent process to the input of the child process.
     *
     * @see #writeLn(String)
     */
    @Nonnull
    private final BufferedWriter childStdInWriter;

    /** We need this reference to the buffer to get the final stdout contents */
    @Nonnull
    private final ByteArrayOutputStream childStdOutBuffer;

    /** This is a writer to the {@link #childStdOutBuffer} */
    @Nonnull
    private final PrintStream childStdOutWriter;

    /** We just need this to close down the STDOUT stream reader */
    @Nonnull
    private final Future<?> childStdOutReaderFuture;

    public static @Nonnull IoManager of(@Nonnull final Process childProcess) {
        final ByteArrayOutputStream childStdOutBuffer = new ByteArrayOutputStream();
        // childProcess.outputWriter() is confusing -- it returns a writer for the child process's STDIN
        return new IoManager(childProcess, Executors.newSingleThreadExecutor(), childProcess.outputWriter(),
                childStdOutBuffer, new PrintStream(childStdOutBuffer));
    }

    private IoManager(@Nonnull final Process childProcess,
                      @Nonnull final ExecutorService executorService,
                      @Nonnull final BufferedWriter childStdInWriter,
                      @Nonnull final ByteArrayOutputStream childStdOutBuffer,
                      @Nonnull final PrintStream childStdOutWriter) {
        this.childProcess = childProcess;
        this.executorService = executorService;
        this.childStdInWriter = childStdInWriter;
        this.childStdOutBuffer = childStdOutBuffer;
        this.childStdOutWriter = childStdOutWriter;
        // childProcess.getInputStream() actually returns the STDOUT of the child process
        final FailableStreamConsumer failableStreamConsumer =
                new FailableStreamConsumer(childProcess.getInputStream(), childStdOutWriter::println);
        this.childStdOutReaderFuture = executorService.submit(failableStreamConsumer);
    }

    public void writeLn(@Nonnull final String text) throws IOException {
        final String paragraph = appendIfMissing(text, "\n");
        childStdInWriter.write(paragraph);
    }

    /** Sends the Termination Linux Signal (15) to our async process */
    public void sigterm() {
        // destroy sends SIGTERM
        childProcess.destroy();
    }

    /** Joins to both background threads (process and STDOUT stream reader) */
    public @Nonnull Pair<Integer, String> join() {
        flush();

        // join to threads
        final int exitCode = asUnchecked(childProcess::waitFor);
        var ignored = asUncheckedIgnoreClosedStreams(() -> childStdOutReaderFuture.get(10, TimeUnit.SECONDS));

        return Pair.of(exitCode, childStdOutBuffer.toString());
    }

    private void flush() {
        try {
            childStdInWriter.flush();
        } catch (IOException e) {
            // ignore
        }

        try {
            childStdOutBuffer.flush();
        } catch (IOException e) {
            // ignore
        }

        childStdOutWriter.flush();
    }

    @Override
    public void close() {
        // shut down the background threads
        if (childProcess.isAlive()) {
            try {
                var ignored = ignoreClosedStreams(this::join);
            } catch (Exception e) {
                LOG.warn(e);
            }
        }

        // close stdin writer
        try {
            ignoreClosedStreams(() -> { childStdInWriter.flush(); return ""; });
            ignoreClosedStreams(() -> { childStdInWriter.close(); return ""; });
        } catch (Exception e) {
            LOG.warn(e);
        } finally {
            IOUtils.closeQuietly(childStdInWriter);
        }

        // close everything else
        try {
            // so many resources to deal with
            ignoreClosedStreams(() -> { childStdOutBuffer.flush(); return ""; });
            ignoreClosedStreams(() -> { childStdOutWriter.flush(); return ""; });

            ignoreClosedStreams(() -> { executorService.close(); return ""; });
            ignoreClosedStreams(() -> { childStdOutBuffer.close(); return ""; });
            ignoreClosedStreams(() -> { childStdOutWriter.close(); return ""; });
        } catch (Exception e) {
            LOG.warn(e);
        } finally {
            IOUtils.closeQuietly(childStdOutBuffer, childStdOutWriter);
            executorService.close();  // executorService didn't qualify for closeQuietly
        }
    }
}
