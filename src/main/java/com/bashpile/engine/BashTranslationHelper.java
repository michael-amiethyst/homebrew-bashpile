package com.bashpile.engine;

import com.bashpile.Asserts;
import com.bashpile.BashpileParser;
import com.bashpile.engine.strongtypes.SimpleType;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.TypeError;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.bashpile.engine.BashTranslationEngine.TAB;
import static com.bashpile.engine.Translation.UNKNOWN_TRANSLATION;
import static com.bashpile.engine.Translation.toStringTranslation;
import static com.bashpile.engine.strongtypes.TranslationMetadata.CALCULATION;
import static com.bashpile.engine.strongtypes.Type.*;

/**
 * Helper methods to {@link BashTranslationEngine}.
 */
public class BashTranslationHelper {

    private static final Pattern escapedNewline = Pattern.compile("\\\\\\r?\\n\\s*");

    private static final Logger LOG = LogManager.getLogger(BashTranslationHelper.class);

    // static methods

    /**
     * Ensures there is only one copy of a given modifier.  Returns "-x " or "" as a Translation.
     */
    /* package */ static @Nonnull Translation visitModifiers(@Nullable List<BashpileParser.ModifierContext> ctx) {
        if (ctx == null || ctx.isEmpty()) {
            return UNKNOWN_TRANSLATION;
        }
        final long lineNumber = lineNumber(ctx.get(0));

        // check readonly declarations
        final long readonlys = ctx.stream().filter(typeCtx -> typeCtx.Readonly() != null).count();
        Asserts.assertNotOver(1, readonlys, "Can only have one readonly statement, line " + lineNumber);

        // check declare declarations
        final long exports = ctx.stream().filter(typeCtx -> typeCtx.Exported() != null).count();
        Asserts.assertNotOver(1, exports, "Can only have one export statement, line " + lineNumber);
        if (exports >= 1) {
            return toStringTranslation("-x ");
        }
        return UNKNOWN_TRANSLATION;
    }

    /* package */ static @Nonnull Translation visitBodyStatements(
            @Nonnull final List<BashpileParser.StatementContext> statements,
            @Nonnull final BashpileVisitor visitor) {
        return statements.stream()
                .map(visitor::visit)
                .map(tr -> tr.lambdaBodyLines(str -> TAB + str))
                .reduce(Translation::add)
                .orElseThrow();
    }

    /* package */ static @Nonnull Translation createCommentTranslation(@Nonnull final String name, final int lineNumber) {
        return toStringTranslation("# %s, Bashpile line %d\n".formatted(name, lineNumber));
    }

    /* package */ static @Nonnull Translation subcommentTranslationOrDefault(
            final boolean subcommentNeeded, @Nonnull final String name) {
        if (subcommentNeeded) {
            return toStringTranslation("## %s\n".formatted(name));
        }
        return UNKNOWN_TRANSLATION;
    }

    /** Get the Bashpile script linenumber that ctx is found in. */
    /* package */ static int lineNumber(@Nonnull final ParserRuleContext ctx) {
        return ctx.start.getLine();
    }

    /**
     * Helper to {@link BashTranslationEngine#functionForwardDeclarationStatement(BashpileParser.FunctionForwardDeclarationStatementContext)}
     */
    /* package */ static @Nonnull ParserRuleContext getFunctionDeclCtx(
            @Nonnull final BashpileVisitor visitor,
            @Nonnull final BashpileParser.FunctionForwardDeclarationStatementContext ctx) {
        LOG.trace("In getFunctionDeclCtx");
        final String functionName = ctx.Id().getText();
        assert visitor.getContextRoot() != null;
        final Stream<ParserRuleContext> allContexts = stream(visitor.getContextRoot());
        final Predicate<ParserRuleContext> namesMatch =
                context -> {
                    final boolean isDeclaration = context instanceof BashpileParser.FunctionDeclarationStatementContext;
                    // is a function declaration and the names match
                    if (!isDeclaration) {
                        return false;
                    }
                    final BashpileParser.FunctionDeclarationStatementContext decl =
                            (BashpileParser.FunctionDeclarationStatementContext) context;
                    final boolean nameMatches = decl.Id().getText().equals(functionName);
                    return nameMatches && paramsMatch(decl.paramaters(), ctx.paramaters());
                };
        return allContexts
                .filter(namesMatch)
                .findFirst()
                .orElseThrow(
                        () -> new BashpileUncheckedException("No matching function declaration for " + functionName));
    }

    /** Concatenates inputs into stream */
    /* package */ static @Nonnull Stream<ParserRuleContext> streamContexts(
            @Nonnull final List<BashpileParser.StatementContext> statements,
            @Nullable final BashpileParser.ReturnPsudoStatementContext returnPsudoStatementContext) {
        // map of x to x needed for upcasting to parent type
        final Stream<ParserRuleContext> statementStream = statements.stream().map(x -> x);
        return returnPsudoStatementContext != null
                ? Stream.concat(statementStream, Stream.of(returnPsudoStatementContext))
                : statementStream;
    }

    /** Preforms any munging needed for the initial condition of an if statement (i.e. if GUARD ...). */
    /* package */ static Translation visitGuardingExpression(TerminalNode notNode, Translation expressionTranslation) {
        final Translation not = notNode != null ? toStringTranslation("! ") : UNKNOWN_TRANSLATION;
        expressionTranslation = Unwinder.unwindAll(expressionTranslation);
        if (expressionTranslation.type().isInt() && expressionTranslation.body().startsWith("$((")) {
            // strip initial $ for (( instead of $((
            expressionTranslation = expressionTranslation.lambdaBody(body -> body.substring(1));
        } else if (expressionTranslation.type().isNumeric()) {
            // to handle floats we use bc, but the test by default will be for if bc succeeded (had exit code 0)
            // so we need to explicitly check if the check returned true (1)
            expressionTranslation = expressionTranslation
                    .inlineAsNeeded(Unwinder::unwindAll)
                    .lambdaBody("[ \"$(bc <<< \"%s == 0\")\" -eq 1 ]"::formatted);
        }
        return not.add(expressionTranslation);
    }

    /** Removes escaped newlines and trailing spaces */
    /* package */ static @Nonnull Translation joinEscapedNewlines(@Nonnull final Translation tr) {
        return tr.lambdaBody(x -> escapedNewline.matcher(x).replaceAll(""));
    }

    /** Convert pair to (Java like) case translation, a pattern and statements */
    /* package */ static Translation toCase(Pair<Translation, List<Translation>> patternAndStatementPair) {
        final Translation pattern = patternAndStatementPair.getLeft();
        final Translation statements = patternAndStatementPair.getRight().stream()
                .map(tr -> tr.lambdaBodyLines(x -> "    " + x))
                .reduce(Translation::add)
                .orElseThrow();
        // second string is indented so will be inline with the ';;'
        final String template = """
                %s)
                %s
                    ;;
                """.formatted(pattern.body(), statements.body());
        return toStringTranslation(template)
                .lambdaBodyLines(x -> "    " + x)
                .addPreamble(pattern.preamble())
                .addPreamble(statements.preamble());
    }

    // typecast static methods
    // TODO not 0.22.0 - extract to its own class, "TypeCaster" with docs as following:
    // computations not checked for parsability or anything that starts with $ (Bash variable)
    // TODO consistent C style number casts, we can't check for correctness of non-literals (but we allow them)

    /* package */ static @Nonnull Translation typecastFromBool(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            @Nonnull final TypeError typecastError) {
        switch (castTo.mainType()) {
            case BOOL -> {}
            case STR -> expression = expression.quoteBody().type(STR_TYPE);
            // no cast to int, float or list
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static @Nonnull Translation typecastFromNumber(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            final int lineNumber,
            @Nonnull final TypeError typecastError
    ) {
        switch (castTo.mainType()) {
            case INT -> expression = typecastToInt(expression, lineNumber);
            case FLOAT -> expression = expression.type(FLOAT_TYPE);
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static @Nonnull Translation typecastFromInt(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        if (!expression.metadata().contains(CALCULATION)) {
            // parse expression to a BigInteger
            try {
                new BigInteger(expression.body());
            } catch (final NumberFormatException e) {
                // TODO allow for non-literals like for typecastFromFloat
                String message = "Couldn't parse '%s' to an INT.  " +
                        "Typecasts only work on literals, was this an ID or function call?";
                throw new TypeError(message.formatted(expression.body()), lineNumber);
            }
        }

        // Cast
        switch (castTo.mainType()) {
            case INT -> {}
            case FLOAT -> expression = expression.type(FLOAT_TYPE);
            case STR -> expression = expression.quoteBody().type(STR_TYPE);
            // no typecast to bool or list
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static @Nonnull Translation typecastFromFloat(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {

        // cast
        switch (castTo.mainType()) {
            case INT -> expression = typecastToInt(expression, lineNumber);
            case FLOAT -> {}
            case STR -> expression = expression.quoteBody().type(STR_TYPE);
            // no typecast to bool or list
            default -> throw typecastError;
        }
        return expression;
    }

    // helper
    private static @Nonnull Translation typecastToInt(@Nonnull Translation expression, final int lineNumber) {
        // parse expression as a BigDecimal to check for literal float
        expression = expression.unquoteBody();
        BigDecimal expressionValue = null;
        try {
            expressionValue = new BigDecimal(expression.body());
        } catch (final NumberFormatException e) {
            // expressionValue is still null if it is not a literal (e.g. variable or function call)
        }
        if (expressionValue != null) {
            return expression.body(expressionValue.toBigInteger().toString()).type(INT_TYPE).unquoteBody();
        } else {
            // if a variable reference then typecast to int (round down) with printf
            String varName = StringUtils.stripStart(expression.body(), "${");
            varName = StringUtils.stripEnd(varName, "}");
            if (!varName.matches("\\d")) {
                return expression.addPreamble("""
                                %s="$(printf '%%d' "%s" 2>/dev/null || true)"
                                """.formatted(varName, expression))
                        .type(INT_TYPE);
            } else {
                // trying to reassign $1, $2, etc
                // too complex to set an individual varable with the command 'set', throw
                final String message = "Could not cast $1, $2, etc.  Assign to a local variable and typecast that.";
                throw new TypeError(message, lineNumber);
            }
        }
    }

    /* package */ static @Nonnull Translation typecastFromStr(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        switch (castTo.mainType()) {
            case BOOL -> {
                expression = expression.unquoteBody();
                if (SimpleType.isNumberString(expression.body())) {
                    expression = typecastFromFloat(expression, castTo, lineNumber, typecastError);
                } else if (expression.body().equalsIgnoreCase("true")
                        || expression.body().equalsIgnoreCase("false")) {
                    expression = expression.body(expression.body().toLowerCase()).type(castTo);
                } else {
                    throw new TypeError("""
                            Could not cast STR to BOOL.
                            Only 'true' and 'false' allowed (capitalization ignored) or numbers for a C style cast.
                            Text was %s.""".formatted(expression.body()), lineNumber);
                }
            }
            case INT -> expression = typecastToInt(expression, lineNumber);
            case FLOAT -> {
                expression = expression.unquoteBody().type(castTo);
                // verify the body parses as a valid number for non-variables
                if (!expression.body().startsWith("$")) {
                    try {
                        SimpleType.parseNumberString(expression.body());
                    } catch (NumberFormatException e) {
                            throw new TypeError("""
                                    Could not cast STR to FLOAT.  Is not a FLOAT.  Text was %s."""
                                    .formatted(expression.body()), lineNumber);
                    }
                }
            }
            case STR -> {}
            // TODO allow typecasting to LIST for all conversions
            case LIST -> expression = expression.type(castTo); // trust the user, may be a bad idea
            default -> throw typecastError;
        }
        return expression;
    }

    /** Casts to a different kind of list (i.e. a different subtype), but no other conversions */
    /* package */ static @Nonnull Translation typecastFromList(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            @Nonnull final TypeError typecastError) {
        if (castTo.isList()) {
            // Allow a typecast to any subtype
            return expression.type(castTo);
        } else {
            // cannot cast to bool, int, float or str
            throw typecastError;
        }
    }

    /* package */ static @Nonnull Translation typecastFromUnknown(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        switch (castTo.mainType()) {
            case BOOL, STR, LIST -> expression = expression.type(castTo);
            case INT -> expression = typecastToInt(expression, lineNumber);
            case FLOAT -> expression = expression.unquoteBody().type(castTo);
            default -> throw typecastError;
        }
        return expression;
    }

    // helpers to helpers

    private static boolean paramsMatch(
            @Nonnull final BashpileParser.ParamatersContext left,
            @Nonnull final BashpileParser.ParamatersContext right) {
        // create a stream of ids and a list of ids
        final Stream<String> leftStream = left.typedId().stream()
                .map(BashpileParser.TypedIdContext::Id).map(ParseTree::getText);
        final List<String> rightList = right.typedId().stream()
                .map(BashpileParser.TypedIdContext::Id).map(ParseTree::getText).toList();

        // match each left id to the corresponding right id, record the mismatches
        final AtomicInteger i = new AtomicInteger(0);
        final Stream<String> mismatches = leftStream.filter(str -> !str.equals(rightList.get(i.getAndIncrement())));

        // params match if we can't find any mismatches
        return mismatches.findFirst().isEmpty();
    }

    /**
     * Lazy DFS.
     * Helper to {@link #getFunctionDeclCtx(BashpileVisitor, BashpileParser.FunctionForwardDeclarationStatementContext)}
     *
     * @see <a href="https://stackoverflow.com/questions/26158082/how-to-convert-a-tree-structure-to-a-stream-of-nodes-in-java">Stack Overflow</a>
     * @param parentNode the root.
     * @return Flattened stream of parent nodes' rule context children.
     */
    private static @Nonnull Stream<ParserRuleContext> stream(@Nonnull final ParserRuleContext parentNode) {
        if (parentNode.getChildCount() == 0) {
            return Stream.of(parentNode);
        } else {
            final Stream<ParserRuleContext> children = parentNode.getRuleContexts(ParserRuleContext.class).stream();
            return Stream.concat(Stream.of(parentNode), children.flatMap(BashTranslationHelper::stream));
        }
    }

}
