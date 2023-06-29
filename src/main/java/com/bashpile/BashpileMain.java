package com.bashpile;

import com.bashpile.commandline.BashExecutor;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.UserError;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Callable;

import static com.bashpile.AntlrUtils.parse;

// TODO use @Nullable, @NonNull vigorously
/** Entry point into the program */
@CommandLine.Command(
        name = "bashpile",
        description = "Converts Bashpile lines to Bash"
)
public class BashpileMain implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(BashpileMain.class);

    public static void main(final String[] args) {
        final BashpileMain bashpile = new BashpileMain();
        final CommandLine argProcessor = new CommandLine(bashpile);
        bashpile.setCommandLine(argProcessor);
        System.exit(argProcessor.execute(args));
    }

    @CommandLine.Option(names = {"-i", "--inputFile"})
    private String inputFile;

    private CommandLine commandLine;

    public BashpileMain() {}

    public BashpileMain(@Nullable final String inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public @Nonnull Integer call() {
        // prints help text and returns 'general error'
        commandLine.usage(System.out);
        return 1;
    }

    /** Called by the picocli framework */
    @CommandLine.Command(name = "execute", description = "Converts Bashpile lines to bash and executes them")
    public int executeCommand() {
        System.out.println(execute().getLeft());
        return 0;
    }

    public @Nonnull Pair<String, Integer> execute() {
        log.debug("In {}", System.getProperty("user.dir"));
        String bashScript = Objects.requireNonNullElse(inputFile, "System.in");
        try {
            bashScript = transpile();
            return BashExecutor.failableRun(bashScript);
        } catch (UserError e) {
            throw e;
        } catch (Throwable e) {
            String msg = "\nCouldn't run `%s`".formatted(bashScript);
            if (e.getMessage() != null) {
                msg += " because of\n`%s`".formatted(e.getMessage());
            }
            if (e.getCause() != null) {
                msg += "\n caused by `%s`".formatted(e.getCause().getMessage());
            }
            throw new BashpileUncheckedException(msg, e);
        }
    }

    /** Called by Picocli framework */
    @CommandLine.Command(name = "transpile", description = "Converts Bashpile lines to bash")
    public int transpileCommand() throws IOException {
        System.out.println(transpile());
        return 0;
    }

    public @Nonnull String transpile() throws IOException {
        return parse(getInputStream());
    }

    private @Nonnull InputStream getInputStream() throws FileNotFoundException {
        if (inputFile != null) {
            return new FileInputStream(inputFile);
        } else {
            System.out.println("Enter your bashpile program, ending with a newline and EOF (ctrl-D).");
            return System.in;
        }
    }

    public void setCommandLine(@Nonnull final CommandLine commandLine) {
        this.commandLine = commandLine;
    }
}
