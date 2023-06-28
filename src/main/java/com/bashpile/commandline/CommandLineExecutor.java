package com.bashpile.commandline;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.concurrent.*;

/**
 * Handles I/O and closing resources on a running child {@link Process}.
 * Spawns an additional thread to consume (read) the child process's STDOUT.
 * <br>
 * Call order should be: {@link #create(Process)}, one or many calls to {@link #write(String)}, {@link #join()}, {@link #getStdOut()}.
 */
public class CommandLineExecutor implements Closeable {

    /** The wrapped child process */
    final private Process childProcess;

    /** This runs our child process's STDOUT stream reader */
    final private ExecutorService executorService;

    /**
     * This pipes output from the parent process to the input of the child process.
     *
     * @see #write(String)
     */
    final private BufferedWriter childStdInWriter;

    /** We need this reference to the buffer to get the final stdout contents */
    final private ByteArrayOutputStream childStdOutBuffer;

    /** This is a writer to the {@link #childStdOutBuffer} */
    final private PrintStream childStdOutWriter;

    /** We just need this to close down the STDOUT stream reader */
    final private Future<?> childStdOutReaderFuture;

    public static CommandLineExecutor create(final Process childProcess) {
        final ByteArrayOutputStream childStdOutBuffer = new ByteArrayOutputStream();
        // childProcess.outputWriter() is confusing -- it returns a writer for the child process's STDIN
        return new CommandLineExecutor(childProcess, Executors.newSingleThreadExecutor(), childProcess.outputWriter(),
                childStdOutBuffer, new PrintStream(childStdOutBuffer));
    }

    private CommandLineExecutor(Process childProcess, ExecutorService executorService, BufferedWriter childStdInWriter,
                                ByteArrayOutputStream childStdOutBuffer, PrintStream childStdOutWriter) {
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

    public void write(String text) throws IOException {
        childStdInWriter.write(text);
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

    public String getStdOut() {
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
