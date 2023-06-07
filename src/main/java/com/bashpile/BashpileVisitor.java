package com.bashpile;

import com.bashpile.engine.TranslationEngine;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.PrintStream;

/**
 * Antlr4 calls these methods.  Both walks the parse tree and buffers all output.
 */
public class BashpileVisitor extends BashpileParserBaseVisitor<String> implements Closeable {

    private final ByteArrayOutputStream translationBackingStore = new ByteArrayOutputStream(1024);

    /** The resulting shell commands */
    private final PrintStream translation = new PrintStream(translationBackingStore);

    private final TranslationEngine translator;

    private final Logger log = LogManager.getLogger();

    public BashpileVisitor(TranslationEngine translator) {
        this.translator = translator;
    }

    // visitors

    @Override
    public String visit(ParseTree tree) {
        super.visit(tree);
        translation.flush();
        return translationBackingStore.toString();
    }

    @Override
    public String visitProg(BashpileParser.ProgContext ctx) {
        translation.print(translator.strictMode());
        super.visitProg(ctx);
        return null;
    }

    @Override
    public String visitAssign(BashpileParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        String rightSide = ctx.expr().getText();
        translation.printf(translator.assign(id, rightSide));
        return null;
    }

    @Override
    public String visitCalc(BashpileParser.CalcContext ctx) {
        log.trace("In Calc with {} children", ctx.children.size());
        translation.print(translator.calc(ctx));
        return null;
    }

    @Override
    public void close() {
        translation.close();
    }
}
