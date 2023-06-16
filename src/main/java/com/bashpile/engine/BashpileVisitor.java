package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileParserBaseVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.stream.Collectors;

import static com.bashpile.engine.Translation.toStringTranslation;

/**
 * Antlr4 calls these methods.
 * 
 * @see com.bashpile.AntlrUtils#parse(InputStream)
 */
public class BashpileVisitor extends BashpileParserBaseVisitor<Translation> {

    private final TranslationEngine translator;

    private final Logger log = LogManager.getLogger(BashpileVisitor.class);

    public BashpileVisitor(TranslationEngine translator) {
        this.translator = translator;
        translator.setVisitor(this);
    }

    // visitors

    @Override
    public Translation visitProg(BashpileParser.ProgContext ctx) {
        // prepend strictMode text to the statement results
        String header = translator.strictMode().text();
        String translatedTextBlock = ctx.stmt().stream()
                .map(this::visit)
                .map(Translation::text)
                .collect(Collectors.joining());
        return toStringTranslation(header + translatedTextBlock);
    }

    // visit statements

    @Override
    public Translation visitExprStmt(BashpileParser.ExprStmtContext ctx) {
        return visit(ctx.expr()).add("\n");
    }

    @Override
    public Translation visitAssignStmt(BashpileParser.AssignStmtContext ctx) {
        String id = ctx.ID().getText();
        String rightSide = ctx.expr().getText();
        return translator.assign(id, rightSide);
    }

    @Override
    public Translation visitPrintStmt(BashpileParser.PrintStmtContext ctx) {
        return translator.print(ctx);
    }

    @Override
    public Translation visitFunctionDeclStmt(BashpileParser.FunctionDeclStmtContext ctx) {
        return translator.functionDecl(ctx);
    }

    @Override
    public Translation visitAnonBlockStmt(BashpileParser.AnonBlockStmtContext ctx) {
        return translator.anonBlock(ctx);
    }

    @Override
    public Translation visitBlock(BashpileParser.BlockContext ctx) {
        return Translation.empty;  // pure comments for now
    }

    @Override
    public Translation visitReturnRule(BashpileParser.ReturnRuleContext ctx) {
        return translator.returnRule(ctx);
    }

    // visit expressions

    @Override
    public Translation visitCalcExpr(BashpileParser.CalcExprContext ctx) {
        log.trace("In Calc with {} children", ctx.children.size());
        return translator.calc(ctx);
    }

    @Override
    public Translation visitFunctionCallExpr(BashpileParser.FunctionCallExprContext ctx) {
        return translator.functionCall(ctx);
    }

    @Override
    public Translation visitParensExpr(BashpileParser.ParensExprContext ctx) {
        return translator.calc(ctx);
    }

    @Override
    public Translation visitIdExpr(BashpileParser.IdExprContext ctx) {
        return toStringTranslation(ctx.ID().getText());
    }

    @Override
    public Translation visitNumberExpr(BashpileParser.NumberExprContext ctx) {
        return toStringTranslation(ctx.getText());
    }

    @Override
    public Translation visitTerminal(TerminalNode node) {
        return toStringTranslation(node.getText());
    }
}
