package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

    public static Pair<String, Integer> run(String bashText) throws IOException {
        return runHelper(bashText, true, true);
    }

    public static Pair<String, Integer> failableRunInPlace(String bashText) throws IOException {
        return runHelper(bashText, false, false);
    }

    public static Pair<String, Integer> failableRun(String bashText) throws IOException {
        return runHelper(bashText, false, true);
    }

    private static Pair<String, Integer> runHelper(
            String bashText, boolean throwOnBadExitCode, boolean cd) throws IOException {
        log.info("Executing bash text:\n" + bashText);
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows()) {
            log.trace("Detected windows");
            builder.command("wsl");
        } else {
            log.trace("Detected 'nix");
            builder.command("bash");
        }
        builder.directory(new File(System.getProperty("user.dir")))
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

            // on Windows 11 `set -e` causes an exit code of 1 unless we do a sub-shell
            bufferedWriter.write("bash\n");
            if (cd) {
                bufferedWriter.write("cd\n");
            }
            // all of this code to run bashText
            bufferedWriter.write(StringUtils.appendIfMissing(bashText, "\n"));

            // exit from sub shell and shell
            bufferedWriter.write("exit $?\n");
            bufferedWriter.write("exit $?\n");
            bufferedWriter.flush();

            exitCode = process.waitFor();

            future.get(10, TimeUnit.SECONDS);

            String stdoutString = stdout.toString();
            if (exitCode != 0 && throwOnBadExitCode) {
                throw new BashpileUncheckedException(
                        "Found failing (non-0) 'nix exit code: " + exitCode + ".  Full text results:\n" + stdoutString);
            }
            // return buffer stripped of random error lines
            log.trace("Shell output before processing: [{}]", stdoutString);
            String processedCommandResultText = bogusScreenLine.matcher(stdoutString).replaceAll("").trim();
            return Pair.of(processedCommandResultText, exitCode);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
    }
}
