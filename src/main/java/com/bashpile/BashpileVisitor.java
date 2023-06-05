package com.bashpile;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Antlr4 calls these methods.  Both walks the parse tree and buffers all output.
 */
public class BashpileVisitor extends BashpileParserBaseVisitor<Void> implements Closeable {
    private final Map<String, Integer> memory = new HashMap<>();

    private final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(1024);

    private final PrintStream output;

    // occasionally need to suppress writing to output
    private boolean bashOutputting = true;

    public BashpileVisitor() {
        output = new PrintStream(byteStream);
    }

    // visitors

    @Override
    public Void visit(ParseTree tree) {
        super.visit(tree);
        output.flush();
        return null;
    }

    @Override
    public Void visitProg(BashpileParser.ProgContext ctx) {
        for (BashpileParser.StatContext lineContext : ctx.stat()) {
            visit(lineContext);
        }
        return null;
    }

    @Override
    public Void visitAssign(BashpileParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        bashOutputting = false; // no better ideas, it's a bit of a hack
        visit(ctx.expr()); // verify all ids are defined
        bashOutputting = true;
        String rightSide = ctx.expr().getText();
        output.printf("export %s=%s\n", id, rightSide);
        memory.put(id, 1);
        return null;
    }

    @Override
    public Void visitPrintExpr(BashpileParser.PrintExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Void visitId(BashpileParser.IdContext ctx) {
        String id = ctx.ID().getText();
        if (memory.containsKey(id)) {
            return null;
        }
        throw new RuntimeException("ID %s not found".formatted(id));
    }

    @Override
    public Void visitMulDiv(BashpileParser.MulDivContext ctx) {
        if (bashOutputting) {
            output.printf("bc <<< \"%s\"\n", getBashText(ctx));
        }
        visit(ctx.expr(0));
        visit(ctx.expr(1));
        return null;
    }

    @Override
    public Void visitAddSub(BashpileParser.AddSubContext ctx) {
        if (bashOutputting) {
            output.printf("bc <<< \"%s\"\n", getBashText(ctx));
        }
        visit(ctx.expr(0));
        visit(ctx.expr(1));
        return null;
    }

    private String getBashText(RuleContext ctx) {
        if (ctx.getChildCount() == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree currentChild = ctx.getChild(i);
            boolean isId = currentChild instanceof BashpileParser.IdContext;
            if (isId) {
                builder.append('$');
            }
            builder.append(currentChild.getText());
        }

        return builder.toString();
    }

    @Override
    public Void visitParens(BashpileParser.ParensContext ctx) {
        return visit(ctx.expr());
    }

    public String getOutput(ParseTree parseTree) {
        visit(parseTree);
        return byteStream.toString();
    }

    @Override
    public void close() {
        output.close();
    }
}
