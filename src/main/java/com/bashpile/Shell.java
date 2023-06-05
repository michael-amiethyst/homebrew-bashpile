package com.bashpile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class Shell {

    private static final Pattern bogusScreenLine = Pattern.compile(
            "your \\d+x\\d+ screen size is bogus. expect trouble\r\n");

    private static final Logger log = LogManager.getLogger();

    public static String run(String bashText) throws IOException {
        log.info("Executing bash text:\n" + bashText);
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            log.trace("Detected windows");
            builder.command("wsl");
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
            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), stdoutWriter::println);
            Future<?> future = executorService.submit(streamGobbler);

            bufferedWriter.write("cd\n");
            bufferedWriter.write(bashText + "\n");
            bufferedWriter.write("exit\n");
            bufferedWriter.flush();

            exitCode = process.waitFor();

            future.get(10, TimeUnit.SECONDS);

            if (exitCode != 0) {
                throw new RuntimeException(Integer.toString(exitCode));
            }
            // return buffer stripped of random error lines
            log.trace("Shell output before processing: [{}]", stdout.toString());
            return bogusScreenLine.matcher(stdout.toString()).replaceAll("").trim();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new BashpileUncheckedException(e);
        }
    }
}
