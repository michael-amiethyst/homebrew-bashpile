package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.UserError;
import com.bashpile.shell.BashShell;
import com.bashpile.shell.ExecutionResults;
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

    @CommandLine.Option(names = {"-c", "--command"},
            description = "Use the specified text.  -i option has precedence if both are specified.")
    @Nullable
    private String bashpileScript;

    @CommandLine.Parameters(arity = "0..1",
            description = "Use the specified bashpile file.")
    @Nullable
    private Path inputFile;

    @CommandLine.Parameters(arity = "0..*",
            description = "Arguments to the script")
    @Nullable
    private List<String> scriptArgs;

    private CommandLine picocliCommandLine;

    public BashpileMain() {}

    public BashpileMain(@Nullable final Path inputFile) {
        this.inputFile = inputFile;
    }

    public BashpileMain(@Nullable final String bashpileScript) {
        this.bashpileScript = bashpileScript;
    }

    @Override
    public @Nonnull Integer call() {
        return executeCommand();
    }

    /** Called by the picocli framework */
    @SuppressWarnings({"unused", "SameReturnValue"})
    @CommandLine.Command(name = "execute", description = "Converts Bashpile lines to bash and executes them")
    public int executeCommand() {
        if (inputFile == null && StringUtils.isEmpty(bashpileScript)) {
            // prints help text and returns 'general error'
            picocliCommandLine.usage(System.out);
            return 1;
        }

        // TODO pass in args
        final ExecutionResults results = execute();
        System.out.println(results.stdout());
        return results.exitCode();
    }

    public @Nonnull ExecutionResults execute() {
        LOG.debug("In {}", System.getProperty("user.dir"));
        String bashScript = null;
        try {
            bashScript = transpile();
            return BashShell.runAndJoin(bashScript);
        } catch (UserError | AssertionError e) {
            throw e;
        } catch (Throwable e) {
            throw createExecutionException(e, bashScript);
        }
    }

    public @Nonnull BashShell executeAsync() {
        LOG.debug("In {}", System.getProperty("user.dir"));
        String bashScript = null;
        try {
            bashScript = transpile();
            return BashShell.runAsync(bashScript);
        } catch (UserError | AssertionError e) {
            throw e;
        } catch (Throwable e) {
            throw createExecutionException(e, bashScript);
        }
    }

    private static BashpileUncheckedException createExecutionException(Throwable e, String bashScript) {
        String msg = bashScript != null ? "\nCouldn't run `%s`".formatted(bashScript) : "\nCouldn't parse input";
        if (e.getMessage() != null) {
            msg += " because of\n`%s`".formatted(e.getMessage());
        }
        if (e.getCause() != null) {
            msg += "\n caused by `%s`".formatted(e.getCause().getMessage());
        }
        return new BashpileUncheckedException(msg, e);
    }

    /** Called by Picocli framework */
    @SuppressWarnings({"unused", "SameReturnValue"})
    @CommandLine.Command(name = "transpile", description = "Converts Bashpile lines to bash")
    public int transpileCommand() throws IOException {
        System.out.println(transpile());
        return 0;
    }

    public @Nonnull String transpile() throws IOException {
        try (InputStream inputStream = getInputStream()) {
            return parse(inputStream);
        }
    }

    private @Nonnull InputStream getInputStream() throws IOException {
        if (inputFile != null) {
            final List<String> lines = Files.readAllLines(inputFile);
            if (SHE_BANG.matcher(lines.get(0)).matches()) {
                final String removedShebang = String.join("", lines.subList(1, lines.size()));
                return IOUtils.toInputStream(removedShebang, StandardCharsets.UTF_8);
            }
            return Files.newInputStream(inputFile);
        } else if (bashpileScript != null) {
            return IOUtils.toInputStream(bashpileScript, StandardCharsets.UTF_8);
        } else {
            throw new BashpileUncheckedException("Neither inputFile nor bashpileScript supplied.");
        }
    }

    public void setPicocliCommandLine(@Nonnull final CommandLine picocliCommandLine) {
        this.picocliCommandLine = picocliCommandLine;
    }
}
