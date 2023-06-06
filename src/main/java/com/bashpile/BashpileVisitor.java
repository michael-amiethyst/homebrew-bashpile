package com.bashpile;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.PrintStream;

/**
 * Antlr4 calls these methods.  Both walks the parse tree and buffers all output.
 */
public class BashpileVisitor extends BashpileParserBaseVisitor<String> implements Closeable {

    private final ByteArrayOutputStream translationBackingStore = new ByteArrayOutputStream(1024);

    private final PrintStream translation = new PrintStream(translationBackingStore);

    // visitors

    @Override
    public String visit(ParseTree tree) {
        super.visit(tree);
        translation.flush();
        return translationBackingStore.toString();
    }

    @Override
    public String visitProg(BashpileParser.ProgContext ctx) {
        translation.print("set -euo pipefail\n");
        translation.print("export IFS=$'\\n\\t'\n");
        super.visitProg(ctx);
        return null;
    }

    @Override
    public String visitAssign(BashpileParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        String rightSide = ctx.expr().getText();
        translation.printf("export %s=%s\n", id, rightSide);
        return null;
    }

    @Override
    public String visitMulDiv(BashpileParser.MulDivContext ctx) {
        translation.printf("bc <<< \"%s\"\n", getBashText(ctx));
        return null;
    }

    @Override
    public String visitAddSub(BashpileParser.AddSubContext ctx) {
        translation.printf("bc <<< \"%s\"\n", getBashText(ctx));
        return null;
    }

    // TODO simplify with parser actions
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
    public void close() {
        translation.close();
    }
}
