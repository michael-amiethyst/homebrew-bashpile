package com.bashpile;

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

/** Entry point into the program.  Only spins up the transpiler and parses the command line with PicoCLI. */
@CommandLine.Command(
        name = "bashpile",
        description = "Bashpilec/bpc - Converts Bashpile lines to Bash OR Bashpile/bpr runs Bashpile directly"
)
public class BashpileMain implements Callable<Integer> {

    // statics

    private static final Logger LOG = LogManager.getLogger(BashpileMain.class);

    /** Our main */
    public static void main(final String[] args) {
        final BashpileMain bashpile = new BashpileMain();
        final CommandLine argProcessor = new CommandLine(bashpile);
        bashpile.setPicocliCommandLine(argProcessor);
        System.exit(argProcessor.execute(args));
    }

    // class fields

    @CommandLine.Option(names = {"-o", "--outputFile"}, arity = "0..1",
            description = "Save the transpiled shell script to this filename.  Will overwrite if filename exists.")
    @Nullable @SuppressWarnings("UnusedDeclaration")
    private Path outputFile;

    @CommandLine.Option(names = {"-c", "--command"}, arity = "0..1",
            description = "Run the command")
    @Nullable @SuppressWarnings("UnusedDeclaration")
    private String command;

    @CommandLine.Parameters(arity = "0..1",
            description = "Use the specified bashpile file.")
    @Nullable @SuppressWarnings("UnusedDeclaration")
    private Path inputFile;

    private CommandLine picocliCommandLine;

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
            transpiledFilename = Path.of(filename + ".bpt");
            if (Files.exists(transpiledFilename)) {
                System.out.println(transpiledFilename + " already exists.  Will not overwrite.");
                return 2;
            }
        }
        LOG.info("Transpiling to {}", transpiledFilename);
        String translation = inputFile != null ? BashpileMainProcessor.transpileNioFile(inputFile)
                : BashpileMainProcessor.transpileScript(Objects.requireNonNull(command));
        final String bashScript = "#!/usr/bin/env bash\n\n" + translation;
        Files.writeString(transpiledFilename, bashScript);
        // last line must be the filename we created
        LOG.info("Created file is:");
        System.out.println(transpiledFilename.toAbsolutePath());
        return 0;
    }
}
