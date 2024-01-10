package com.bashpile;

import com.bashpile.engine.BashTranslationEngine;
import com.bashpile.engine.BashpileVisitor;
import com.bashpile.exceptions.BashpileUncheckedException;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

public class BashpileMainProcessor {

    // statics

    private static final Pattern SHEBANG = Pattern.compile("^#!.*$");

    private static final Logger LOG = LogManager.getLogger(BashpileMainProcessor.class);

    // class methods

    /** Returns the translation */
    public static @Nonnull String transpileNioFile(@Nonnull Path inputFile) throws IOException {
        final InputStream inputStream = getSourceInputStream(inputFile);
        final String sourceName = inputFile.toString();
        final String parsed = parse(sourceName, inputStream);
        return Asserts.assertNoShellcheckWarnings(parsed);
    }

    /** Returns the translation */
    public static @Nonnull String transpileScript(@Nonnull String bashpileScript) throws IOException {
        final InputStream inputStream = IOUtils.toInputStream(bashpileScript, StandardCharsets.UTF_8);
        final String parsed = parse(bashpileScript, inputStream);
        return Asserts.assertNoShellcheckWarnings(parsed);
    }

    // helpers

    /** Returns an input stream of inputFile (without a Shebang line) or defaults to the bashpileScript as an IS */
    private static @Nonnull InputStream getSourceInputStream(@Nonnull final Path inputFile) throws IOException {
        List<String> lines = Files.readAllLines(findFile(inputFile));
        if (SHEBANG.matcher(lines.get(0)).matches()) {
            lines = lines.subList(1, lines.size());
        }
        return IOUtils.toInputStream(String.join("\n", lines), StandardCharsets.UTF_8);
    }

    private static @Nonnull Path findFile(@Nonnull final Path find) {
        Path path = find.normalize().toAbsolutePath();
        final Path filename = path.getFileName();
        while(!Files.exists(path) && path.getParent() != null && path.getParent().getParent() != null) {
            path = path.getParent().getParent().resolve(filename);
            LOG.trace("Looking for path " + path);
        }
        if (Files.exists(path)) {
            return path;
        }
        String message = "Could not find %s from directory %s".formatted(
                path.getFileName(), Paths.get("").toAbsolutePath());
        throw new BashpileUncheckedException(message);
    }

    /**
     * These are the core antlr calls to run the lexer, parser, visitor and translation engine.
     *
     * @param origin The filename (if a file) or text (if just script lines) of the <code>is</code>.
     * @param is The input stream holding the Bashpile that we parse.
     * @return The generated shell script.
     */
    private static @Nonnull String parse(
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
}
