package com.bashpile;

import com.bashpile.renderers.WslBashRenderer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

import static com.bashpile.ArrayUtils.arrayOf;

/** Entry point into the program */
public class BashpileMain {

    private static final Logger log = LogManager.getLogger(BashpileMain.class);

    public static void main(String[] args) throws IOException {
        processArgs(args);
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

        return applyBashpileLogic(tree);
    }

    private static String[] applyBashpileLogic(ParseTree tree) throws IOException {
        // visitor
        try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
             BashpileVisitor bashpileLogic = new BashpileVisitor(byteOutput)) {

            AstNode astRoot = bashpileLogic.visit(tree);  // writes to byteOutput here
            WslBashRenderer renderer = new WslBashRenderer();
            renderer.render(astRoot);
            return byteOutput.toString().split("\r?\n");
        }
    }
}
