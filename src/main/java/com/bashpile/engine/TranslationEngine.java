package com.bashpile.engine;

import com.bashpile.BashpileParser;
import org.antlr.v4.runtime.ParserRuleContext;

import javax.annotation.Nonnull;

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

    Translation strictModeHeader();

    /** To source our bundled libraries */
    Translation imports();

    Translation assign(final BashpileParser.AssignStmtContext ctx);

    Translation reassign(final BashpileParser.ReAssignStmtContext ctx);

    Translation print(final BashpileParser.PrintStmtContext ctx);

    Translation functionForwardDecl(final BashpileParser.FunctionForwardDeclStmtContext ctx);

    Translation functionDecl(final BashpileParser.FunctionDeclStmtContext ctx);

    Translation anonBlock(final BashpileParser.AnonBlockStmtContext ctx);

    Translation returnRule(final BashpileParser.ReturnRuleContext ctx);

    Translation calc(final ParserRuleContext ctx);

    Translation functionCall(final BashpileParser.FunctionCallExprContext ctx);

    Translation parens(final BashpileParser.ParensExprContext ctx);
}
