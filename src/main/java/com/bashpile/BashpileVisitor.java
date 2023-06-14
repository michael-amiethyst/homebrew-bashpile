package com.bashpile;

import com.bashpile.engine.TranslationEngine;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

/**
 * Antlr4 calls these methods.  Both walks the parse tree and buffers all output.
 */
public class BashpileVisitor extends BashpileParserBaseVisitor<String> {

    private final TranslationEngine translator;

    private final Logger log = LogManager.getLogger(BashpileVisitor.class);

    public BashpileVisitor(TranslationEngine translator) {
        this.translator = translator;
    }

    // visitors

    @Override
    public String visitProg(BashpileParser.ProgContext ctx) {
        return translator.strictMode() + ctx.stat().stream().map(this::visit).collect(Collectors.joining());
    }

    // visit statements

    @Override
    public String visitPrintExpr(BashpileParser.PrintExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public String visitAssign(BashpileParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        String rightSide = ctx.expr().getText();
        return translator.assign(id, rightSide);
    }

    @Override
    public String visitFunctionDecl(BashpileParser.FunctionDeclContext ctx) {
        return translator.functionDecl(this, ctx);
    }

    @Override
    public String visitAnonBlock(BashpileParser.AnonBlockContext ctx) {
        return translator.anonBlock(this, ctx);
    }

    // visit expressions

    @Override
    public String visitCalc(BashpileParser.CalcContext ctx) {
        log.trace("In Calc with {} children", ctx.children.size());
        return translator.calc(this, ctx);
    }

    @Override
    public String visitFunctionCall(BashpileParser.FunctionCallContext ctx) {
        // TODO move to translation engine
        // TODO refactor visits out of translation engine
        return ctx.ID().getText() + " " + ctx.paramaters().expr().stream()
                .map(RuleContext::getText).collect(Collectors.joining(" ")) + "\n";
    }

    @Override
    public String visitParens(BashpileParser.ParensContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public String visitId(BashpileParser.IdContext ctx) {
        return ctx.ID().getText();
    }

    @Override
    public String visitNumber(BashpileParser.NumberContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        return node.getText();
    }
}
