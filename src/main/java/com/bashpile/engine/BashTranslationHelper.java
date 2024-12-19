package com.bashpile.engine;

import com.bashpile.Asserts;
import com.bashpile.BashpileParser;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.BashpileUncheckedException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.bashpile.engine.BashTranslationEngine.TAB;
import static com.bashpile.engine.Translation.UNKNOWN_TRANSLATION;
import static com.bashpile.engine.Translation.toStringTranslation;
import static com.bashpile.engine.strongtypes.Type.NA_TYPE;

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

    /**
     * Get left hand side's type.
     *
     * @param ctx The Antlr context
     * @return The left hand side's type, including contents type if applicable
     */
    /* package */ static @NotNull Type getLhsType(@NotNull BashpileParser.AssignmentStatementContext ctx) {
        // extract info from context
        int lineNumber = lineNumber(ctx);
        final BashpileParser.ComplexTypeContext lhsTypeRoot = ctx.typedId().complexType();
        final String lhsTypeText = lhsTypeRoot.types(0).getText();

        // find type - the result might be a list with a contents type
        final Type.TypeNames lhsMainType = Type.TypeNames.valueOf(lhsTypeText.toUpperCase());
        final BashpileParser.TypesContext contentsTypeNode = lhsTypeRoot.types(1);
        final Type lhsContentsType =
                contentsTypeNode != null ? Type.valueOf(contentsTypeNode.getText(), lineNumber) : NA_TYPE;
        return new Type(lhsMainType, Optional.of(lhsContentsType));
    }

    /** Get the Bashpile script line number that ctx is found in. */
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
    /* package */ static Translation visitGuardingExpression(Translation expressionTranslation) {
        if (expressionTranslation.type().isInt() && expressionTranslation.body().startsWith("$((")) {
            // strip initial $ for (( instead of $((
            expressionTranslation = expressionTranslation.lambdaBody(body -> body.substring(1));
        } else if (expressionTranslation.type().isNumeric()) {
            // to handle floats we use bc, but the test by default will be for if bc succeeded (had exit code 0)
            // so we need to explicitly check if the check returned true (1)
            expressionTranslation = expressionTranslation
                    .inlineAsNeeded()
                    .lambdaBody("[ \"$(bc <<< \"%s == 0\")\" -eq 1 ]"::formatted);
        }
        return expressionTranslation
                .lambdaBody(body -> {
                    // remove $() for if statement
                    if (!body.contains("\n") && !body.startsWith("$((") && body.endsWith(")")) {
                        if (body.startsWith("$(")) {
                            body = body.substring(2, body.length() - 1);
                        } else if (body.startsWith("! $(")) {
                            body = "! " + body.substring(body.indexOf("$(") + 2, body.length() - 1);
                        }
                    }
                    return body;
                });
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
