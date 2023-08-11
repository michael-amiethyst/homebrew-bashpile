package com.bashpile.shell;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.concurrent.*;

import static com.bashpile.StringUtils.appendIfMissing;

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
    private final Process childProcess;

    /** This runs our child process's STDOUT stream reader */
    private final ExecutorService executorService;

    /**
     * This pipes output from the parent process to the input of the child process.
     *
     * @see #writeLn(String)
     */
    private final BufferedWriter childStdInWriter;

    /** We need this reference to the buffer to get the final stdout contents */
    private final ByteArrayOutputStream childStdOutBuffer;

    /** This is a writer to the {@link #childStdOutBuffer} */
    private final PrintStream childStdOutWriter;

    /** We just need this to close down the STDOUT stream reader */
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
    public Pair<Integer, String> join() {
        flush();

        try {
            // join to threads
            final int exitCode = childProcess.waitFor();
            childStdOutReaderFuture.get(10, TimeUnit.SECONDS);

            return Pair.of(exitCode, childStdOutBuffer.toString());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new BashpileUncheckedException(e);
        }
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
                join();
            } catch (Exception e) {
                LOG.warn(e);
            }
        }

        // close stdin writer
        try {
            childStdInWriter.flush();
            childStdInWriter.close();
        } catch (Exception e) {
            LOG.warn(e);
        } finally {
            IOUtils.closeQuietly(childStdInWriter);
        }

        // close everything else
        try {
            // so many resources to deal with
            childStdOutBuffer.flush();
            childStdOutWriter.flush();
            executorService.close();
            childStdOutBuffer.close();
            childStdOutWriter.close();
        } catch (Exception e) {
            LOG.warn(e);
        } finally {
            IOUtils.closeQuietly(childStdOutBuffer, childStdOutWriter);
            executorService.close();  // executorService didn't qualify for closeQuietly
        }
    }
}
