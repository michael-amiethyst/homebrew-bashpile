package com.bashpile;

import com.bashpile.engine.BashTranslationEngine;
import com.bashpile.engine.BashpileVisitor;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.google.common.annotations.VisibleForTesting;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/** Entry point into the program */
@CommandLine.Command(
        name = "bashpile",
        description = "Bashpilec/bpc - Converts Bashpile lines to Bash OR Bashpile/bpr runs Bashpile directly"
)
public class BashpileMain implements Callable<Integer> {

    // statics

    private static final Pattern SHEBANG = Pattern.compile("^#!.*$");

    private static final Logger LOG = LogManager.getLogger(BashpileMain.class);

    /** Our main */
    public static void main(final String[] args) {
        final BashpileMain bashpile = new BashpileMain();
        final CommandLine argProcessor = new CommandLine(bashpile);
        bashpile.setPicocliCommandLine(argProcessor);
        System.exit(argProcessor.execute(args));
    }

    // class fields

    @Nullable
    private String bashpileScript;

    @CommandLine.Option(names = {"-o", "--outputFile"}, arity = "0..1",
            description = "Save the transpiled shell script to this filename.  Will overwrite if filename exists.")
    @Nullable @SuppressWarnings("UnusedDeclaration")
    private Path outputFile;

    @CommandLine.Option(names = {"-c", "--command"}, arity = "0..1",
            description = "Run the command")
    @Nullable
    private String command;

    @CommandLine.Parameters(arity = "0..1",
            description = "Use the specified bashpile file.")
    @Nullable
    private Path inputFile;

    private CommandLine picocliCommandLine;

    /** Command line call uses this */
    public BashpileMain() {}

    @VisibleForTesting
    public BashpileMain(@Nullable final Path inputFile) {
        this.inputFile = inputFile;
    }

    @VisibleForTesting
    public BashpileMain(@Nullable final String bashpileScript) {
        this.bashpileScript = bashpileScript;
    }

    /** Sets the Pico Cli processor */
    public void setPicocliCommandLine(@Nonnull final CommandLine picocliCommandLine) {
        this.picocliCommandLine = picocliCommandLine;
    }

    /**
     * Front-door after the PicoCLI framework does command-line option / argument processing.
     * Saves transpiled input file to inputFile.bpt OR runs command.
     */
    @Override
    public @Nonnull Integer call() throws IOException {
        // guard
        String filename = inputFile != null ? inputFile.toString() : "";
        if (Strings.isEmpty(filename) && Strings.isEmpty(command)) {
            // bad input
            System.out.println("Input file or -c/--command option must be specified.");
            picocliCommandLine.usage(System.out);
            return 1;
        } else if (Strings.isNotEmpty(command)) {
            // massage command into a file
            filename = "command.bps";
            inputFile = Path.of(filename);
            Files.writeString(inputFile, command);
        }

        // transpile
        Path transpiledFilename;
        if (outputFile != null) {
            transpiledFilename = outputFile;
        } else {
            transpiledFilename = Path.of(filename + ".bpt");
            if (Files.exists(transpiledFilename)) {
                System.out.println(transpiledFilename + " already exists.  Will not overwrite.");
                return 2;
            }
        }
        LOG.info("Transpiling {} to {}", filename, transpiledFilename);
        final String bashScript = "#!/usr/bin/env bash\n\n" + transpile();
        Files.writeString(transpiledFilename, bashScript);
        // last line must be the filename we created
        LOG.info("Created file is:");
        System.out.println(transpiledFilename.toAbsolutePath());
        return 0;
    }

    // helpers

    /** Returns the translation */
    @VisibleForTesting
    public @Nonnull String transpile() throws IOException {
        // TODO use params for getNameAndInputStream?
        final Pair<String, InputStream> namedInputStream = getNameAndInputStream();
        try (final InputStream inputStream = namedInputStream.getRight()) {
            final String parsed = parse(namedInputStream.getLeft(), inputStream);
            return Asserts.assertNoShellcheckWarnings(parsed);
        }
    }

    // TODO break into getName and getInputStream?
    private @Nonnull Pair<String, InputStream> getNameAndInputStream() throws IOException {
        if (inputFile != null) {
            final List<String> lines = Files.readAllLines(findFile(inputFile));
            InputStream is;
            if (SHEBANG.matcher(lines.get(0)).matches()) {
                final String removedShebang = String.join("\n", lines.subList(1, lines.size()));
                LOG.trace("Removed shebang to get:\n" + removedShebang);
                is = IOUtils.toInputStream(removedShebang, StandardCharsets.UTF_8);
            } else {
                is = Files.newInputStream(inputFile);
            }
            return Pair.of(inputFile.toString(), is);
        } else if (bashpileScript != null) {
            return Pair.of(bashpileScript, IOUtils.toInputStream(bashpileScript, StandardCharsets.UTF_8));
        } else {
            throw new BashpileUncheckedException("Neither inputFile nor bashpileScript supplied.");
        }
    }

    private @Nonnull Path findFile(@Nonnull final Path find) {
        Path path = find.normalize().toAbsolutePath();
        final Path filename = path.getFileName();
        while(!Files.exists(path) && path.getParent() != null && path.getParent().getParent() != null) {
            path = path.getParent().getParent().resolve(filename);
            LOG.trace("Looking for path " + path);
        }
        if (Files.exists(path)) {
            return path;
        }
        throw new  BashpileUncheckedException("Could not find " + path.getFileName());
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
