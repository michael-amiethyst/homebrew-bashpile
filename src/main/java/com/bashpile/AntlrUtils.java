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
    public static String parse(final InputStream is) throws IOException {
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
    private static String transpile(final ParseTree tree) {
        // visitor and engine linked in visitor constructor
        final BashpileVisitor bashpileLogic = new BashpileVisitor(new BashTranslationEngine());
        return bashpileLogic.visit(tree).text();
    }

    /** Helper to {@link BashTranslationEngine#functionDecl(BashpileParser.FunctionDeclStmtContext)} */
    public static ParserRuleContext getFunctionDeclCtx(
            final BashpileVisitor visitor, final BashpileParser.FunctionForwardDeclStmtContext ctx) {
        final String functionName = ctx.ID().getText();
        final Stream<ParserRuleContext> allContexts = stream(visitor.getContextRoot());
        final Predicate<ParserRuleContext> namesMatch =
                x -> {
                    boolean isDeclaration = x instanceof BashpileParser.FunctionDeclStmtContext;
                    // is a function declaration and the names match
                    if (!isDeclaration) {
                        return false;
                    }
                    final BashpileParser.FunctionDeclStmtContext decl = (BashpileParser.FunctionDeclStmtContext) x;
                    final boolean nameMatches = decl.ID().getText().equals(functionName);
                    return nameMatches && paramCompare(decl.paramaters(), ctx.paramaters());
                };
        return allContexts
                .filter(namesMatch)
                .findFirst()
                .orElseThrow(
                        () -> new BashpileUncheckedException("No matching function declaration for " + functionName));
    }

    private static boolean paramCompare(
            final BashpileParser.ParamatersContext left, final BashpileParser.ParamatersContext right) {
        final Stream<String> leftStream = left.ID().stream().map(ParseTree::getText);
        final List<String> rightList = right.ID().stream().map(ParseTree::getText).toList();
        return leftStream.allMatch(rightList::contains);
    }

    /**
     * Lazy DFS.  Helper to {@link #getFunctionDeclCtx(BashpileVisitor, BashpileParser.FunctionForwardDeclStmtContext)}
     *
     * @see <a href="https://stackoverflow.com/questions/26158082/how-to-convert-a-tree-structure-to-a-stream-of-nodes-in-java>Stack Overflow</a>
     * @param parentNode the root.
     * @return Flattened stream of parent nodes' rule context children.
     */
    private static Stream<ParserRuleContext> stream(final ParserRuleContext parentNode) {
        if (parentNode.getChildCount() == 0) {
            return Stream.of(parentNode);
        } else {
            return Stream.concat(Stream.of(parentNode),
                    parentNode.getRuleContexts(ParserRuleContext.class).stream().flatMap(AntlrUtils::stream));
        }
    }

    public static Translation visitBlock(final BashpileVisitor visitor, final Stream<ParserRuleContext> stmtStream) {
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
    public static Stream<ParserRuleContext> addContexts(
            final List<BashpileParser.StmtContext> stmts, final BashpileParser.ReturnRuleContext ctx) {
        // map of x to x needed for upcasting to parent type
        final Stream<ParserRuleContext> stmt = stmts.stream().map(x -> x);
        return Stream.concat(stmt, Stream.of(ctx));
    }
}
