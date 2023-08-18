package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileParserBaseVisitor;
import com.bashpile.engine.strongtypes.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

import static com.bashpile.engine.Translation.NEWLINE;
import static com.bashpile.engine.strongtypes.TypeMetadata.NORMAL;

/**
 * Antlr4 calls these methods.
 * 
 * @see com.bashpile.AntlrUtils#parse(String, InputStream) 
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
    public @Nonnull Translation visitProgram(@Nonnull final BashpileParser.ProgramContext ctx) {
        // save root for later usage
        contextRoot = ctx;

        final Translation statementTranslation = ctx.statement().stream()
                .map(this::visit)
                .map(Translation::assertEmptyPreamble)
                .reduce(Translation::add)
                .orElseThrow();

        // add header, libs and statements
        return translator.originHeader()
                .add(translator.strictModeHeader())
                .add(translator.importsHeaders())
                .add(statementTranslation);
    }

    // visit statements

    @Override
    public Translation visitCreatesStatement(BashpileParser.CreatesStatementContext ctx) {
        return translator.createsStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitFunctionForwardDeclarationStatement(
            @Nonnull final BashpileParser.FunctionForwardDeclarationStatementContext ctx) {
        return translator.functionForwardDeclarationStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitFunctionDeclarationStatement(
            @Nonnull final BashpileParser.FunctionDeclarationStatementContext ctx) {
        return translator.functionDeclarationStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitAnonymousBlockStatement(
            @Nonnull final BashpileParser.AnonymousBlockStatementContext ctx) {
        return translator.anonymousBlockStatement(ctx);
    }

    @Override
    public Translation visitConditionalStatement(BashpileParser.ConditionalStatementContext ctx) {
        return translator.conditionalStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitAssignmentStatement(@Nonnull final BashpileParser.AssignmentStatementContext ctx) {
        return translator.assignmentStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitReassignmentStatement(@Nonnull BashpileParser.ReassignmentStatementContext ctx) {
        return translator.reassignmentStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitPrintStatement(@Nonnull final BashpileParser.PrintStatementContext ctx) {
        return translator.printStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitExpressionStatement(@Nonnull final BashpileParser.ExpressionStatementContext ctx) {
        return translator.expressionStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitBlankStmt(@Nonnull BashpileParser.BlankStmtContext ctx) {
        // will return "\r\n" on Windows without an override
        return NEWLINE;
    }

    @Override
    public @Nonnull Translation visitReturnPsudoStatement(
            @Nonnull final BashpileParser.ReturnPsudoStatementContext ctx) {
        return translator.returnPsudoStatement(ctx);
    }

    // visit expressions

    @Override
    public Translation visitTypecastExpression(BashpileParser.TypecastExpressionContext ctx) {
        return translator.typecastExpression(ctx);
    }

    @Override
    public @Nonnull Translation visitFunctionCallExpression(
            @Nonnull final BashpileParser.FunctionCallExpressionContext ctx) {
        return translator.functionCallExpression(ctx);
    }

    // visit operator expressions

    @Override
    public @Nonnull Translation visitParenthesisExpression(
            @Nonnull final BashpileParser.ParenthesisExpressionContext ctx) {
        return translator.parenthesisExpression(ctx);
    }

    @Override
    public @Nonnull Translation visitCalculationExpression(
            @Nonnull final BashpileParser.CalculationExpressionContext ctx) {
        log.trace("In Calc with {} children", ctx.children.size());
        return translator.calculationExpression(ctx);
    }

    @Override
    public Translation visitPrimaryExpression(BashpileParser.PrimaryExpressionContext ctx) {
        return translator.primaryExpression(ctx);
    }

    // visit type expressions

    @Override
    public Translation visitBoolExpression(BashpileParser.BoolExpressionContext ctx) {
        return new Translation(ctx.Bool().getText(), Type.BOOL, NORMAL);
    }

    @Override
    public @Nonnull Translation visitNumberExpression(@Nonnull final BashpileParser.NumberExpressionContext ctx) {
        return new Translation(ctx.getText(), Type.parseNumberString(ctx.Number().getText()), NORMAL);
    }

    /**
     * Put variable into ${}, e.g. "var" becomes "${var}".
     * */
    @Override
    public @Nonnull Translation visitIdExpression(@Nonnull final BashpileParser.IdExpressionContext ctx) {
        return translator.idExpression(ctx);
    }

    /** Default type is STR */
    @Override
    public @Nonnull Translation visitTerminal(@Nonnull final TerminalNode node) {
        // may or may not be multi-line
        return new Translation(node.getText(), Type.STR, NORMAL);
    }

    // expression helper rules

    @Override
    public Translation visitShellString(BashpileParser.ShellStringContext ctx) {
        return translator.shellString(ctx);
    }

    @Override
    public Translation visitInline(BashpileParser.InlineContext ctx) {
        return translator.inline(ctx);
    }
}
