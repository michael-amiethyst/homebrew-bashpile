package com.bashpile;

import com.bashpile.engine.BashTranslationEngine;
import com.bashpile.engine.BashpileVisitor;
import com.bashpile.engine.Translation;
import com.bashpile.exceptions.BashpileUncheckedException;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bashpile.engine.BashTranslationEngine.TAB;
import static com.bashpile.engine.Translation.toParagraphTranslation;

/** Has the Antlr parser and a lot of helper methods to BashTranslationEngine */
public class AntlrUtils {

    private static final Logger LOG = LogManager.getLogger(AntlrUtils.class);

    /**
     * These are the core antlr calls to run the lexer, parser, visitor and translation engine.
     *
     * @param origin The filename (if a file) or text (if just script lines) of the <code>is</code>.
     * @param is The input stream holding the Bashpile that we parse.
     * @return The generated shell script.
     */
    public static @Nonnull String parse(
            @Nonnull final String origin, @Nonnull final InputStream is) throws IOException {
        LOG.trace("Starting parse");
        // lexer
        final CharStream input = CharStreams.fromStream(is);
        final BashpileLexer lexer = new BashpileLexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);

        // parser
        final BashpileParser parser = new BashpileParser(tokens);
        final ParseTree tree = parser.program();

        return transpile(origin, tree);
    }

    /** Returns bash text block */
    private static @Nonnull String transpile(@Nonnull final String origin, @Nonnull final ParseTree tree) {
        // visitor and engine linked in visitor constructor
        final BashpileVisitor bashpileLogic = new BashpileVisitor(new BashTranslationEngine(origin));
        return bashpileLogic.visit(tree).body();
    }

    /**
     * Helper to {@link BashTranslationEngine#functionForwardDeclarationStatement(BashpileParser.FunctionForwardDeclarationStatementContext)}
     */
    public static @Nonnull ParserRuleContext getFunctionDeclCtx(
            @Nonnull final BashpileVisitor visitor,
            @Nonnull final BashpileParser.FunctionForwardDeclarationStatementContext ctx) {
        final String functionName = ctx.typedId().Id().getText();
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
                    final boolean nameMatches = decl.typedId().Id().getText().equals(functionName);
                    return nameMatches && paramsMatch(decl.paramaters(), ctx.paramaters());
                };
        return allContexts
                .filter(namesMatch)
                .findFirst()
                .orElseThrow(
                        () -> new BashpileUncheckedException("No matching function declaration for " + functionName));
    }

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
            return Stream.concat(Stream.of(parentNode), children.flatMap(AntlrUtils::stream));
        }
    }

    /** Visits all statements and indents the results */
    public static @Nonnull Translation visitBlock(
            @Nonnull final BashpileVisitor visitor, @Nonnull final Stream<ParserRuleContext> statementStream) {
        final String translationText = statementStream.map(visitor::visit)
                .map(Translation::assertEmptyPreamble)
                .map(Translation::body)
                // bodies may be multiline strings, convert to single lines
                .flatMap(str -> Arrays.stream(str.split("\n")))
                // indent each line
                .map(str -> "%s%s\n".formatted(TAB, str))
                .collect(Collectors.joining());
        return toParagraphTranslation(translationText);
    }

    /** Concatenates inputs into stream */
    public static @Nonnull Stream<ParserRuleContext> addContexts(
            @Nonnull final List<BashpileParser.StatementContext> statements,
            @Nonnull final BashpileParser.ReturnPsudoStatementContext returnPsudoStatementContext) {
        // map of x to x needed for upcasting to parent type
        final Stream<ParserRuleContext> statementStream = statements.stream().map(x -> x);
        return Stream.concat(statementStream, Stream.of(returnPsudoStatementContext));
    }
}
