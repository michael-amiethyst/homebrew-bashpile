package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileVisitor;

public interface TranslationEngine {

    String strictMode();

    String assign(String variable, String value);

    String block(BashpileVisitor visitor, BashpileParser.BlockContext ctx);

    String calc(BashpileParser.CalcContext ctx);
}
