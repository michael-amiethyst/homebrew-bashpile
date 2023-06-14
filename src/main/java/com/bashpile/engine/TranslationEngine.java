package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileVisitor;

public interface TranslationEngine {

    void setVisitor(BashpileVisitor visitor);

    String strictMode();

    String assign(String variable, String value);

    String functionDecl(BashpileParser.FunctionDeclStmtContext ctx);

    String anonBlock(BashpileParser.AnonBlockStmtContext ctx);

    String returnStmt(BashpileParser.ReturnStmtContext ctx);

    String calc(BashpileParser.CalcExprContext ctx);

    String functionCall(BashpileParser.FunctionCallExprContext ctx);
}
