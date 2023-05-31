package com.bashpile;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class BashpileMain {

    public static void main(String[] args) throws IOException {
        BashpileMain bashpile = new BashpileMain();
        bashpile.processArgs(args);
    }

    public static String[] processArg(String filename) throws IOException {
        return processArgs(filename.split(" "));
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

    private static String[] parse(InputStream is) throws IOException {
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

            bashpileLogic.visit(tree);  // writes to byteOutput here

            return byteOutput.toString().split("\r?\n");
        }
    }
}
