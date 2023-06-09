package com.bashpile;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Runs commands in Bash.  Runs `wsl bash` in Windows.
 */
public class CommandLineExecutor {

    private static final Pattern bogusScreenLine = Pattern.compile(
            "your \\d+x\\d+ screen size is bogus. expect trouble\r\n");

    private static final Logger log = LogManager.getLogger(CommandLineExecutor.class);

    public static String run(String bashText) throws IOException {
        log.info("Executing bash text:\n" + bashText);
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            log.trace("Detected windows");
            builder.command("wsl", "bash");
        } else {
            log.trace("Detected 'nix");
            builder.command("bash");
        }
        builder.directory(new File(System.getProperty("user.home")))
                .redirectErrorStream(true);
        Process process = builder.start();

        int exitCode;
        try (   // so many closable resources
                // process related
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                BufferedWriter bufferedWriter = process.outputWriter();
                // to get the child process's STDOUT
                ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                PrintStream stdoutWriter = new PrintStream(stdout)) {
            FailableStreamConsumer failableStreamConsumer =
                    new FailableStreamConsumer(process.getInputStream(), stdoutWriter::println);
            Future<?> future = executorService.submit(failableStreamConsumer);

            bufferedWriter.write("cd\n");
            bufferedWriter.write(StringUtils.appendIfMissing(bashText, "\n"));
            bufferedWriter.write("exit\n");
            bufferedWriter.flush();

            exitCode = process.waitFor();

            future.get(10, TimeUnit.SECONDS);

            String stdoutString = stdout.toString();
            if (exitCode != 0) {
                throw new BashpileUncheckedException(
                        "Found failing (non-0) 'nix exit code: " + exitCode + ".  Full text results:\n" + stdoutString);
            }
            // return buffer stripped of random error lines
            log.trace("Shell output before processing: [{}]", stdoutString);
            return bogusScreenLine.matcher(stdoutString).replaceAll("").trim();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new BashpileUncheckedException(e);
        }
    }
}
