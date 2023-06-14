package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileVisitor;

public interface TranslationEngine {

    void setVisitor(BashpileVisitor visitor);

    String strictMode();

    String assign(String variable, String value);

    String functionDecl(BashpileParser.FunctionDeclContext ctx);

    String anonBlock(BashpileParser.AnonBlockContext ctx);

    String returnStmt(BashpileParser.ReturnStmtContext ctx);

    String calc(BashpileParser.CalcContext ctx);

    String functionCall(BashpileParser.FunctionCallContext ctx);
}
