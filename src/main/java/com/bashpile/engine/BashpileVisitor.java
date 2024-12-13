package com.bashpile.engine;

import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bashpile.BashpileParser;
import com.bashpile.BashpileParserBaseVisitor;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.BashpileUncheckedAssertionException;
import com.bashpile.exceptions.BashpileUncheckedException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.bashpile.engine.Translation.NEWLINE;
import static com.bashpile.engine.strongtypes.TranslationMetadata.NORMAL;

/**
 * Antlr4 calls these methods.
 */
public class BashpileVisitor extends BashpileParserBaseVisitor<Translation> {

    @Nonnull
    private final TranslationEngine translator;

    private final Logger log = LogManager.getLogger(BashpileVisitor.class);

    private ParserRuleContext contextRoot;

    public BashpileVisitor(@Nonnull final TranslationEngine translator) {
        this.translator = translator;
        translator.setVisitor(this);
    }

    /**
     * Do not modify.  Will be null before the first visit.
     *
     * @return The root of the Bashpile context tree.
     */
    public @Nullable ParserRuleContext getContextRoot() {
        // pass-by-reference because a deep copy for a non-serializable object is a nightmare
        return contextRoot;
    }

    // visitors

    /**
     * This is the default visitor.
     * <br>
     * {@inheritDoc}
     *
     * @param node The {@link RuleNode} whose children should be visited.
     * @return A Translation aggregating all visited children.
     */
    @Override
    public @Nonnull Translation visitChildren(RuleNode node) {
        final RuleContext ctx = node.getRuleContext();
        final Optional<Translation> optional = IntStream.range(0, ctx.getChildCount())
                .mapToObj(ctx::getChild)
                .map(this::visit)
                .reduce(Translation::add);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            // throw custom exception
            final String message = "Exception during visitChildren for node [%s] with %d children"
                    .formatted(node.getText(), node.getChildCount());
            throw new BashpileUncheckedException(message);
        }
    }

    @Override
    public @Nonnull Translation visitProgram(@Nonnull final BashpileParser.ProgramContext ctx) {
        // save root for later usage
        contextRoot = ctx;

        final Translation statementTranslation = ctx.statement().stream()
                .map(this::visit)
                .map(Translation::assertEmptyPreamble)
                .reduce(Translation::add)
                .orElseThrow();

        // add header, libs and statements
        return translator.originHeader()
                .add(translator.strictModeHeader())
                .add(translator.importsHeaders())
                .add(statementTranslation);
    }

    // visit statements

    @Override
    public Translation visitWhileStatement(BashpileParser.WhileStatementContext ctx) {
        return translator.whileStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitFunctionForwardDeclarationStatement(
            @Nonnull final BashpileParser.FunctionForwardDeclarationStatementContext ctx) {
        return translator.functionForwardDeclarationStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitFunctionDeclarationStatement(
            @Nonnull final BashpileParser.FunctionDeclarationStatementContext ctx) {
        return translator.functionDeclarationStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitAnonymousBlockStatement(
            @Nonnull final BashpileParser.AnonymousBlockStatementContext ctx) {
        return translator.anonymousBlockStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitConditionalStatement(BashpileParser.ConditionalStatementContext ctx) {
        return translator.conditionalStatement(ctx);
    }

    @Override
    public Translation visitSwitchStatement(BashpileParser.SwitchStatementContext ctx) {
        return translator.switchStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitAssignmentStatement(@Nonnull final BashpileParser.AssignmentStatementContext ctx) {
        return translator.assignmentStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitReassignmentStatement(@Nonnull BashpileParser.ReassignmentStatementContext ctx) {
        return translator.reassignmentStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitPrintStatement(@Nonnull final BashpileParser.PrintStatementContext ctx) {
        return translator.printStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitExpressionStatement(@Nonnull final BashpileParser.ExpressionStatementContext ctx) {
        return translator.expressionStatement(ctx);
    }

    @Override
    public @Nonnull Translation visitBlankStmt(@Nonnull BashpileParser.BlankStmtContext ctx) {
        // will return "\r\n" on Windows without an override
        return NEWLINE;
    }

    @Override
    public @Nonnull Translation visitReturnPsudoStatement(
            @Nonnull final BashpileParser.ReturnPsudoStatementContext ctx) {
        return translator.returnPsudoStatement(ctx);
    }

    // visit expressions

    @Override
    public Translation visitUnaryPostCrementExpression(BashpileParser.UnaryPostCrementExpressionContext ctx) {
        return translator.unaryPostCrementExpression(ctx);
    }

    @Override
    public @Nonnull Translation visitTypecastExpression(BashpileParser.TypecastExpressionContext ctx) {
        return translator.typecastExpression(ctx);
    }

    @Override
    public @Nonnull Translation visitFunctionCallExpression(
            @Nonnull final BashpileParser.FunctionCallExpressionContext ctx) {
        return translator.functionCallExpression(ctx);
    }

    // visit operator expressions

    @Override
    public @Nonnull Translation visitParenthesisExpression(
            @Nonnull final BashpileParser.ParenthesisExpressionContext ctx) {
        return translator.parenthesisExpression(ctx);
    }

    @Override
    public @Nonnull Translation visitCalculationExpression(
            @Nonnull final BashpileParser.CalculationExpressionContext ctx) {
        log.trace("In Calc with {} children", ctx.children.size());
        return translator.calculationExpression(ctx);
    }

    @Override
    public @Nonnull Translation visitUnaryPrimaryExpression(BashpileParser.UnaryPrimaryExpressionContext ctx) {
        return translator.unaryPrimaryExpression(ctx);
    }

    @Override
    public Translation visitBinaryPrimaryExpression(BashpileParser.BinaryPrimaryExpressionContext ctx) {
        return translator.binaryPrimaryExpression(ctx);
    }

    @Override
    public Translation visitCombiningExpression(BashpileParser.CombiningExpressionContext ctx) {
        return translator.combiningExpression(ctx);
    }

    @Override
    public Translation visitArgumentsBuiltinExpression(BashpileParser.ArgumentsBuiltinExpressionContext ctx) {
        return translator.argumentsBuiltinExpression(ctx);
    }

    @Override
    public Translation visitListOfBuiltinExpression(BashpileParser.ListOfBuiltinExpressionContext ctx) {
        return translator.listOfBuiltinExpression(ctx);
    }

    // visit type expressions

    @Override
    public @Nonnull Translation visitNumberExpression(@Nonnull final BashpileParser.NumberExpressionContext ctx) {
        final TerminalNode number = ctx.Number();
        if (number == null) {
            throw new BashpileUncheckedAssertionException("No number in number expression.  Bad parse?");
        }
        final Type type = Type.parseNumberString(number.getText());
        return new Translation(ctx.getText(), type, NORMAL);
    }

    @Override
    public Translation visitLiteralExpression(BashpileParser.LiteralExpressionContext ctx) {
        Type type;
        if (ctx.literal().String() != null) {
            type = Type.STR_TYPE;
        } else if (ctx.literal().Number() != null) {
            type = Type.parseNumberString(ctx.literal().Number().getText());
        } else if (ctx.literal().Bool() != null) {
            type = Type.BOOL_TYPE;
        } else if (ctx.literal().Empty() != null) {
            type = Type.EMPTY_TYPE;
        } else {
            throw new BashpileUncheckedException("Unexpected literal");
        }
        return new Translation(ctx.getText(), type, NORMAL);
    }

    /**
     * Put variable into ${}, e.g. "var" becomes "${var}".
     * */
    @Override
    public @Nonnull Translation visitIdExpression(@Nonnull final BashpileParser.IdExpressionContext ctx) {
        return translator.idExpression(ctx);
    }

    @Override
    public Translation visitListAccessExpression(BashpileParser.ListAccessExpressionContext ctx) {
        return translator.listIndexExpression(ctx);
    }

    /** Default type is STR */
    @Override
    public @Nonnull Translation visitTerminal(@Nonnull final TerminalNode node) {
        // may or may not be multi-line
        return new Translation(node.getText(), Type.STR_TYPE, NORMAL);
    }

    // expression helper rules

    @Override
    public Translation visitShellString(BashpileParser.ShellStringContext ctx) {
        return translator.shellString(ctx);
    }

    @Override
    public Translation visitShellLineStatement(BashpileParser.ShellLineStatementContext ctx) {
        return visit(ctx.ShellLine()).add(NEWLINE);
    }
}
