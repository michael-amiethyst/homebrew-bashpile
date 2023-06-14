package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileVisitor;

public interface TranslationEngine {

    String strictMode();

    String assign(String variable, String value);

    String functionDecl(BashpileVisitor bashpileVisitor, BashpileParser.FunctionDeclContext ctx);

    String anonBlock(BashpileVisitor visitor, BashpileParser.AnonBlockContext ctx);

    String calc(BashpileVisitor visitor, BashpileParser.CalcContext ctx);
}
