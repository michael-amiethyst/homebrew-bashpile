package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileVisitor;
import org.antlr.v4.runtime.ParserRuleContext;

public interface TranslationEngine {

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
