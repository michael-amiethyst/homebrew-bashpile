package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Callable;

import static com.bashpile.AntlrUtils.parse;

/** Entry point into the program */
@CommandLine.Command(
        name = "bashpile",
        description = "Converts Bashpile lines to Bash"
)
public class BashpileMain implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(BashpileMain.class);

    public static void main(String[] args) {
        final BashpileMain bashpile = new BashpileMain();
        final CommandLine argProcessor = new CommandLine(bashpile);
        bashpile.setCommandLine(argProcessor);
        System.exit(argProcessor.execute(args));
    }

    @CommandLine.Option(names = {"-i", "--inputFile"})
    private String inputFile;

    private CommandLine commandLine;

    public BashpileMain() {}

    public BashpileMain(String inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public Integer call() {
        // prints help text and returns 'general error'
        commandLine.usage(System.out);
        return 1;
    }

    /** Called by the picocli framework */
    @CommandLine.Command(name = "execute", description = "Converts Bashpile lines to bash and executes them")
    public int executeCommand() {
        System.out.println(execute().getLeft());
        return 0;
    }

    public Pair<String, Integer> execute() {
        log.debug("In {}", System.getProperty("user.dir"));
        String bashScript = "<unparsed stream: %s>".formatted(
                Objects.requireNonNullElse(inputFile, "System.in"));
        try {
            bashScript = transpile();
            return CommandLineExecutor.failableRun(bashScript);
        } catch (Throwable e) {
            throw new BashpileUncheckedException("Couldn't run `%s`.".formatted(bashScript), e);
        }
    }

    /** Called by Picocli framework */
    @CommandLine.Command(name = "transpile", description = "Converts Bashpile lines to bash")
    public int transpileCommand() throws IOException {
        System.out.println(transpile());
        return 0;
    }

    public String transpile() throws IOException {
        return parse(getInputStream());
    }

    private InputStream getInputStream() throws FileNotFoundException {
        if (inputFile != null) {
            return new FileInputStream(inputFile);
        } else {
            System.out.println("Enter your bashpile program, ending with a newline and EOF (ctrl-D).");
            return System.in;
        }
    }

    public void setCommandLine(CommandLine commandLine) {
        this.commandLine = commandLine;
    }
}
