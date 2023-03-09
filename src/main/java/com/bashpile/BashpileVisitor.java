package com.bashpile;

import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Antlr4 calls these methods and we create an AST */
public class BashpileVisitor extends BashpileParserBaseVisitor<AstNode<List<Integer>>> implements Closeable {
    private final Map<String, Integer> memory = new HashMap<>();

    private final PrintStream output;

    public BashpileVisitor(OutputStream os) {
        output = new PrintStream(os);
    }

    // visitors

    @Override
    public AstNode visit(ParseTree tree) {
        AstNode ret = super.visit(tree);
        output.flush();
        return ret;
    }

    @Override
    public AstNode<List<Integer>> visitAssign(BashpileParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        AstNode<List<Integer>> rightSideNode = visit(ctx.expr());

        return new AstNode<>(() -> {
            int value = rightSideNode.getValue().get(0);
            memory.put(id, value);
            return new AstNode<>(List.of(value));
        });
    }

    @Override
    public AstNode<List<Integer>> visitPrintExpr(BashpileParser.PrintExprContext ctx) {
        Integer value = visit(ctx.expr()).getValue().get(0);
        return new AstNode<>(() -> {
            System.out.println(value);
            return new AstNode<>(List.of(value));
        });
    }

    @Override
    public AstNode<List<Integer>> visitInt(BashpileParser.IntContext ctx) {
        return new AstNode<>(List.of(Integer.valueOf(ctx.INT().getText())));
    }

    @Override
    public AstNode<List<Integer>> visitId(BashpileParser.IdContext ctx) {
        String id = ctx.ID().getText();
        return new AstNode<>(() -> {
            if (memory.containsKey(id)) {
                return new AstNode<>(List.of(memory.get(id)));
            }
            // TODO verify
            throw new RuntimeException("ID %s not found".formatted(id));
        });
    }

    @Override
    public AstNode<List<Integer>> visitMulDiv(BashpileParser.MulDivContext ctx) {
        int left = visit(ctx.expr(0)).getValue().get(0);
        int right = visit(ctx.expr(1)).getValue().get(0);
        boolean multiply = ctx.op.getType() == BashpileParser.MUL;
        return new AstNode<>(() -> {
            if (multiply) {
                return new AstNode<>(List.of(left * right));
            } // else divide
            return new AstNode<>(List.of(left / right));
        });
    }

    @Override
    public AstNode<List<Integer>> visitAddSub(BashpileParser.AddSubContext ctx) {
        int left = visit(ctx.expr(0)).getValue().get(0);
        int right = visit(ctx.expr(1)).getValue().get(0);
        boolean add = ctx.op.getType() == BashpileParser.ADD;
        return new AstNode<>(() -> {
            if (add) {
                return new AstNode<>(List.of(left + right));
            } // else subtract
            return new AstNode<>(List.of(left - right));
        });
    }

    @Override
    public AstNode<List<Integer>> visitParens(BashpileParser.ParensContext ctx) {
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
