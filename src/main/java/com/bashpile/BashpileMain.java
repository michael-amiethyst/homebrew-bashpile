package com.bashpile;

import com.bashpile.commandline.BashExecutor;
import com.bashpile.commandline.ExecutionResults;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.UserError;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static com.bashpile.AntlrUtils.parse;

/** Entry point into the program */
@CommandLine.Command(
        name = "bashpile",
        description = "Converts Bashpile lines to Bash"
)
public class BashpileMain implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(BashpileMain.class);

    public static void main(final String[] args) {
        final BashpileMain bashpile = new BashpileMain();
        final CommandLine argProcessor = new CommandLine(bashpile);
        bashpile.setCommandLine(argProcessor);
        System.exit(argProcessor.execute(args));
    }

    @CommandLine.Option(names = {"-i", "--inputFile"},
            description = "Use the specified bashpile file.  Has precedence over the -c option.")
    @Nullable
    private Path inputFile;

    @CommandLine.Option(names = {"-c", "--command"},
            description = "Use the specified text.  -i option has precedence if both are specified.")
    @Nullable
    private String command;

    private CommandLine commandLine;

    public BashpileMain() {}

    public BashpileMain(@Nullable final Path inputFile) {
        this.inputFile = inputFile;
    }

    public BashpileMain(@Nullable final String command) {
        this.command = command;
    }

    @Override
    public @Nonnull Integer call() {
        // prints help text and returns 'general error'
        commandLine.usage(System.out);
        return 1;
    }

    /** Called by the picocli framework */
    @SuppressWarnings("unused")
    @CommandLine.Command(name = "execute", description = "Converts Bashpile lines to bash and executes them")
    public int executeCommand() {
        System.out.println(execute().stdout());
        return 0;
    }

    public @Nonnull ExecutionResults execute() {
        log.debug("In {}", System.getProperty("user.dir"));
        String bashScript = null;
        try {
            bashScript = transpile();
            return BashExecutor.run(bashScript);
        } catch (UserError | AssertionError e) {
            throw e;
        } catch (Throwable e) {
            String msg = bashScript != null ? "\nCouldn't run `%s`".formatted(bashScript) : "\nCouldn't parse input";
            if (e.getMessage() != null) {
                msg += " because of\n`%s`".formatted(e.getMessage());
            }
            if (e.getCause() != null) {
                msg += "\n caused by `%s`".formatted(e.getCause().getMessage());
            }
            throw new BashpileUncheckedException(msg, e);
        }
    }

    /** Called by Picocli framework */
    @SuppressWarnings("unused")
    @CommandLine.Command(name = "transpile", description = "Converts Bashpile lines to bash")
    public int transpileCommand() throws IOException {
        System.out.println(transpile());
        return 0;
    }

    public @Nonnull String transpile() throws IOException {
        try (InputStream inputStream = getInputStream()) {
            return parse(inputStream);
        }
    }

    private @Nonnull InputStream getInputStream() throws IOException {
        if (inputFile != null) {
            return Files.newInputStream(inputFile);
        } else if (command != null) {
            return IOUtils.toInputStream(command, StandardCharsets.UTF_8);
        } else {
            System.out.println("Enter your bashpile program, ending with a newline and EOF (ctrl-D).");
            return System.in;
        }
    }

    public void setCommandLine(@Nonnull final CommandLine commandLine) {
        this.commandLine = commandLine;
    }
}
