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

    Translation originHeader();

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

    Translation createsStatement(final BashpileParser.CreatesStatementContext ctx);

    Translation expressionStatement(final BashpileParser.ExpressionStatementContext ctx);

    Translation returnPsudoStatement(final BashpileParser.ReturnPsudoStatementContext ctx);

    // expression translations

    Translation typecastExpression(final BashpileParser.TypecastExpressionContext ctx);

    Translation functionCallExpression(final BashpileParser.FunctionCallExpressionContext ctx);

    Translation parenthesisExpression(final BashpileParser.ParenthesisExpressionContext ctx);

    Translation calculationExpression(final BashpileParser.CalculationExpressionContext ctx);

    Translation idExpression(final BashpileParser.IdExpressionContext ctx);

    // expression helper translations

    Translation shellString(final BashpileParser.ShellStringContext ctx);

    Translation inline(final BashpileParser.InlineContext ctx);
}
