package com.bashpile.maintests;

import com.bashpile.Asserts;
import com.bashpile.BashpileMainHelper;
import com.bashpile.Strings;
import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.UserError;
import com.bashpile.shell.BashShell;
import com.bashpile.shell.ExecutionResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.bashpile.engine.BashTranslationHelper.COMMAND_SUBSTITUTION;
import static com.bashpile.engine.BashTranslationHelper.NESTED_COMMAND_SUBSTITUTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Base class for Bashpile Tests */
abstract public class BashpileTest {

    /** Full match for Bash comments ('#') */
    protected static final Pattern END_OF_LINE_COMMENT = Pattern.compile("^[^ #]+#.*$");

    private static final Logger LOG = LogManager.getLogger(BashpileTest.class);

    /** Asserts the exit code is 0 */
    protected static void assertSuccessfulExitCode(@Nonnull final ExecutionResults executionResults) {
        assertEquals(ExecutionResults.SUCCESS, executionResults.exitCode(),
                "Found failing (non-0) exit code: %s.  Full text results:\n%s".formatted(
                        executionResults.exitCode(), executionResults.stdout()));
    }

    /** Asserts exit code is NOT zero */
    protected static void assertFailedExitCode(@Nonnull final ExecutionResults executionResults) {
        assertNotEquals(ExecutionResults.SUCCESS, executionResults.exitCode(),
                "Found successful exit code (0) when expecting errored exit code.  Full text results:\n%s".formatted(
                        executionResults.stdout()));
    }

    /**
     * Couldn't find an off the shelf linter to check for correct indents.  Maybe a Bash parser would be better?
     */
    @SuppressWarnings("all") // Intellij doesn't like .forEachOrdered for some reason
    protected static void assertCorrectFormatting(@Nonnull final ExecutionResults executionResults) {
        assertCorrectIndents(executionResults);

        // TODO break other loops into their own methods
        final AtomicReference<List<Long>> erroredLines = new AtomicReference<>(new ArrayList<>(10));
        final AtomicLong i = new AtomicLong(1);
        // check for nested command substitutions
        i.set(1);
        executionResults.stdinLines().stream().forEachOrdered(line -> {
            if (line.trim().charAt(0) != '#'
                    && !line.contains("(set -o noclobber")
                    && NESTED_COMMAND_SUBSTITUTION.matcher(line).find()) {
                LOG.error("Found nested command substitution, line {}, text {}", i, line);
                erroredLines.get().add(i.getAndIncrement());
            }
        });

        // check for unnecessary unnested command substitutions
        i.set(1);
        executionResults.stdinLines().stream().forEachOrdered(line -> {
            if (line.trim().startsWith("__bp_subshellReturn") && !COMMAND_SUBSTITUTION.matcher(line).find()) {
                LOG.error("Unneeded unnest, line {}, text {}", i, line);
                erroredLines.get().add(i.getAndIncrement());
            }
        });

        // check for missing ifs
        i.set(1);
        executionResults.stdinLines().stream().forEachOrdered(line -> {
            if (!line.trim().contains("if") && line.endsWith("; then") ) {
                LOG.error("Mangled if-then found, line {}, text {}", i, line);
                erroredLines.get().add(i.getAndIncrement());
            }
        });

        final String message = "Bad formatting on lines " + erroredLines.get().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        Asserts.assertEmpty(erroredLines.get(), message);
    }

    /** Runs {@code bashText} in a Bash environment */
    protected @Nonnull ExecutionResults runText(@Nonnull final String bashText, @Nullable String... args) {
        LOG.debug("Start of:\n{}", bashText);
        try {
            return execute(BashpileMainHelper.transpileScript(bashText), args);
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    /** Runs {@code bashText} in a Bash environment in the background */
    protected @Nonnull BashShell runTextAsync(@Nonnull final String bashText) {
        LOG.debug("Starting background threads for:\n{}", bashText);
        try {
            return executeAsync(BashpileMainHelper.transpileScript(bashText));
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    /** Runs {@code file} from src/test/resources/scripts as a script in a Bash environment. */
    protected @Nonnull ExecutionResults runPath(@Nonnull final Path file) {
        final Path filename = !file.isAbsolute() ? Path.of("src/test/resources/scripts/" + file) : file;
        LOG.debug("Start of {}", filename);
        try {
            return execute(BashpileMainHelper.transpileNioFile(filename), null);
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    // helpers

    /**
     * Helper to assertCorrectFormatting.
     * @see #assertCorrectFormatting(ExecutionResults)
     */
    private static void assertCorrectIndents(@Nonnull final ExecutionResults executionResults) {
        final AtomicReference<List<Long>> erroredLines = new AtomicReference<>(new ArrayList<>(10));
        final AtomicLong indentLevel = new AtomicLong(0);
        final AtomicLong i = new AtomicLong(1);
        final AtomicBoolean inCase = new AtomicBoolean(false);

        // check for correct indents
        executionResults.stdinLines().stream().forEachOrdered(line -> {
            final int spaces = line.length() - line.stripLeading().length();
            if (spaces % 4 != 0 || Strings.isBlank(line)) {
                erroredLines.get().add(i.getAndIncrement());
                return;
            }
            final long tabs = spaces / 4;
            final String[] tokens = line.stripLeading().split(" ");
            final String firstToken = tokens[0];
            final String lastToken = tokens[tokens.length - 1];

            // check for increments
            final boolean isStartOfFunctionBlock =
                    firstToken.matches("\\w(?:\\w|\\d)+") && "{".equals(lastToken);
            final boolean isNestedIf = line.contains("if") && !line.contains("elif") && lastToken.equals("then");
            final boolean casePattern = inCase.get() && lastToken.endsWith(")");
            if (List.of("if", "while", "case").contains(firstToken)
                    || isStartOfFunctionBlock || isNestedIf || casePattern) {
                if (firstToken.equals("case")) {
                    inCase.set(true);
                }
                if (tabs != indentLevel.get()) {
                    erroredLines.get().add(i.get());
                }
                // generated code uses a Bash if all on one line (e.g. starts with if and ends with fi)
                if (!line.endsWith("fi")) {
                    indentLevel.getAndIncrement();
                }
                i.getAndIncrement();
                return;
            } // else

            // check for middle statements (e.g. else in an if-then-else statement)
            if (List.of("elif", "else").contains(firstToken)) {
                if (tabs != indentLevel.get() - 1) {
                    erroredLines.get().add(i.get());
                }
                i.getAndIncrement();
                return;
            } // else

            // check for decrements
            if (List.of("fi", "done", "}", "};", "esac").contains(firstToken) || firstToken.startsWith("fi)")) {
                if (firstToken.equals("esac")) {
                    inCase.set(false);
                }
                indentLevel.getAndDecrement();
                if (tabs != indentLevel.get()) {
                    erroredLines.get().add(i.get());
                }
                i.getAndIncrement();
                return;
            }
            if (firstToken.equals(";;")) {
                if (tabs != indentLevel.get()) {
                    erroredLines.get().add(i.get());
                }
                indentLevel.getAndDecrement();
                i.getAndIncrement();
                return;
            }
            if (firstToken.equals(")") || firstToken.equals("then")) {
                if (tabs != indentLevel.get() - 1) {
                    erroredLines.get().add(i.get());
                }
                i.getAndIncrement();
                return;
            }

            // check for 'regular' lines
            if (tabs != indentLevel.get()) {
                erroredLines.get().add(i.get());
            }
            i.getAndIncrement();
        });

        final String message = "Bad indenting on lines " + erroredLines.get().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        Asserts.assertEmpty(erroredLines.get(), message);
    }

    private @Nonnull ExecutionResults execute(@Nonnull final String bashScript, @Nullable final String[] args) {
        LOG.debug("In {}", System.getProperty("user.dir"));
        try {
            return BashShell.runAndJoin(bashScript, args);
        } catch (UserError | AssertionError e) {
            throw e;
        } catch (Throwable e) {
            throw createExecutionException(e, bashScript, args);
        }
    }

    private @Nonnull BashShell executeAsync(@Nonnull final String bashScript) {
        LOG.debug("In {}", System.getProperty("user.dir"));
        try {
            return BashShell.runAsync(bashScript, null);
        } catch (UserError | AssertionError e) {
            throw e;
        } catch (Throwable e) {
            throw createExecutionException(e, bashScript, null);
        }
    }

    private static BashpileUncheckedException createExecutionException(
            @Nonnull final Throwable e, @Nullable final String bashScript, @Nullable String[] args) {
        if (e.getMessage() != null && e.getMessage().contains("shellcheck") && e.getMessage().contains("not found")) {
            return new BashpileUncheckedException("Please install shellcheck (e.g. via `brew install shellcheck`)");
        }
        String msg;
        if (bashScript != null) {
            args = Objects.requireNonNullElse(args, new String[0]);
            msg = "\nCouldn't run Args: (%s), Script: %s".formatted(String.join(" ", args), bashScript);
        } else {
            msg = "\nCouldn't parse input";
        }
        if (e.getMessage() != null) {
            msg += " because of:\n`%s`".formatted(e.getMessage().trim());
        }
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            msg += "\n caused by `%s`".formatted(e.getCause().getMessage().trim());
        }
        return new BashpileUncheckedException(msg, e);
    }
}
