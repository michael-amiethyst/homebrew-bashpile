package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedException;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
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

// TODO verify translations with ShellCheck
/** Entry point into the program */
@CommandLine.Command(
        name = "bashpile",
        description = "Converts Bashpile lines to Bash"
)
public class BashpileMain implements Callable<Integer> {

    // statics

    private static final Pattern SHE_BANG = Pattern.compile("^#!.*$");

    private static final Logger LOG = LogManager.getLogger(BashpileMain.class);

    public static void main(final String[] args) {
        final BashpileMain bashpile = new BashpileMain();
        final CommandLine argProcessor = new CommandLine(bashpile);
        bashpile.setPicocliCommandLine(argProcessor);
        System.exit(argProcessor.execute(args));
    }

    // class fields

    @Nullable
    private String bashpileScript;

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

    public void setPicocliCommandLine(@Nonnull final CommandLine picocliCommandLine) {
        this.picocliCommandLine = picocliCommandLine;
    }

    /** Saves transpiled input file to inputFile.bpt */
    @Override
    public @Nonnull Integer call() throws IOException {
        final String filename = inputFile != null ? inputFile.toString() : "";
        if (StringUtils.isEmpty(filename)) {
            System.out.println("Input file must be specified.");
            picocliCommandLine.usage(System.out);
            return 1;
        }

        final String transpiledFilename = filename + ".bpt";
        final Path outputFile = Path.of(transpiledFilename);
        if (Files.exists(outputFile)) {
            System.out.println(transpiledFilename + " already exists.  Will not overwrite.");
            return 2;
        }
        LOG.info("Transpiling {} to {}", filename, transpiledFilename);
        final String bashScript = "#!/usr/bin/env bash\n\n" + transpile();
        Files.writeString(outputFile, bashScript);
        // last line must be the filename we created
        LOG.info("Created file is:");
        System.out.println(transpiledFilename);
        return 0;
    }

    // helpers

    @VisibleForTesting
    public @Nonnull String transpile() throws IOException {
        try (InputStream inputStream = getInputStream()) {
            return Asserts.assertNoShellcheckWarnings(parse(inputStream));
        }
    }

    private @Nonnull InputStream getInputStream() throws IOException {
        if (inputFile != null) {
            final List<String> lines = Files.readAllLines(findFile(inputFile));
            if (SHE_BANG.matcher(lines.get(0)).matches()) {
                final String removedShebang = String.join("\n", lines.subList(1, lines.size()));
                LOG.debug("Removed shebang to get:\n" + removedShebang);
                return IOUtils.toInputStream(removedShebang, StandardCharsets.UTF_8);
            }
            return Files.newInputStream(inputFile);
        } else if (bashpileScript != null) {
            return IOUtils.toInputStream(bashpileScript, StandardCharsets.UTF_8);
        } else {
            throw new BashpileUncheckedException("Neither inputFile nor bashpileScript supplied.");
        }
    }

    private @Nonnull Path findFile(@Nonnull Path path) {
        path = path.normalize().toAbsolutePath();
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
