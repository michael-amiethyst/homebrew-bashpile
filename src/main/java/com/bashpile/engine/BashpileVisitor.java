package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileParserBaseVisitor;
import com.bashpile.engine.strongtypes.MetaType;
import com.bashpile.engine.strongtypes.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    public BashpileVisitor(@Nonnull final TranslationEngine translator) {
        this.translator = translator;
        translator.setVisitor(this);
    }

    /**
     * Do not modify.  Will be null before the first visit.
     *
     * @return The root of the Bashpile context tree.
     */
    public @Nullable ParserRuleContext getContextRoot() {
        // pass-by-reference because a deep copy for a non-serializable object is a nightmare
        return contextRoot;
    }

    // visitors

    @Override
    public @Nonnull Translation visitProg(@Nonnull final BashpileParser.ProgContext ctx) {
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

        final String importLibs = translator.importsHeaders().text();

        return toStringTranslation(header, importLibs, translatedTextBlock);
    }

    // visit statements

    @Override
    public @Nonnull Translation visitExprStmt(@Nonnull final BashpileParser.ExprStmtContext ctx) {
        return visit(ctx.expr()).add("\n");
    }

    @Override
    public @Nonnull Translation visitAssignStmt(@Nonnull final BashpileParser.AssignStmtContext ctx) {
        return translator.assignmentStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitReAssignStmt(@Nonnull BashpileParser.ReAssignStmtContext ctx) {
        return translator.reassignmentStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitPrintStmt(@Nonnull final BashpileParser.PrintStmtContext ctx) {
        return translator.printStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitFunctionForwardDeclStmt(
            @Nonnull final BashpileParser.FunctionForwardDeclStmtContext ctx) {
        return translator.functionForwardDeclStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitFunctionDeclStmt(@Nonnull final BashpileParser.FunctionDeclStmtContext ctx) {
        return translator.functionDeclStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitAnonBlockStmt(@Nonnull final BashpileParser.AnonBlockStmtContext ctx) {
        return translator.anononymousBlockStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitReturnPsudoStmt(@Nonnull final BashpileParser.ReturnPsudoStmtContext ctx) {
        return translator.returnRuleStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitBlankStmt(@Nonnull BashpileParser.BlankStmtContext ctx) {
        // was returning "\r\n" on Windows without an override
        return toStringTranslation("\n");
    }

    // visit expressions

    @Override
    public @Nonnull Translation visitFunctionCallExpr(@Nonnull final BashpileParser.FunctionCallExprContext ctx) {
        return translator.functionCallExpression(ctx);
    }

    // visit operator expressions

    @Override
    public @Nonnull Translation visitParensExpr(@Nonnull final BashpileParser.ParensExprContext ctx) {
        return translator.parensExpression(ctx);
    }

    @Override
    public @Nonnull Translation visitNumberExpr(@Nonnull final BashpileParser.NumberExprContext ctx) {
        return new Translation(ctx.getText(), Type.parseNumberString(ctx.NUMBER().getText()), MetaType.NORMAL);
    }

    @Override
    public @Nonnull Translation visitCalcExpr(@Nonnull final BashpileParser.CalcExprContext ctx) {
        log.trace("In Calc with {} children", ctx.children.size());
        return translator.calcExpression(ctx);
    }

    // visit type expressions

    @Override
    public Translation visitBoolExpr(BashpileParser.BoolExprContext ctx) {
        return new Translation(ctx.BOOL().getText(), Type.BOOL, MetaType.NORMAL);
    }

    @Override
    public @Nonnull Translation visitIdExpr(@Nonnull final BashpileParser.IdExprContext ctx) {
        return toStringTranslation(ctx.ID().getText());
    }

    /** Default type is STR */
    @Override
    public @Nonnull Translation visitTerminal(@Nonnull final TerminalNode node) {
        return toStringTranslation(node.getText());
    }
}
