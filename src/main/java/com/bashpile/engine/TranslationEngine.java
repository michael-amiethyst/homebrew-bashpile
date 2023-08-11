package com.bashpile.engine;

import com.bashpile.BashpileParser;

/**
 * Methods translate small parser rules (e.g. statements and expressions) to the target language.
 */
public interface TranslationEngine {

    /**
     * Our {@link BashpileVisitor} needs a TranslationEngine and we need a BashpileVisitor.
     * <br>
     * So you make a TranslationEngine, pass to the BashpileVisitor then set the visitor.
     */
    void setVisitor(final BashpileVisitor visitor);

    // headers

    /** For emitting where the translated script comes from and when it was generated */
    Translation originHeader();

    // TODO https://disconnected.systems/blog/another-bash-strict-mode/ with set -E
    /**
     * For using the equivalent of strict mode
     * @see <a href=http://redsymbol.net/articles/unofficial-bash-strict-mode/>Unofficial Bash Strict Mode</a>
     */
    Translation strictModeHeader();

    /** To source our bundled libraries */
    Translation importsHeaders();

    // statement translations

    /** Translates an assignment */
    Translation assignmentStatement(final BashpileParser.AssignmentStatementContext ctx);

    /** Translates a reassignment */
    Translation reassignmentStatement(final BashpileParser.ReassignmentStatementContext ctx);

    /** Translates a print */
    Translation printStatement(final BashpileParser.PrintStatementContext ctx);

    /** Translates a forward declaration */
    Translation functionForwardDeclarationStatement(final BashpileParser.FunctionForwardDeclarationStatementContext ctx);

    /** Translates a function declaration */
    Translation functionDeclarationStatement(final BashpileParser.FunctionDeclarationStatementContext ctx);

    /** Translates an anonymous block */
    Translation anonymousBlockStatement(final BashpileParser.AnonymousBlockStatementContext ctx);

    /** Translates a creates statement, similar to a try-with-resources block in Java */
    Translation createsStatement(final BashpileParser.CreatesStatementContext ctx);

    /** Translates an expression */
    Translation expressionStatement(final BashpileParser.ExpressionStatementContext ctx);

    /**
     * Translates a return.
     * It's called a psudo-statement because it is logically a statement but not technically one in the parser
     */
    Translation returnPsudoStatement(final BashpileParser.ReturnPsudoStatementContext ctx);

    // expression translations

    /**
     * Translates a type-cast.
     * Typically implemented in the implementing class, like Typescript adds types to JavaScript
     */
    Translation typecastExpression(final BashpileParser.TypecastExpressionContext ctx);

    /** Translates a function call */
    Translation functionCallExpression(final BashpileParser.FunctionCallExpressionContext ctx);

    /** Translates a parenthesis */
    Translation parenthesisExpression(final BashpileParser.ParenthesisExpressionContext ctx);

    /** Translates a calculation, including adding strings */
    Translation calculationExpression(final BashpileParser.CalculationExpressionContext ctx);

    /** Translates IDs */
    Translation idExpression(final BashpileParser.IdExpressionContext ctx);

    // expression helper translations

    /** Translates shell strings */
    Translation shellString(final BashpileParser.ShellStringContext ctx);

    /** Translates inlines */
    Translation inline(final BashpileParser.InlineContext ctx);
}
