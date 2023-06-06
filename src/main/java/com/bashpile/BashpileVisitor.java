package com.bashpile;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.PrintStream;
import java.util.stream.Collectors;

/**
 * Antlr4 calls these methods.  Both walks the parse tree and buffers all output.
 */
public class BashpileVisitor extends BashpileParserBaseVisitor<String> implements Closeable {

    private final ByteArrayOutputStream translationBackingStore = new ByteArrayOutputStream(1024);

    private final PrintStream translation = new PrintStream(translationBackingStore);

    private final Logger log = LogManager.getLogger();

    // visitors

    @Override
    public String visit(ParseTree tree) {
        super.visit(tree);
        translation.flush();
        return translationBackingStore.toString();
    }

    @Override
    public String visitProg(BashpileParser.ProgContext ctx) {
        translation.print("set -euo pipefail\n");
        translation.print("export IFS=$'\\n\\t'\n");
        super.visitProg(ctx);
        return null;
    }

    @Override
    public String visitAssign(BashpileParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        String rightSide = ctx.expr().getText();
        translation.printf("export %s=%s\n", id, rightSide);
        return null;
    }

    @Override
    public String visitCalc(BashpileParser.CalcContext ctx) {
        log.trace("In Calc with {} children", ctx.children.size());
        // convert "var" to "$var" for Bash
        String text = ctx.children.stream().map(
                x -> x instanceof BashpileParser.IdContext ? "$" + x.getText() : x.getText())
                .collect(Collectors.joining());
        translation.printf("bc <<< \"%s\"\n", text);
        return null;
    }

    @Override
    public void close() {
        translation.close();
    }
}
