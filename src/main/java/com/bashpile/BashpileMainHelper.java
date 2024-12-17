package com.bashpile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import com.bashpile.engine.BashTranslationEngine;
import com.bashpile.engine.BashpileVisitor;
import com.bashpile.exceptions.BashpileUncheckedAssertionException;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.shell.ExecutionResults;
import com.google.common.annotations.VisibleForTesting;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.bashpile.exceptions.Exceptions.asUncheckedFunction;
import static com.bashpile.exceptions.Exceptions.asUncheckedSupplier;
import static com.bashpile.shell.BashShell.runAndJoin;
import static com.bashpile.shell.ExecutionResults.SUCCESS;

public class BashpileMainHelper {

    // statics

    private static final Pattern SHEBANG = Pattern.compile("^#!.*$");

    private static final Logger LOG = LogManager.getLogger(BashpileMainHelper.class);

    // class methods

    /**
     * Returns the translation.
     * @throws IOException on bad input file.
     * @throws BashpileUncheckedAssertionException on shellcheck errors.
     */
    @VisibleForTesting
    public static @Nonnull String transpileNioFile(@Nonnull Path inputFile) throws IOException {
        final InputStream inputStream = getSourceInputStream(inputFile);
        final String sourceName = inputFile.toString();
        final String parsed = parse(sourceName, inputStream);
        final String formatted = format(parsed);
        return assertNoShellcheckWarnings(formatted);
    }

    /**
     * Returns the translation.
     * @throws IOException on bad input file.
     * @throws BashpileUncheckedAssertionException on shellcheck errors.
     */
    @VisibleForTesting
    public static @Nonnull String transpileScript(@Nonnull String bashpileScript) throws IOException {
        final InputStream inputStream = IOUtils.toInputStream(bashpileScript, StandardCharsets.UTF_8);
        final String parsed = parse(bashpileScript, inputStream);
        LOG.debug("Parsed Bashpile script became:\n{}", parsed);
        final String formatted = format(parsed);
        return assertNoShellcheckWarnings(formatted);
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
            LOG.trace("Looking for path {}", path);
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

    /**
     * Reformats the bashScript to Google Bash formatting conventions with an external shfmt program.
     *
     * @param bashScript The script to reformat.
     * @return The reformatted Bash script.
     */
    private static @Nonnull String format(@Nonnull final String bashScript) {
        final Path temp = asUncheckedSupplier(() -> Files.createTempFile("format_temp", ".bash"));
        try {
            Files.writeString(temp, bashScript);
            if (runAndJoin("which shfmt").exitCode() != SUCCESS) {
                LOG.warn("shfmt not found on PATH.  Skipping formatting (is it installed?)");
                return bashScript;
            }
            final ExecutionResults shfmtResults = runAndJoin(
                    "shfmt -i 2 -ci -bn %s".formatted(temp.toString()));
            if (shfmtResults.exitCode() != SUCCESS) {
                final String message = """
                        Script was unable to format with shfmt.  Command: %s
                        shfmt code: %d, output: %s
                        JVM env PATH: %s
                        Script:
                        %s"""
                        .stripIndent()
                        .formatted(shfmtResults.stdin(), shfmtResults.exitCode(), shfmtResults.stdout(),
                                System.getenv("PATH"), bashScript);
                throw new BashpileUncheckedAssertionException(message);
            }
            return shfmtResults.stdout();
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        } finally {
            // let filesystem percolate a bit
            asUncheckedFunction(() -> Thread.sleep(Duration.ofMillis(20)));
            asUncheckedSupplier(() -> Files.deleteIfExists(temp));
        }
    }

    /**
     * Ensures that the shellcheck program can find no warnings.
     *
     * @param translatedShellScript The Bash script
     * @return The translatedShellScript for chaining.
     */
    public static @Nonnull String assertNoShellcheckWarnings(@Nonnull final String translatedShellScript) {
        Path tempFile = null;
        try {
            if (runAndJoin("which shellcheck").exitCode() != SUCCESS) {
                LOG.warn("shellcheck not found on PATH.  Skipping (is it installed?)");
                return translatedShellScript;
            }
            tempFile = Files.createTempFile("assert", "bps");
            Files.writeString(tempFile, translatedShellScript);
            // ignore many errors that don't apply
            final String excludes = Stream.of(2034, 2050, 2071, 2072, 2157)
                    .map(i -> "--exclude=SC" + i).collect(Collectors.joining(" "));
            final ExecutionResults shellcheckResults = runAndJoin(
                    "shellcheck --shell=bash --severity=warning %s %s".formatted(excludes, tempFile));
            if (shellcheckResults.exitCode() != SUCCESS) {
                final String message = "Script failed shellcheck.  Script:\n%s\nShellcheck output:\n%s".formatted(
                        translatedShellScript, shellcheckResults.stdout());
                throw new BashpileUncheckedAssertionException(message);
            }
            return translatedShellScript;
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        } finally {
            // delete tempFile
            if (tempFile != null) {
                final Path finalTempFile = tempFile;
                asUncheckedSupplier(() -> Files.deleteIfExists(finalTempFile));
            }
        }
    }
}
