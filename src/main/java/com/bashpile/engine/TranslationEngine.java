package com.bashpile.engine;

import com.bashpile.BashpileParser;

/**
 * Methods translate small parser rules (e.g. statements and expressions) to the target language.
 */
public interface TranslationEngine {

    /**
     * Our {@link BashpileVisitor} needs a TranslationEngine and we need a BashpileVisitor.
     * <br>
     * So you make a TranslationEngine, pass to the BashpileVisitor then set the visitor.
     */
    void setVisitor(final BashpileVisitor visitor);

    // headers

    Translation strictModeHeader();

    /** To source our bundled libraries */
    Translation importsHeaders();

    // statement translations

    Translation assignmentStatement(final BashpileParser.AssignmentStatementContext ctx);

    Translation reassignmentStatement(final BashpileParser.ReassignmentStatementContext ctx);

    Translation printStatement(final BashpileParser.PrintStatementContext ctx);

    Translation functionForwardDeclarationStatement(final BashpileParser.FunctionForwardDeclarationStatementContext ctx);

    Translation functionDeclarationStatement(final BashpileParser.FunctionDeclarationStatementContext ctx);

    Translation anonymousBlockStatement(final BashpileParser.AnonymousBlockStatementContext ctx);

    Translation returnPsudoStatement(final BashpileParser.ReturnPsudoStatementContext ctx);

    // expression translations

    Translation commandObjectExpression(final BashpileParser.CommandObjectExpressionContext ctx);

    Translation functionCallExpression(final BashpileParser.FunctionCallExprContext ctx);

    Translation parenthesisExpression(final BashpileParser.ParenthesisExprContext ctx);

    Translation calculationExpression(final BashpileParser.CalculationExprContext ctx);
}
