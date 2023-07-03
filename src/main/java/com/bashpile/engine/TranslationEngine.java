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
    Translation imports();

    // statement translations

    Translation assign(final BashpileParser.AssignStmtContext ctx);

    Translation reassign(final BashpileParser.ReAssignStmtContext ctx);

    Translation print(final BashpileParser.PrintStmtContext ctx);

    Translation functionForwardDecl(final BashpileParser.FunctionForwardDeclStmtContext ctx);

    Translation functionDecl(final BashpileParser.FunctionDeclStmtContext ctx);

    Translation anonBlock(final BashpileParser.AnonBlockStmtContext ctx);

    Translation returnRule(final BashpileParser.ReturnRuleContext ctx);

    // expression translations

    Translation functionCall(final BashpileParser.FunctionCallExprContext ctx);

    Translation parens(final BashpileParser.ParensExprContext ctx);

    Translation calc(final BashpileParser.CalcExprContext ctx);
}
