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

    Translation assignmentStatement(final BashpileParser.AssignStmtContext ctx);

    Translation reassignmentStatement(final BashpileParser.ReAssignStmtContext ctx);

    Translation printStatement(final BashpileParser.PrintStmtContext ctx);

    Translation functionForwardDeclStatement(final BashpileParser.FunctionForwardDeclStmtContext ctx);

    Translation functionDeclStatement(final BashpileParser.FunctionDeclStmtContext ctx);

    Translation anononymousBlockStatement(final BashpileParser.AnonBlockStmtContext ctx);

    Translation returnPsudoStatement(final BashpileParser.ReturnPsudoStatementContext ctx);

    // expression translations

    Translation functionCallExpression(final BashpileParser.FunctionCallExprContext ctx);

    Translation parenthesisExpression(final BashpileParser.ParenthesisExprContext ctx);

    Translation calculationExpression(final BashpileParser.CalculationExprContext ctx);
}
