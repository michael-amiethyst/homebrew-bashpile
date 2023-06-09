package com.bashpile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import static com.bashpile.AntlrUtils.parse;

/** Entry point into the program */
@CommandLine.Command(
        name = "bashpile",
        description = "Converts Bashpile lines to Bash"
)
public class BashpileMain implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(BashpileMain.class);

    @CommandLine.Option(names = {"-i", "--inputFile"})
    private String inputFile;

    private CommandLine commandLine;

    public BashpileMain() {}

    public BashpileMain(String inputFile) {
        this.inputFile = inputFile;
    }

    public static void main(String[] args) {
        BashpileMain bashpile = new BashpileMain();
        CommandLine argProcessor = new CommandLine(bashpile);
        bashpile.setCommandLine(argProcessor);
        System.exit(argProcessor.execute(args));
    }

    @Override
    public Integer call() {
        commandLine.usage(System.out);
        return -1;
    }

    @CommandLine.Command(name = "execute", description = "Converts Bashpile lines to bash and executes them")
    public void executeCommand() throws IOException {
        System.out.println(execute());
        System.exit(0);
    }

    public String execute() throws IOException {
        String bashScript = parse(getInputStream());
        return CommandLineExecutor.run(bashScript);
    }

    @CommandLine.Command(name = "transpile", description = "Converts Bashpile lines to bash")
    public void transpileCommand() throws IOException {
        System.out.println(parse(getInputStream()));
        System.exit(0);
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
