package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Entry point into the program.  Only spins up the transpiler and parses the command line with PicoCLI. */
@CommandLine.Command(
        name = "bashpile",
        description = "Bashpilec ('bpc' for short) - Compiles Bashpile to Bash\n" +
                "Bashpile ('bpr' for short) - Runs the Bashpile script directly"
)
public class BashpileMain implements Callable<Integer> {

    // statics

    private static final Logger LOG = LogManager.getLogger(BashpileMain.class);

    /** Matches stuff like a.jpeg or b.e */
    private static final Pattern FILE_EXTENSION = Pattern.compile("^(.+)?\\..[^.]?[^.]?[^.]?[^.]?$");

    /** Our main */
    public static void main(final String[] args) {
        final BashpileMain bashpile = new BashpileMain();
        final CommandLine argProcessor = new CommandLine(bashpile);
        bashpile.setPicocliCommandLine(argProcessor);
        System.exit(argProcessor.execute(args));
    }

    // class fields

    @CommandLine.Option(names = {"-o", "--outputFile"}, arity = "0..1",
            description = "Save the transpiled shell script to this filename.\n" +
                    "It will overwrite if filename exists.")
    @Nullable @SuppressWarnings("UnusedDeclaration")
    private Path outputFile;

    @CommandLine.Option(names = {"-c", "--command"}, arity = "0..1",
            description = "Run the command.  -c or INPUT_FILE is required")
    @Nullable @SuppressWarnings("UnusedDeclaration")
    private String command;

    // TODO make inputFile required
    @CommandLine.Parameters(arity = "0..1",
            description = "Use the specified Bashpile file.  -c or INPUT_FILE")
    @Nullable @SuppressWarnings("UnusedDeclaration")
    private Path inputFile;

    private CommandLine picocliCommandLine;

    /** Sets the Pico Cli processor */
    public void setPicocliCommandLine(@Nonnull final CommandLine picocliCommandLine) {
        this.picocliCommandLine = picocliCommandLine;
    }

    /**
     * Front-door after the PicoCLI framework does command-line option / argument processing.
     * Saves transpiled input file OR runs command.
     */
    @Override
    public @Nonnull Integer call() throws IOException {
        // guard
        if (inputFile == null && command == null) {
            // bad input
            System.out.println("Input file or -c/--command option must be specified.");
            picocliCommandLine.usage(System.out);
            return 1;
        }

        // transpile
        Path transpiledFilename;
        if (outputFile != null) {
            transpiledFilename = outputFile;
        } else {
            String filename = inputFile != null ? inputFile.toString() : "command";
            LOG.debug("Input file is: {}", filename);
            final Matcher matcher = FILE_EXTENSION.matcher(filename);
            if (matcher.find()) {
                transpiledFilename = Path.of(matcher.group(1));
            } else {
                transpiledFilename = Path.of(filename + ".bash");
            }
        }

        // will overwrite
        LOG.info("Transpiling in directory {}.  Will create or overwrite file {}",
                System.getProperty("user.dir"), transpiledFilename);
        try {
            String translation = inputFile != null ? BashpileMainHelper.transpileNioFile(inputFile)
                    : BashpileMainHelper.transpileScript(Objects.requireNonNull(command));
            final String bashScript = "#!/usr/bin/env bash\n\n" + translation;
            Files.writeString(transpiledFilename, bashScript);
            // last line must be the filename we created
            LOG.info("Created file is:");
            System.out.println(transpiledFilename.toAbsolutePath());
            return 0;
        } catch (final IOException | BashpileUncheckedException e) {
            // delete if output file exists (e.g. we had a generated tempfile as output)
            // this indicates a failed compile
            Files.deleteIfExists(transpiledFilename);
            return 1;
        }
    }
}
