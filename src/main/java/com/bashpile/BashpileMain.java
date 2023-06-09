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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static com.bashpile.ArrayUtils.arrayOf;

/** Entry point into the program */
@CommandLine.Command(
        name = "execute",
        description = "Converts Bashpile lines to bash and executes them, printing the results"
)
public class BashpileMain implements Runnable {

    private static String[] commandLineArgs;
    private static final Logger log = LogManager.getLogger(BashpileMain.class);

    public static void main(String[] args) {
        commandLineArgs = args;
        CommandLine argProcessor = new CommandLine(new BashpileMain());
        System.exit(argProcessor.execute(args));
    }

    @Override
    public void run() {
        try {
            System.out.println("Enter your bashpile program, ending with a newline and EOF (ctrl-D).");
            String[] executedBashResults = processArgs(BashpileMain.commandLineArgs);
            System.out.println(Arrays.toString(executedBashResults));
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    public static String[] processArg(String filename) throws IOException {
        return processArgs(arrayOf(filename));
    }

    public static String[] processArgs(String[] args) throws IOException {
        // stream is either stdin or the first argument
        InputStream is = System.in;
        boolean argsExist = args.length > 0;
        if (argsExist) {
            String inputFile = args[0];
            is = new FileInputStream(inputFile);
        }

        return parse(is);
    }

    /** antlr calls */
    private static String[] parse(InputStream is) throws IOException {
        log.trace("Starting parse");
        // lexer
        CharStream input = CharStreams.fromStream(is);
        BashpileLexer lexer = new BashpileLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // parser
        BashpileParser parser = new BashpileParser(tokens);
        ParseTree tree = parser.prog();

        String bashScript = transpile(tree);
        String output = CommandLineExecutor.run(bashScript);
        return output.split("\r?\n");
    }

    private static String transpile(ParseTree tree) {
        // visitor
        BashpileVisitor bashpileLogic = new BashpileVisitor(new BashTranslationEngine());
        String bashScript = bashpileLogic.visit(tree);
        return bashScript;
    }
}
