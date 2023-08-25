package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedException;
import com.google.common.annotations.VisibleForTesting;
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

import static com.bashpile.AntlrUtils.parse;

// TODO FEATURE feature/andOrOperators add 'and' to check for file existence in bpr/bpc
// TODO FEATURE have bpc use arguments, arguments[all] and --output
/** Entry point into the program */
@CommandLine.Command(
        name = "bashpile",
        description = "Converts Bashpile lines to Bash"
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

    @CommandLine.Parameters(arity = "1..1",
            description = "Use the specified bashpile file.")
    @Nullable
    private Path inputFile;

    private CommandLine picocliCommandLine;

    public BashpileMain() {}

    public BashpileMain(@Nullable final Path inputFile) {
        this.inputFile = inputFile;
    }

    public BashpileMain(@Nullable final String bashpileScript) {
        this.bashpileScript = bashpileScript;
    }

    /** Sets the Pico Cli processor */
    public void setPicocliCommandLine(@Nonnull final CommandLine picocliCommandLine) {
        this.picocliCommandLine = picocliCommandLine;
    }

    /** Saves transpiled input file to inputFile.bpt */
    @Override
    public @Nonnull Integer call() throws IOException {
        final String filename = inputFile != null ? inputFile.toString() : "";
        if (Strings.isEmpty(filename)) {
            System.out.println("Input file must be specified.");
            picocliCommandLine.usage(System.out);
            return 1;
        }

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
        System.out.println(transpiledFilename);
        return 0;
    }

    // helpers

    @VisibleForTesting
    public @Nonnull String transpile() throws IOException {
        final Pair<String, InputStream> namedInputStream = getNameAndInputStream();
        try (final InputStream inputStream = namedInputStream.getRight()) {
            final String parsed = parse(namedInputStream.getLeft(), inputStream);
            return Asserts.assertNoShellcheckWarnings(parsed);
        }
    }

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
}
