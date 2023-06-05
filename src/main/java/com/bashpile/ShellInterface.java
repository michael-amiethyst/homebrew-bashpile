package com.bashpile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.concurrent.*;

public class ShellInterface {

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
        OutputStreamWriter stdoutWriter = new OutputStreamWriter(stdout);
        // TODO write to stdoutWriter
        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), System.out::println);
        int exitCode;
        try (
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                BufferedWriter bufferedWriter = process.outputWriter()) {
            Future<?> future = executorService.submit(streamGobbler);

            bufferedWriter.write("cd\n");
            bufferedWriter.write("ls\n");
            bufferedWriter.write("exit\n");
            bufferedWriter.flush();

            exitCode = process.waitFor();

            future.get(10, TimeUnit.SECONDS);
        }
        if (exitCode != 0) {
            throw new RuntimeException(Integer.toString(exitCode));
        }
        return "2"; // TODO stub
    }
}
