package com.bashpile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class ShellInterface {

    private static final Pattern bogusScreenLine = Pattern.compile(
            "your \\d+x\\d+ screen size is bogus. expect trouble\r\n");

    private static final Logger log = LogManager.getLogger();

    public static String run(String bashText) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("wsl");
        } else {
            builder.command("sh", "-c", "ls");
        }
        builder.directory(new File(System.getProperty("user.home")))
                .redirectErrorStream(true);
        Process process = builder.start();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream stdoutWriter = new PrintStream(stdout);
        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), stdoutWriter::println);
        int exitCode;
        try (
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                BufferedWriter bufferedWriter = process.outputWriter()) {
            Future<?> future = executorService.submit(streamGobbler);

            bufferedWriter.write("cd\n");
            bufferedWriter.write(bashText + "\n");
            bufferedWriter.write("exit\n");
            bufferedWriter.flush();

            exitCode = process.waitFor();

            future.get(10, TimeUnit.SECONDS);
        }
        if (exitCode != 0) {
            throw new RuntimeException(Integer.toString(exitCode));
        }
        // return buffer stripped of random error lines
        return bogusScreenLine.matcher(stdout.toString()).replaceAll("");
    }
}
