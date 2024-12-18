package com.bashpile.engine;

import com.bashpile.BashpileParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/** Encapsulates handling of generated Antlr4 context objects */
public class ContextUtils {

    /** Gets the left hand side's ID's literal text */
    public static @Nonnull String getIdText(@Nonnull final BashpileParser.ReassignmentStatementContext ctx) {
        if (ctx.Id() != null) {
            return ctx.Id().getText();
        } else {
            return Objects.requireNonNull(ctx.listAccess()).Id().getText();
        }
    }

    /** Gets the ID literal text */
    public static @Nullable String getIdText(@Nonnull final BashpileParser.ListAccessExpressionContext ctx) {
        return ctx.listAccess() != null && ctx.listAccess().Id() != null ? ctx.listAccess().Id().getText() : null;
    }

    /** Returns the list access index text (e.g. "5" from "[5]") or null if it cannot be found */
    public static @Nullable String getListAccessorIndexText(@Nonnull final BashpileParser.ReassignmentStatementContext ctx) {
        if (ctx.listAccess() == null || ctx.listAccess().NumberValues() == null) {
            return null;
        }
        final String minus = ctx.listAccess().Minus() != null ? "-" : "";
        return minus + ctx.listAccess().NumberValues().getText();
    }

    /** Returns the list access index text (e.g. "5" from "[5]") or null if it cannot be found */
    public static @Nullable Integer getListAccessorIndex(@Nonnull final BashpileParser.ListAccessExpressionContext ctx) {
        if (ctx.listAccess() == null || ctx.listAccess().NumberValues() == null) {
            return null;
        }
        final String minus = ctx.listAccess().Minus() != null ? "-" : "";
        return Integer.parseInt(minus + ctx.listAccess().NumberValues().getText());
    }
}
