package com.bashpile;

import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

/** Antlr4 calls these methods and we create an AST */
public class BashpileVisitor extends BashpileParserBaseVisitor<List<Integer>> implements Closeable {
    private final Map<String, Integer> memory = new HashMap<>();

    private final PrintStream output;

    public BashpileVisitor(OutputStream os) {
        output = new PrintStream(os);
    }

    // visitors

    @Override
    public List<Integer> visit(ParseTree tree) {
        List<Integer> ret = super.visit(tree);
        output.flush();
        return ret;
    }

    @Override
    public List<Integer> visitProg(BashpileParser.ProgContext ctx) {
        final List<Integer> ret = new LinkedList<>();
        for (BashpileParser.StatContext lineContext : ctx.stat()) {
            ret.addAll(visit(lineContext));
        }
        return ret;
    }

    @Override
    public List<Integer> visitAssign(BashpileParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        List<Integer> rightSide = visit(ctx.expr());

        int value = rightSide.get(0);
        memory.put(id, value);
        return rightSide;
    }

    @Override
    public List<Integer> visitPrintExpr(BashpileParser.PrintExprContext ctx) {
        List<Integer> value = visit(ctx.expr());
        print(value);
        return value;
    }

    @Override
    public List<Integer> visitInt(BashpileParser.IntContext ctx) {
        return List.of(Integer.valueOf(ctx.INT().getText()));
    }

    @Override
    public List<Integer> visitId(BashpileParser.IdContext ctx) {
        String id = ctx.ID().getText();
        if (memory.containsKey(id)) {
            return List.of(memory.get(id));
        }
        throw new RuntimeException("ID %s not found".formatted(id));
    }

    @Override
    public List<Integer> visitMulDiv(BashpileParser.MulDivContext ctx) {
        int left = visit(ctx.expr(0)).get(0);
        int right = visit(ctx.expr(1)).get(0);
        boolean multiply = ctx.op.getType() == BashpileParser.MUL;
        if (multiply) {
            return List.of(left * right);
        } // else divide
        return List.of(left / right);
    }

    @Override
    public List<Integer> visitAddSub(BashpileParser.AddSubContext ctx) {
        int left = visit(ctx.expr(0)).get(0);
        int right = visit(ctx.expr(1)).get(0);
        boolean add = ctx.op.getType() == BashpileParser.ADD;
        if (add) {
            return List.of(left + right);
        } // else subtract
        return List.of(left - right);
    }

    @Override
    public List<Integer> visitParens(BashpileParser.ParensContext ctx) {
        return visit(ctx.expr());
    }

    // helpers

    protected void print(List<Integer> integers) {
        integers.stream().map(Object::toString).forEach(this::printLn);
    }

    protected void printLn(String line) {
        output.println(line);
        System.out.println(line);
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
