package com.bashpile;

import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.*;

/**
 * Antlr4 calls these methods.  Both walks the parse tree and buffers all output.
 */
public class BashpileVisitor extends BashpileParserBaseVisitor<List<Integer>> implements Closeable {
    private final Map<String, Integer> memory = new HashMap<>();

    private final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(1024);

    private final PrintStream output;

    private boolean bashOutputting = true;

    public BashpileVisitor() {
        output = new PrintStream(byteStream);
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
        bashOutputting = false; // TODO fix this ugly hack
        List<Integer> rightRet = visit(ctx.expr()); // verify all ids are defined
        bashOutputting = true;
        String rightSide = ctx.expr().getText();
        output.printf("export %s=%s\n", id, rightSide);
        memory.put(id, rightRet.get(0));
        return rightRet;
    }

    @Override
    public List<Integer> visitPrintExpr(BashpileParser.PrintExprContext ctx) {
        List<Integer> value = visit(ctx.expr());
        //print(value);
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
        // get all id's, make sure they are defined
        output.printf("bc <<< \"%s\"\n", ctx.getText());

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
        // find all IDs in ctx
        if (bashOutputting) {
            ParseTree leftParseTree = ctx.children.get(0);
            String equation = ctx.getText();
            // TODO stub
            if ("someVar".compareTo(leftParseTree.getText()) == 0) {
                equation = equation.replace("someVar", "$someVar");
            }
            output.printf("bc <<< \"%s\"\n", equation);
        }
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

    public String getOutput(ParseTree parseTree) {
        visit(parseTree);
        return byteStream.toString();
    }

    @Override
    public void close() {
        output.close();
    }
}
