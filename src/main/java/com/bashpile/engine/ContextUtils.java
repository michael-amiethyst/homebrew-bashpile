package com.bashpile.engine;

import com.bashpile.BashpileParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Encapsulates handling of generated Antlr4 context objects */
public class ContextUtils {

    /** Gets the ID literal text */
    public static @Nonnull String getIdText(@Nonnull final BashpileParser.ReassignmentStatementContext ctx) {
        if (ctx.Id() != null) {
            return ctx.Id().getText();
        } else {
            return ctx.listAccess().Id().getText();
        }
    }

    /** Returns the list access index text (e.g. "5" from "[5]") or null if it cannot be found */
    public static @Nullable String getListAccessorIndexText(@Nonnull final BashpileParser.ReassignmentStatementContext ctx) {
        if (ctx.listAccess() == null) {
            return null;
        }
        return ctx.listAccess().Number().getText();
    }
}
