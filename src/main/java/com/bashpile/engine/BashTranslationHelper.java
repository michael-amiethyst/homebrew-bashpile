package com.bashpile.engine;

import com.bashpile.Asserts;
import com.bashpile.BashpileParser;
import com.bashpile.Strings;
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
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bashpile.Strings.lambdaAllLines;
import static com.bashpile.Strings.lambdaFirstLine;
import static com.bashpile.engine.BashTranslationEngine.TAB;
import static com.bashpile.engine.Translation.UNKNOWN_TRANSLATION;
import static com.bashpile.engine.Translation.toStringTranslation;
import static com.bashpile.engine.strongtypes.SimpleType.INT;
import static com.bashpile.engine.strongtypes.TranslationMetadata.CALCULATION;
import static com.bashpile.engine.strongtypes.Type.INT_TYPE;
import static com.bashpile.engine.strongtypes.Type.STR_TYPE;

/**
 * Helper methods to {@link BashTranslationEngine}.
 */
public class BashTranslationHelper {

    private static final Pattern escapedNewline = Pattern.compile("\\\\\\r?\\n\\s*");

    private static final Logger LOG = LogManager.getLogger(BashTranslationHelper.class);

    // static methods

    /* package */ static @Nonnull String getBodyStringForCreatesStatement(
            @Nonnull final BashpileParser.CreatesStatementContext ctx,
            @Nonnull final Translation shellString,
            @Nonnull final String filename,
            @Nonnull final BashpileVisitor visitor,
            @Nonnull final Stack<String> createFilenamesStack) {
        String preamble, check, thenFragment;
        boolean briefGuard = !shellString.hasPreamble();
        if (briefGuard) {
            // collapse with semicolons to one line
            preamble = Arrays.stream(shellString.preamble().trim().split("\n"))
                    .filter(str -> !str.trim().startsWith("#"))
                    .collect(Collectors.joining("; "));
            check = String.join("; ", shellString.body().trim().split("\n"));
            if (Strings.isNotEmpty(preamble)) {
                preamble += "; ";
            }
            thenFragment = "; then";
        } else {
            // preserve whitespace
            preamble = "\n    ## end of unnest\n" + shellString.lambdaPreambleLines(str -> TAB + str).preamble();
            check = shellString.lambdaBodyLines(str -> TAB + str).body();
            thenFragment = "\nthen";
        }

        // set noclobber avoids some race conditions
        String ifGuard = "if (set -o noclobber; %s%s) 2> /dev/null%s".formatted(preamble, check, thenFragment);

        // create our statements translation
        final Translation statements = ctx.statement().stream()
                .map(visitor::visit)
                .reduce(Translation::add)
                .orElseThrow()
                .assertParagraphBody()
                .assertNoBlankLinesInBody();
        // create an ifBody to put into the bodyTranslation
        // only one trap can be in effect at a time, so we keep a stack of all current filenames to delete
        String ifBody = """
                trap 'rm -f %s; exit 10' INT TERM EXIT
                ## wrapped body of creates statement
                %s
                ## end of wrapped body of creates statement
                rm -f %s
                trap - INT TERM EXIT""".formatted(
                String.join(" ", createFilenamesStack), statements.body(), filename);
        ifBody = lambdaAllLines(ifBody, str -> TAB + str);
        ifBody = lambdaFirstLine(ifBody, String::stripLeading);

        final String plainFilename = StringUtils.removeStart(Strings.unquote(filename), "$");
        String elseBody = """
                printf "Failed to create %s correctly, script output was:\\n"
                cat %s
                rm -f %s
                exit 1""".formatted(plainFilename, filename, filename);
        elseBody = lambdaAllLines(elseBody, str -> TAB + str);
        elseBody = lambdaFirstLine(elseBody, String::stripLeading);
        return """
                %s
                    %s
                else
                    %s
                fi
                declare -i __bp_exitCode=$?
                if [ "$__bp_exitCode" -ne 0 ]; then exit "$__bp_exitCode"; fi
                """.formatted(ifGuard, ifBody, elseBody);
    }

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

    /* package */ static @Nonnull Translation typecastFromBool(
            @Nonnull final SimpleType castTo,
            @Nonnull Translation expression,
            @Nonnull final TypeError typecastError) {
        switch (castTo) {
            case BOOL -> {}
            case STR -> expression = expression.quoteBody().type(STR_TYPE);
            // no cast to int, float or list
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static @Nonnull Translation typecastFromInt(
            @Nonnull final SimpleType castTo,
            @Nonnull Translation expression,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        if (!expression.metadata().contains(CALCULATION)) {
            // parse expression to a BigInteger
            try {
                new BigInteger(expression.body());
            } catch (final NumberFormatException e) {
                String message = "Couldn't parse '%s' to an INT.  " +
                        "Typecasts only work on literals, was this an ID or function call?";
                throw new TypeError(message.formatted(expression.body()), lineNumber);
            }
        }

        // Cast
        switch (castTo) {
            case INT, FLOAT -> {}
            case STR -> expression = expression.quoteBody();
            // no typecast to bool or list
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static @Nonnull Translation typecastFromFloat(
            @Nonnull final SimpleType castTo,
            @Nonnull Translation expression,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        // parse expression as a BigDecimal
        BigDecimal expressionValue = null;
        boolean literalValue = true;  // e.g. not a variable reference
        try {
            expressionValue = new BigDecimal(expression.body());
        } catch (final NumberFormatException e) {
            // check for literal float
            try {
                expressionValue = new BigDecimal(expression.body());
                throw new TypeError(
                        "Couldn't parse %s (FLOAT) to a(n) %s".formatted(expression.body(), castTo),
                        lineNumber);
            } catch (final NumberFormatException e1) {
                literalValue = false;
            }
        }

        // cast
        switch (castTo) {
            case INT -> {
                if (literalValue) {
                    return expression.body(expressionValue.toBigInteger().toString()).type(INT_TYPE);
                } else {
                    // if a variable reference then typecast to int (round down) with printf
                    String varName = StringUtils.stripStart(expression.body(), "${");
                    varName = StringUtils.stripEnd(varName, "}");
                    return expression.addPreamble("""
                                    %s=$(printf '%%d' "%s" 2>/dev/null || true)
                                    """.formatted(varName, expression))
                            .type(INT_TYPE);
                }
            }
            case FLOAT -> {}
            // TODO ensure .type(new_type) on other typecast methods
            case STR -> expression = expression.quoteBody().type(STR_TYPE);
            // no typecast to bool or list
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static @Nonnull Translation typecastFromStr(
            @Nonnull final SimpleType castTo,
            @Nonnull Translation expression,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        switch (castTo) {
            case BOOL -> {
                expression = expression.unquoteBody();
                if (SimpleType.isNumberString(expression.body())) {
                    expression = typecastFromFloat(castTo, expression, lineNumber, typecastError);
                } else if (expression.body().equalsIgnoreCase("true")
                        || expression.body().equalsIgnoreCase("false")) {
                    expression = expression.body(expression.body().toLowerCase());
                } else {
                    throw new TypeError("""
                            Could not cast STR to BOOL.
                            Only 'true' and 'false' allowed (capitalization ignored).
                            Text was %s.""".formatted(expression.body()), lineNumber);
                }
            }
            case INT -> {
                // no automatic rounding for things like `"2.5":int`
                // for argument variables (e.g. $1) take the user's word for it, we can't check here
                expression = expression.unquoteBody();
                final SimpleType foundType =
                        !expression.body().startsWith("$") ? SimpleType.parseNumberString(expression.body()) : INT;
                if (!INT.equals(foundType)) {
                    throw new TypeError("""
                        Could not cast FLOAT value in STR to INT.  Try casting to float first.  Text was %s."""
                            .formatted(expression.body()), lineNumber);
                }
            }
            case FLOAT -> {
                expression = expression.unquoteBody();
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
            // no typecast to list
            default -> throw typecastError;
        }
        return expression;
    }

    /** Casts to a different kind of list (i.e. a different subtype), but no other conversions */
    /* package */ static @Nonnull Translation typecastFromList(
            @Nonnull final Type castTo,
            @Nonnull Translation expression,
            @Nonnull final TypeError typecastError) {
        if (castTo.isList()) {
            // Allow a typecast to any subtype
            return expression.type(castTo);
        } else {
            // cannot cast to bool, int, float or str
            throw typecastError;
        }
    }

    /* package */ static void typecastFromUnknown(
            @Nonnull final SimpleType castTo, @Nonnull final TypeError typecastError) {
        // TODO check that INTs and Floats parse
        switch (castTo) {
            case BOOL, INT, FLOAT, STR, LIST -> {}
            default -> throw typecastError;
        }
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
