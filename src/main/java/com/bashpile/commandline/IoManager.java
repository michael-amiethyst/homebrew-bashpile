package com.bashpile.commandline;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.concurrent.*;

import static org.apache.commons.lang3.StringUtils.appendIfMissing;

/**
 * Handles I/O and closing resources on a running child {@link Process}.<br>
 * Spawns an additional thread to consume (read) the child process's STDOUT.<br>
 * <br>
 * Call order should be: {@link #spawnConsumer(Process)}, one or many calls to {@link #writeLn(String)}, {@link #join()}, {@link #getStdOut()}.
 */
public class IoManager implements Closeable {

    /** The wrapped child process */
    final private Process childProcess;

    /** This runs our child process's STDOUT stream reader */
    final private ExecutorService executorService;

    /**
     * This pipes output from the parent process to the input of the child process.
     *
     * @see #writeLn(String)
     */
    final private BufferedWriter childStdInWriter;

    /** We need this reference to the buffer to get the final stdout contents */
    final private ByteArrayOutputStream childStdOutBuffer;

    /** This is a writer to the {@link #childStdOutBuffer} */
    final private PrintStream childStdOutWriter;

    /** We just need this to close down the STDOUT stream reader */
    final private Future<?> childStdOutReaderFuture;

    public static @Nonnull IoManager spawnConsumer(@Nonnull final Process childProcess) {
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
        final String textBlock = appendIfMissing(text, "\n");
        childStdInWriter.write(textBlock);
    }

    /** Joins to both background threads (process and STDOUT stream reader) */
    public int join() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        flush();
        final int ret = childProcess.waitFor();
        childStdOutReaderFuture.get(10, TimeUnit.SECONDS);
        return ret;
    }

    private void flush() throws IOException {
        childStdInWriter.flush();
        childStdOutBuffer.flush();
        childStdOutWriter.flush();
    }

    public @Nonnull String getStdOut() {
        if (childProcess.isAlive()) {
            try {
                join();
            } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
                throw new BashpileUncheckedException(e);
            }
        }
        return childStdOutBuffer.toString();
    }

    @Override
    public void close() throws IOException {
        try {
            // so many resources to deal with
            executorService.close();
            childStdInWriter.flush();
            childStdInWriter.close();
            childStdOutBuffer.flush();
            childStdOutBuffer.close();
            childStdOutWriter.flush();
            childStdOutWriter.close();
        } finally {
            IOUtils.closeQuietly(childStdInWriter, childStdOutBuffer, childStdOutWriter);
            executorService.close();  // executorService didn't qualify for closeQuietly
        }
    }
}
