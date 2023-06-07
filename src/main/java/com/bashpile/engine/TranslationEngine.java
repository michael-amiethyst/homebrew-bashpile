package com.bashpile.engine;

import com.bashpile.BashpileParser;

public interface TranslationEngine {

    String strictMode();

    String assign(String variable, String value);

    String calc(BashpileParser.CalcContext ctx);
}
