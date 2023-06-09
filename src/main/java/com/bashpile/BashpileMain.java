package com.bashpile;

import com.bashpile.engine.BashTranslationEngine;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

/** Entry point into the program */
@CommandLine.Command(
        name = "bashpile",
        description = "Converts Bashpile lines to bash and executes them, printing the results"
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

    @CommandLine.Command(name = "execute")
    public void executeCommand() throws IOException {
        System.out.println(execute());
        System.exit(0);
    }

    public String execute() throws IOException {
        String bashScript = parse(getInputStream());
        return CommandLineExecutor.run(bashScript);
    }

    @CommandLine.Command(name = "transpile")
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

    // static helpers

    /** antlr calls */
    private static String parse(InputStream is) throws IOException {
        log.trace("Starting parse");
        // lexer
        CharStream input = CharStreams.fromStream(is);
        BashpileLexer lexer = new BashpileLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // parser
        BashpileParser parser = new BashpileParser(tokens);
        ParseTree tree = parser.prog();

        return transpile(tree);
    }

    /** Returns bash text block */
    private static String transpile(ParseTree tree) {
        // visitor
        BashpileVisitor bashpileLogic = new BashpileVisitor(new BashTranslationEngine());
        return bashpileLogic.visit(tree);
    }
}
