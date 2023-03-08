package com.bashpile;

import java.util.HashMap;
import java.util.Map;

public class BashpileVisitor extends BashpileParserBaseVisitor<Integer> {

    Map<String, Integer> memory = new HashMap<>();

    @Override
    public Integer visitAssign(BashpileParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        int value = visit(ctx.expr());
        memory.put(id, value);
        return value;
    }

    @Override
    public Integer visitPrintExpr(BashpileParser.PrintExprContext ctx) {
        Integer value = visit(ctx.expr());
        System.out.println(value);
        return 0;
    }

    @Override
    public Integer visitInt(BashpileParser.IntContext ctx) {
        return Integer.valueOf(ctx.INT().getText());
    }

    @Override
    public Integer visitId(BashpileParser.IdContext ctx) {
        String id = ctx.ID().getText();
        if (memory.containsKey(id)) {
            return memory.get(id);
        }
        throw new RuntimeException("ID %s not found".formatted(id));
    }

    @Override
    public Integer visitMulDiv(BashpileParser.MulDivContext ctx) {
        int left = visit(ctx.expr(0));
        int right = visit(ctx.expr(1));
        if (ctx.op.getType() == BashpileParser.MUL) {
            return left * right;
        } // else divide
        return left / right;
    }

    @Override
    public Integer visitAddSub(BashpileParser.AddSubContext ctx) {
        int left = visit(ctx.expr(0));
        int right = visit(ctx.expr(1));
        if (ctx.op.getType() == BashpileParser.ADD) {
            return left + right;
        } // else subtract
        return left - right;
    }

    @Override
    public Integer visitParens(BashpileParser.ParensContext ctx) {
        return visit(ctx.expr());
    }
}
