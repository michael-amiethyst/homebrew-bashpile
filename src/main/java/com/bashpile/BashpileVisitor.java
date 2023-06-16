package com.bashpile;

import com.bashpile.engine.TranslationEngine;
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
        translator.setVisitor(this);
    }

    // visitors

    @Override
    public String visitProg(BashpileParser.ProgContext ctx) {
        return translator.strictMode() + ctx.stmt().stream().map(this::visit).collect(Collectors.joining());
    }

    // visit statements

    @Override
    public String visitExprStmt(BashpileParser.ExprStmtContext ctx) {
        return visit(ctx.expr()) + "\n";
    }

    @Override
    public String visitAssignStmt(BashpileParser.AssignStmtContext ctx) {
        String id = ctx.ID().getText();
        String rightSide = ctx.expr().getText();
        return translator.assign(id, rightSide);
    }

    @Override
    public String visitPrintStmt(BashpileParser.PrintStmtContext ctx) {
        return translator.print(ctx);
    }

    @Override
    public String visitFunctionDeclStmt(BashpileParser.FunctionDeclStmtContext ctx) {
        return translator.functionDecl(ctx);
    }

    @Override
    public String visitAnonBlockStmt(BashpileParser.AnonBlockStmtContext ctx) {
        return translator.anonBlock(ctx);
    }

    @Override
    public String visitBlock(BashpileParser.BlockContext ctx) {
        return "";  // pure comments for now
    }

    @Override
    public String visitReturnRule(BashpileParser.ReturnRuleContext ctx) {
        return translator.returnRule(ctx);
    }

    // visit expressions

    @Override
    public String visitCalcExpr(BashpileParser.CalcExprContext ctx) {
        log.trace("In Calc with {} children", ctx.children.size());
        return translator.calc(ctx);
    }

    @Override
    public String visitFunctionCallExpr(BashpileParser.FunctionCallExprContext ctx) {
        return translator.functionCall(ctx);
    }

    @Override
    public String visitParensExpr(BashpileParser.ParensExprContext ctx) {
        return translator.calc(ctx);
    }

    @Override
    public String visitIdExpr(BashpileParser.IdExprContext ctx) {
        return ctx.ID().getText();
    }

    @Override
    public String visitNumberExpr(BashpileParser.NumberExprContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        return node.getText();
    }
}
