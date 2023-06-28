package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileParserBaseVisitor;
import com.bashpile.exceptions.TypeError;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bashpile.Asserts.assertTextBlock;
import static com.bashpile.engine.Translation.toStringTranslation;
import static java.util.Objects.requireNonNullElse;

/**
 * Antlr4 calls these methods.
 * 
 * @see com.bashpile.AntlrUtils#parse(InputStream)
 */
public class BashpileVisitor extends BashpileParserBaseVisitor<Translation> {

    private final TranslationEngine translator;

    private final Logger log = LogManager.getLogger(BashpileVisitor.class);

    private ParserRuleContext contextRoot;

    // TODO make sure this works quickly with large programs (100+ functions)
    private final Map<String, List<Types>> functionArgumentTypes = HashMap.newHashMap(10);

    public BashpileVisitor(final TranslationEngine translator) {
        this.translator = translator;
        translator.setVisitor(this);
    }

    /**
     * Do not modify.
     *
     * @return The prog context.
     */
    public ParserRuleContext getContextRoot() {
        // pass-by-reference because a deep copy for a non-serializable object is a nightmare
        return contextRoot;
    }

    // visitors

    @Override
    public Translation visitProg(final BashpileParser.ProgContext ctx) {
        // save root for later usage
        contextRoot = ctx;

        // prepend strictMode text to the statement results
        final String header = translator.strictModeHeader().text();
        assertTextBlock(header);
        String translatedTextBlock = ctx.stmt().stream()
                .map(this::visit)
                .map(Translation::text)
                .collect(Collectors.joining());
        assertTextBlock(translatedTextBlock);

        final String importLibs = translator.imports().text();

        return toStringTranslation(header, importLibs, translatedTextBlock);
    }

    // visit statements

    @Override
    public Translation visitBlankStmt(BashpileParser.BlankStmtContext ctx) {
        // was returning "\r\n" without an override
        return toStringTranslation("\n");
    }

    @Override
    public Translation visitExprStmt(final BashpileParser.ExprStmtContext ctx) {
        return visit(ctx.expr()).add("\n");
    }

    @Override
    public Translation visitAssignStmt(final BashpileParser.AssignStmtContext ctx) {
        return translator.assign(ctx);
    }

    @Override
    public Translation visitPrintStmt(final BashpileParser.PrintStmtContext ctx) {
        return translator.print(ctx);
    }

    @Override
    public Translation visitFunctionForwardDeclStmt(final BashpileParser.FunctionForwardDeclStmtContext ctx) {
        return translator.functionForwardDecl(ctx);
    }

    @Override
    public Translation visitFunctionDeclStmt(final BashpileParser.FunctionDeclStmtContext ctx) {
        final String functionName = ctx.typedId().ID().getText();
        // TODO require strong typing, impl all types in parser
        final List<Types> typeList = ctx.paramaters().typedId()
                .stream().map(Types::valueOf).collect(Collectors.toList());
        functionArgumentTypes.put(functionName, typeList);
        return translator.functionDecl(ctx);
    }

    @Override
    public Translation visitAnonBlockStmt(final BashpileParser.AnonBlockStmtContext ctx) {
        return translator.anonBlock(ctx);
    }

    @Override
    public Translation visitBlock(final BashpileParser.BlockContext ctx) {
        return Translation.empty;  // pure comments for now
    }

    @Override
    public Translation visitReturnRule(final BashpileParser.ReturnRuleContext ctx) {
        return translator.returnRule(ctx);
    }

    // visit expressions

    @Override
    public Translation visitCalcExpr(final BashpileParser.CalcExprContext ctx) {
        log.trace("In Calc with {} children", ctx.children.size());
        return translator.calc(ctx);
    }

    @Override
    public Translation visitFunctionCallExpr(final BashpileParser.FunctionCallExprContext ctx) {
        final String functionName = ctx.ID().getText();
        final List<Types> actualTypes = ctx.arglist() != null
                ? ctx.arglist().expr().stream().map(x -> x.type).collect(Collectors.toList())
                : List.of();
        final List<Types> expectedTypes = requireNonNullElse(functionArgumentTypes.get(functionName), List.of());
        // TODO move into Asserts, make more tests, use comparator?
        // check if the argument lengths match
        boolean typesMatch = actualTypes.size() == expectedTypes.size();
        // if they match iterate over both lists
        if (typesMatch) {
            for (int i = 0; i < actualTypes.size(); i++) {
                Types expected = expectedTypes.get(i);
                Types actual = actualTypes.get(i);
                // the types match if they are equal, UNDEF matches everything, and FLOAT matches INT ('type coercion')
                typesMatch &= expected.equals(actual)
                        || expected.equals(Types.UNDEF)
                        || (expected.equals(Types.FLOAT) && actual.equals(Types.INT));
            }
        }
        if (!typesMatch) {
            throw new TypeError("Expected %s %s but was %s %s on Bashpile Line %s"
                    .formatted(functionName, expectedTypes, functionName, actualTypes, ctx.start.getLine()));
        }
        return translator.functionCall(ctx);
    }

    @Override
    public Translation visitParensExpr(final BashpileParser.ParensExprContext ctx) {
        return translator.calc(ctx);
    }

    @Override
    public Translation visitIdExpr(final BashpileParser.IdExprContext ctx) {
        return toStringTranslation(ctx.ID().getText());
    }

    @Override
    public Translation visitNumberExpr(final BashpileParser.NumberExprContext ctx) {
        return toStringTranslation(ctx.getText());
    }

    @Override
    public Translation visitTerminal(final TerminalNode node) {
        return toStringTranslation(node.getText());
    }
}
