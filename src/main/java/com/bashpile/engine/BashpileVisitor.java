package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.stream.Collectors;

import static com.bashpile.Asserts.assertTextBlock;
import static com.bashpile.engine.Translation.toStringTranslation;

/**
 * Antlr4 calls these methods.
 * 
 * @see com.bashpile.AntlrUtils#parse(InputStream)
 */
public class BashpileVisitor extends BashpileParserBaseVisitor<Translation> {

    private final TranslationEngine translator;

    private final Logger log = LogManager.getLogger(BashpileVisitor.class);

    private ParserRuleContext contextRoot;

    public BashpileVisitor(final TranslationEngine translator) {
        this.translator = translator;
        translator.setVisitor(this);
    }

    /**
     * Do not modify.
     *
     * @return The prog context.
     */
    public ParserRuleContext getContextRoot() {
        // pass-by-reference because a deep copy for a non-serializable object is a nightmare
        return contextRoot;
    }

    // visitors

    @Override
    public Translation visitProg(final BashpileParser.ProgContext ctx) {
        // save root for later usage
        contextRoot = ctx;

        // prepend strictMode text to the statement results
        final String header = translator.strictModeHeader().text();
        assertTextBlock(header);
        String translatedTextBlock = ctx.stmt().stream()
                .map(this::visit)
                .map(Translation::text)
                .collect(Collectors.joining());
        assertTextBlock(translatedTextBlock);

        final String importLibs = translator.imports().text();

        return toStringTranslation(header, importLibs, translatedTextBlock);
    }

    // visit statements

    @Override
    public Translation visitBlankStmt(BashpileParser.BlankStmtContext ctx) {
        // was returning "\r\n" without an override
        return toStringTranslation("\n");
    }

    @Override
    public Translation visitExprStmt(final BashpileParser.ExprStmtContext ctx) {
        return visit(ctx.expr()).add("\n");
    }

    @Override
    public Translation visitAssignStmt(final BashpileParser.AssignStmtContext ctx) {
        return translator.assign(ctx);
    }

    @Override
    public Translation visitReAssignStmt(BashpileParser.ReAssignStmtContext ctx) {
        return translator.reassign(ctx);
    }

    @Override
    public Translation visitPrintStmt(final BashpileParser.PrintStmtContext ctx) {
        return translator.print(ctx);
    }

    @Override
    public Translation visitFunctionForwardDeclStmt(final BashpileParser.FunctionForwardDeclStmtContext ctx) {
        return translator.functionForwardDecl(ctx);
    }

    @Override
    public Translation visitFunctionDeclStmt(final BashpileParser.FunctionDeclStmtContext ctx) {
        return translator.functionDecl(ctx);
    }

    @Override
    public Translation visitAnonBlockStmt(final BashpileParser.AnonBlockStmtContext ctx) {
        return translator.anonBlock(ctx);
    }

    @Override
    public Translation visitBlock(final BashpileParser.BlockContext ctx) {
        return Translation.empty;  // pure comments for now
    }

    @Override
    public Translation visitReturnRule(final BashpileParser.ReturnRuleContext ctx) {
        return translator.returnRule(ctx);
    }

    // visit expressions

    @Override
    public Translation visitCalcExpr(final BashpileParser.CalcExprContext ctx) {
        log.trace("In Calc with {} children", ctx.children.size());
        return translator.calc(ctx);
    }

    @Override
    public Translation visitFunctionCallExpr(final BashpileParser.FunctionCallExprContext ctx) {
        return translator.functionCall(ctx);
    }

    @Override
    public Translation visitParensExpr(final BashpileParser.ParensExprContext ctx) {
        return translator.calc(ctx);
    }

    @Override
    public Translation visitIdExpr(final BashpileParser.IdExprContext ctx) {
        return toStringTranslation(ctx.ID().getText());
    }

    @Override
    public Translation visitNumberExpr(final BashpileParser.NumberExprContext ctx) {
        return new Translation(ctx.getText(), Type.parseNumberString(ctx.NUMBER().getText()), MetaType.NORMAL);
    }

    @Override
    public Translation visitTerminal(final TerminalNode node) {
        return toStringTranslation(node.getText());
    }
}
