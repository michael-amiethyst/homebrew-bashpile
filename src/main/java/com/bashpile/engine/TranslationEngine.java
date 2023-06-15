package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileVisitor;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Methods translate small parser rules (e.g. statements and expressions) to the target language.
 */
public interface TranslationEngine {

    /**
     * Our {@link BashpileVisitor} needs a TranslationEngine and we need a BashpileVisitor.
     *
     * So you make a TranslationEngine, pass to the BashpileVisitor then set the visitor.
     */
    void setVisitor(BashpileVisitor visitor);

    String strictMode();

    String assign(String variable, String value);

    String print(BashpileParser.PrintStmtContext ctx);

    String functionDecl(BashpileParser.FunctionDeclStmtContext ctx);

    String anonBlock(BashpileParser.AnonBlockStmtContext ctx);

    String returnRule(BashpileParser.ReturnRuleContext ctx);

    String calc(ParserRuleContext ctx);

    String functionCall(BashpileParser.FunctionCallExprContext ctx);
}
