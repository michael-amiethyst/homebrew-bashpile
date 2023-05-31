package com.bashpile;

import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BashpileVisitor extends BashpileParserBaseVisitor<Integer> implements Closeable {
    private final Map<String, Integer> memory = new HashMap<>();

    private final PrintStream output;

    public BashpileVisitor(OutputStream os) {
        output = new PrintStream(os);
    }

    // visitors

    @Override
    public Integer visit(ParseTree tree) {
        Integer ret = super.visit(tree);
        output.flush();
        return ret;
    }

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
        print(value.toString());
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

    // helpers

    protected void print(String line) {
        output.println(line);
        System.out.println(line);
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
