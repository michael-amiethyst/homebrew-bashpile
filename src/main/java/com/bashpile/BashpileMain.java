package com.bashpile;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BashpileMain {

    public static void main(String[] args) throws IOException {
        String inputFile = null;
        if (args.length > 0) {
            inputFile = args[0];
        }
        InputStream is = System.in;
        if (inputFile != null) {
            is = new FileInputStream(inputFile);
        }
        CharStream input = CharStreams.fromStream(is);
        BashpileLexer lexer = new BashpileLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BashpileParser parser = new BashpileParser(tokens);
        ParseTree tree = parser.prog();
        BashpileVisitor eval = new BashpileVisitor();
        eval.visit(tree);
    }
}
