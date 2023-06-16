package com.bashpile;

import com.bashpile.engine.BashTranslationEngine;
import com.bashpile.engine.BashpileVisitor;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public class AntlrUtils {

    private static final Logger log = LogManager.getLogger(AntlrUtils.class);

    /** antlr calls */
    public static String parse(InputStream is) throws IOException {
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
        // visitor and engine linked in visitor constructor
        BashpileVisitor bashpileLogic = new BashpileVisitor(new BashTranslationEngine());
        return bashpileLogic.visit(tree).text();
    }
}
