package com.bashpile.engine;

import com.bashpile.BashpileParser;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Methods translate small parser rules (e.g. statements and expressions) to the target language.
 */
public interface TranslationEngine {

    /**
     * Our {@link BashpileVisitor} needs a TranslationEngine and we need a BashpileVisitor.
     * <br>
     * So you make a TranslationEngine, pass to the BashpileVisitor then set the visitor.
     */
    void setVisitor(BashpileVisitor visitor);

    Translation strictMode();

    Translation assign(String variable, String value);

    Translation print(BashpileParser.PrintStmtContext ctx);

    Translation functionForwardDecl(BashpileParser.FunctionForwardDeclStmtContext ctx);

    Translation functionDecl(BashpileParser.FunctionDeclStmtContext ctx);

    Translation anonBlock(BashpileParser.AnonBlockStmtContext ctx);

    Translation returnRule(BashpileParser.ReturnRuleContext ctx);

    Translation calc(ParserRuleContext ctx);

    Translation functionCall(BashpileParser.FunctionCallExprContext ctx);
}
