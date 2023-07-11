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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bashpile.engine.BashTranslationEngine.TAB;
import static com.bashpile.engine.Translation.toStringTranslation;

/** Has the Antlr parser and a lot of helper methods to BashTranslationEngine */
public class AntlrUtils {

    private static final Logger log = LogManager.getLogger(AntlrUtils.class);

    /** antlr calls */
    public static @Nonnull String parse(@Nonnull final InputStream is) throws IOException {
        log.trace("Starting parse");
        // lexer
        final CharStream input = CharStreams.fromStream(is);
        final BashpileLexer lexer = new BashpileLexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);

        // parser
        final BashpileParser parser = new BashpileParser(tokens);
        final ParseTree tree = parser.prog();

        return transpile(tree);
    }

    /** Returns bash text block */
    private static @Nonnull String transpile(@Nonnull final ParseTree tree) {
        // visitor and engine linked in visitor constructor
        final BashpileVisitor bashpileLogic = new BashpileVisitor(new BashTranslationEngine());
        return bashpileLogic.visit(tree).text();
    }

    /** Helper to {@link BashTranslationEngine#functionDeclStatement(BashpileParser.FunctionDeclStmtContext)} */
    public static @Nonnull ParserRuleContext getFunctionDeclCtx(
            @Nonnull final BashpileVisitor visitor, @Nonnull final BashpileParser.FunctionForwardDeclStmtContext ctx) {
        final String functionName = ctx.typedId().ID().getText();
        assert visitor.getContextRoot() != null;
        final Stream<ParserRuleContext> allContexts = stream(visitor.getContextRoot());
        final Predicate<ParserRuleContext> namesMatch =
                x -> {
                    boolean isDeclaration = x instanceof BashpileParser.FunctionDeclStmtContext;
                    // is a function declaration and the names match
                    if (!isDeclaration) {
                        return false;
                    }
                    final BashpileParser.FunctionDeclStmtContext decl = (BashpileParser.FunctionDeclStmtContext) x;
                    final boolean nameMatches = decl.typedId().ID().getText().equals(functionName);
                    return nameMatches && paramCompare(decl.paramaters(), ctx.paramaters());
                };
        return allContexts
                .filter(namesMatch)
                .findFirst()
                .orElseThrow(
                        () -> new BashpileUncheckedException("No matching function declaration for " + functionName));
    }

    private static boolean paramCompare(
            @Nonnull final BashpileParser.ParamatersContext left,
            @Nonnull final BashpileParser.ParamatersContext right) {
        final Stream<String> leftStream = left.typedId().stream()
                .map(BashpileParser.TypedIdContext::ID).map(ParseTree::getText);
        final List<String> rightList = right.typedId().stream()
                .map(BashpileParser.TypedIdContext::ID).map(ParseTree::getText).toList();
        return leftStream.allMatch(rightList::contains);
    }

    /**
     * Lazy DFS.  Helper to {@link #getFunctionDeclCtx(BashpileVisitor, BashpileParser.FunctionForwardDeclStmtContext)}
     *
     * @see <a href="https://stackoverflow.com/questions/26158082/how-to-convert-a-tree-structure-to-a-stream-of-nodes-in-java>Stack Overflow</a>
     * @param parentNode the root.
     * @return Flattened stream of parent nodes' rule context children.
     */
    private static @Nonnull Stream<ParserRuleContext> stream(@Nonnull final ParserRuleContext parentNode) {
        if (parentNode.getChildCount() == 0) {
            return Stream.of(parentNode);
        } else {
            return Stream.concat(Stream.of(parentNode),
                    parentNode.getRuleContexts(ParserRuleContext.class).stream().flatMap(AntlrUtils::stream));
        }
    }

    public static @Nonnull Translation visitBlock(
            @Nonnull final BashpileVisitor visitor, @Nonnull final Stream<ParserRuleContext> stmtStream) {
        final String translationText = stmtStream.map(visitor::visit)
                .map(Translation::text)
                // visit results may be multiline strings, convert to array of single lines
                .map(str -> str.split("\n"))
                // stream the lines, indent each line, then flatten
                .flatMap(lines -> Arrays.stream(lines).sequential().map(s -> TAB + s + "\n"))
                .collect(Collectors.joining());
        return toStringTranslation(translationText);
    }

    /** Concatenates inputs into stream */
    public static @Nonnull Stream<ParserRuleContext> addContexts(
            @Nonnull final List<BashpileParser.StmtContext> stmts,
            @Nonnull final BashpileParser.ReturnRuleContext ctx) {
        // map of x to x needed for upcasting to parent type
        final Stream<ParserRuleContext> stmt = stmts.stream().map(x -> x);
        return Stream.concat(stmt, Stream.of(ctx));
    }
}
