package com.bashpile.engine;

import javax.annotation.Nonnull;

import com.bashpile.BashpileParser;

/**
 * Methods translate small parser rules (e.g. statements and expressions) to the target language.
 * Exists to allow for other implementations in the future (e.g. sh, ksh, Bash3).
 * Currently only has {@code BashTranslationEngine}.
 * @see BashTranslationEngine
 */
public interface TranslationEngine {

    /**
     * Our {@link BashpileVisitor} needs a TranslationEngine and we need a BashpileVisitor.
     * <br>
     * So you make a TranslationEngine, pass to the BashpileVisitor then set the visitor.
     */
    void setVisitor(final BashpileVisitor visitor);

    /**
     * Some expressions need a statement executed beforehand, after the expression is translated this buffer is filled.
     * Calling this also drains the buffer.
     */
    @Nonnull Translation getExpressionSetup();

    // headers

    /** For emitting where the translated script comes from and when it was generated */
    Translation originHeader();

    /**
     * For using the equivalent of strict mode
     */
    Translation strictModeHeader();

    // statement translations

    Translation importStatement(BashpileParser.ImportStatementContext ctx);

    /** Translates a while loop */
    Translation whileStatement(final BashpileParser.WhileStatementContext ctx);

    /** Translates a forward declaration */
    Translation functionForwardDeclarationStatement(final BashpileParser.FunctionForwardDeclarationStatementContext ctx);

    /** Translates a function declaration */
    Translation functionDeclarationStatement(final BashpileParser.FunctionDeclarationStatementContext ctx);

    /** Translates an anonymous block */
    Translation anonymousBlockStatement(final BashpileParser.AnonymousBlockStatementContext ctx);

    /** Translates a conditional (if, else if, else block) */
    Translation conditionalStatement(final BashpileParser.ConditionalStatementContext ctx);

    Translation switchStatement(final BashpileParser.SwitchStatementContext ctx);

    /** Translates an assignment */
    Translation assignmentStatement(final BashpileParser.AssignmentStatementContext ctx);

    /** Translates a reassignment */
    Translation reassignmentStatement(final BashpileParser.ReassignmentStatementContext ctx);

    /** Translates a print */
    Translation printStatement(final BashpileParser.PrintStatementContext ctx);

    /** Translates an expression */
    Translation expressionStatement(final BashpileParser.ExpressionStatementContext ctx);

    /**
     * Translates a return.
     * It's called a psudo-statement because it is logically a statement but not technically one in the parser
     */
    Translation returnPsudoStatement(final BashpileParser.ReturnPsudoStatementContext ctx);

    // expression translations

    /** For Increment and Decrement such as `i++` */
    Translation unaryPostCrementExpression(final BashpileParser.UnaryPostCrementExpressionContext ctx);

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

    /**
     * Translates a relational or equality with a single argument (e.g. 'not'), called a primary in Bash
     * @see <a href=https://tldp.org/LDP/Bash-Beginners-Guide/html/sect_07_01.html>Primaries</a>
     */
    Translation unaryPrimaryExpression(final BashpileParser.UnaryPrimaryExpressionContext ctx);

    /**
     * Translates a relational or equality with two arguments (e.g. '=='), called a primary in Bash
     * @see <a href=https://tldp.org/LDP/Bash-Beginners-Guide/html/sect_07_01.html>Primaries</a>
     */
    Translation binaryPrimaryExpression(final BashpileParser.BinaryPrimaryExpressionContext ctx);

    Translation combiningExpression(final BashpileParser.CombiningExpressionContext ctx);

    Translation argumentsBuiltinExpression(BashpileParser.ArgumentsBuiltinExpressionContext ctx);

    Translation listOfBuiltinExpression(final BashpileParser.ListOfBuiltinExpressionContext ctx);

    /** Translates IDs */
    Translation idExpression(final BashpileParser.IdExpressionContext ctx);

    /** List access like listName[num] */
    Translation listIndexExpression(final BashpileParser.ListAccessExpressionContext ctx);

    // expression helper translations

    /** Translates shell strings */
    Translation shellString(final BashpileParser.ShellStringContext ctx);
}
