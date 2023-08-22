package com.bashpile.engine;

import com.bashpile.BashpileParser;
import com.bashpile.Strings;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.bashpile.Strings.lambdaAllLines;
import static com.bashpile.Strings.lambdaFirstLine;
import static com.bashpile.engine.BashTranslationEngine.TAB;
import static com.bashpile.engine.LevelCounter.*;
import static com.bashpile.engine.LevelCounter.PRINT_LABEL;
import static com.bashpile.engine.Translation.*;
import static com.bashpile.engine.strongtypes.Type.INT;
import static com.bashpile.engine.strongtypes.Type.STR;
import static com.bashpile.engine.strongtypes.TypeMetadata.NORMAL;

public class BashTranslationHelper {

    /**
     * This Pattern has three matching groups.
     * They are everything before the command substitution, the command substitution, and everything after.
     */
    public static final Pattern COMMAND_SUBSTITUTION = Pattern.compile("(.*)(\\$\\(.*?\\))(.*)");

    /**
     * This single-line Pattern has three matching groups.
     * They are the start of the outer command substitution, the inner command substitution and the remainder of
     * the outer command substitution.
     * <br>
     * WARNING: bug on two different, non-nesting command substitutions
     */
    public static final Pattern NESTED_COMMAND_SUBSTITUTION =
            Pattern.compile("(?s)(\\$\\(.*?)(\\$\\(.*\\))(.*?\\))");

    /* package */ static final Function<Translation, Translation> unnestLambda = (tr) -> unnestOnMatch(tr, COMMAND_SUBSTITUTION);

    /* package */ static final Function<Translation, Translation> innerUnnestLambda =
            (tr) -> unnestOnMatch(tr, NESTED_COMMAND_SUBSTITUTION);

    private static final Logger LOG = LogManager.getLogger(BashTranslationHelper.class);

    /** Used to ensure variable names are unique */
    private static int subshellWorkaroundCounter = 0;

    // static methods

    /* package */ static String getBodyString(
            @Nonnull final BashpileParser.CreatesStatementContext ctx,
            @Nonnull final Translation shellString,
            @Nonnull final String filename,
            @Nonnull final BashpileVisitor visitor,
            @Nonnull final Stack<String> createFilenamesStack) {
        String preamble, check;
        boolean briefGuard = !shellString.hasPreamble();
        if (briefGuard) {
            // collapse with semicolons to one line
            preamble = Arrays.stream(shellString.preamble().trim().split("\n"))
                    .filter(str -> !str.trim().startsWith("#"))
                    .collect(Collectors.joining("; "));
            check = String.join("; ", shellString.body().trim().split("\n"));
            if (Strings.isNotEmpty(preamble)) {
                preamble += "; ";
            }
        } else {
            // preserve whitespace
            preamble = "\n    ## end of unnest\n" + shellString.lambdaPreambleLines(str -> TAB + str).preamble();
            check = shellString.lambdaBodyLines(str -> TAB + str).body();
        }

        // set noclobber avoids some race conditions
        String ifGuard;
        String variableName = null;
        if (ctx.typedId() != null) {
            variableName = ctx.typedId().Id().getText();
            ifGuard = "%s %s\nif %s=$(set -o noclobber; %s%s) 2> /dev/null; then".formatted(
                    getLocalText(), variableName, variableName, preamble, check);
        } else {
            ifGuard = "if (set -o noclobber; %s%s) 2> /dev/null; then".formatted(preamble, check);
        }

        // create our statements translation
        final Translation statements = ctx.statement().stream()
                .map(visitor::visit)
                .reduce(Translation::add)
                .orElseThrow()
                .assertParagraphBody()
                .assertNoBlankLinesInBody();
        // create an ifBody to put into the bodyTranslation
        // only one trap can be in effect at a time, so we keep a stack of all current filenames to delete
        String ifBody = """
                trap 'rm -f %s; exit 10' INT TERM EXIT
                ## wrapped body of creates statement
                %s
                ## end of wrapped body of creates statement
                rm -f %s
                trap - INT TERM EXIT""".formatted(
                String.join(" ", createFilenamesStack), statements.body(), filename);
        ifBody = lambdaAllLines(ifBody, str -> TAB + str);
        ifBody = lambdaFirstLine(ifBody, String::stripLeading);

        // `return` in an if statement doesn't work, so we need to `exit` if we're not in a function or subshell
        final String exitOrReturn = isTopLevelShell() && !in(BLOCK_LABEL) ? "exit" : "return";
        final String plainFilename = STRING_QUOTES.matcher(filename).replaceAll("").substring(1);
        final String errorDetails = variableName != null ? "  Output from attempted creation:\\n$" + variableName : "";
        String elseBody = """
                printf "Failed to create %s correctly.%s"
                rm -f %s
                %s 1""".formatted(plainFilename, errorDetails, filename, exitOrReturn);
        elseBody = lambdaAllLines(elseBody, str -> TAB + str);
        elseBody = lambdaFirstLine(elseBody, String::stripLeading);
        return """
                %s
                    %s
                else
                    %s
                fi
                declare -i __bp_exitCode=$?
                if [ "$__bp_exitCode" -ne 0 ]; then exit "$__bp_exitCode"; fi
                """.formatted(ifGuard, ifBody, elseBody);
    }

    /* package */ static @Nonnull Translation visitBodyStatements(
            @Nonnull final List<BashpileParser.StatementContext> statements,
            @Nonnull final BashpileVisitor visitor) {
        return statements.stream()
                .map(visitor::visit)
                .map(tr -> tr.lambdaBodyLines(str -> TAB + str))
                .reduce(Translation::add)
                .orElseThrow();
    }

    /* package */ static @Nonnull Translation createCommentTranslation(@Nonnull final String name, final int lineNumber) {
        return toLineTranslation("# %s, Bashpile line %d\n".formatted(name, lineNumber));
    }

    /* package */ static @Nonnull String getLocalText() {
        return getLocalText(false);
    }

    /* package */ static @Nonnull String getLocalText(final boolean reassignment) {
        final boolean indented = LevelCounter.in(BLOCK_LABEL);
        if (indented && !reassignment) {
            return "local ";
        } else if (!indented && !reassignment) {
            return "export ";
        } else { // reassignment
            return "";
        }
    }

    /* package */ static @Nonnull Translation subcommentTranslationOrDefault(
            final boolean subcommentNeeded, @Nonnull final String name) {
        if (subcommentNeeded) {
            return toLineTranslation("## %s\n".formatted(name));
        }
        return EMPTY_TRANSLATION;
    }

    /* package */ static @Nonnull Translation createHoistedCommentTranslation(
            @Nonnull final String name, final int lineNumber) {
        final String hoisted = LevelCounter.in(FORWARD_DECL_LABEL) ? " (hoisted)" : "";
        return toLineTranslation("# %s, Bashpile line %d%s\n".formatted(name, lineNumber, hoisted));
    }

    /* package */ static boolean isTopLevelShell() {
        return !in(CALC_LABEL) && !in(PRINT_LABEL);
    }

    /** Get the Bashpile script linenumber that ctx is found in. */
    /* package */ static int lineNumber(@Nonnull final ParserRuleContext ctx) {
        return ctx.start.getLine();
    }

    // unnesting static methods

    /* package */ static @Nonnull Translation unnestOnMatch(@Nonnull Translation tr, @Nonnull final Pattern pattern) {
        Translation ret = tr;
        // extract inner command substitution
        final Matcher bodyMatcher = pattern.matcher(ret.body());
        if (!bodyMatcher.find()) {
            return ret;
        }
        final Translation unnested = unnest(new Translation(bodyMatcher.group(2), STR, NORMAL));
        // replace group
        final String unnestedBody = Matcher.quoteReplacement(unnested.body());
        LOG.debug("Replacing with {}", unnestedBody);
        ret = ret.body(bodyMatcher.replaceFirst("$1%s$3".formatted(unnestedBody)));
        return ret.addPreamble(unnested.preamble());
    }

    /* package */ static @Nonnull Translation unnestAsNeeded(@Nonnull final Translation tr) {
        Translation ret = tr;
        while(NESTED_COMMAND_SUBSTITUTION.matcher(ret.body()).find()) {
            ret = innerUnnestLambda.apply(ret);
        }
        return ret;
    }

    /**
     * Subshell and inline errored exit codes are ignored in Bash despite all configurations.
     * This workaround explicitly propagates errored exit codes.
     * Unnests one level.
     *
     * @param tr The base translation.
     * @return A Translation where the preamble is <code>tr</code>'s body and the work-around.
     * The body is a Command Substitution of a created variable
     * that holds the results of executing <code>tr</code>'s body.
     */
    /* package */ static Translation unnest(@Nonnull final Translation tr) {
        // guard to check if unnest not needed
        if (!COMMAND_SUBSTITUTION.matcher(tr.body()).find()) {
            LOG.debug("Skipped unnest for " + tr.body());
            return tr;
        }

        if (NESTED_COMMAND_SUBSTITUTION.matcher(tr.body()).find()) {
            LOG.debug("Found nested command substitution in unnest: {}", tr.body());
            return unnestAsNeeded(tr);
        }

        // assign Strings to use in translations
        final String subshellReturn = "__bp_subshellReturn%d".formatted(subshellWorkaroundCounter);
        final String exitCodeName = "__bp_exitCode%d".formatted(subshellWorkaroundCounter++);

        // create 5 lines of translations
        final Translation subcomment = toLineTranslation("## unnest for %s\n".formatted(tr.body()));
        final Translation export     = toLineTranslation("export %s\n".formatted(subshellReturn));
        final Translation assign     = toLineTranslation("%s=%s\n".formatted(subshellReturn, tr.body()));
        final Translation exitCode   = toLineTranslation("%s=$?\n".formatted(exitCodeName));
        final Translation check      = toLineTranslation("""
                if [ "$%s" -ne 0 ]; then exit "$%s"; fi
                """.formatted(exitCodeName, exitCodeName));

        // add the lines up
        final Translation preambles = subcomment.add(export).add(assign).add(exitCode).add(check);

        // add the preambles and swap the body
        return tr.addPreamble(preambles.body()).body("${%s}".formatted(subshellReturn));
    }

    // typecast static methods

    /* package */ static Translation typecastBool(
            @Nonnull final Type castTo,
            @Nonnull Translation expression,
            @Nonnull final TypeError typecastError) {
        switch (castTo) {
            case BOOL -> {}
            case INT -> expression =
                    expression.body(expression.body().equalsIgnoreCase("true") ? "1" : "0");
            case FLOAT -> expression =
                    expression.body(expression.body().equalsIgnoreCase("true") ? "1.0" : "0.0");
            case STR -> expression = expression.quoteBody();
            default -> throw typecastError;
        }
        return expression;
    }

    @Nonnull
    /* package */ static Translation typecastInt(
            @Nonnull final Type castTo,
            @Nonnull Translation expression,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        // parse expression to a BigInteger
        BigInteger expressionValue;
        try {
            expressionValue = new BigInteger(expression.body());
        } catch (final NumberFormatException e) {
            throw new UserError(
                    "Couldn't parse %s to a FLOAT".formatted(expression.body()), lineNumber);
        }

        // cast
        switch (castTo) {
            case BOOL -> expression =
                    expression.body(!expressionValue.equals(BigInteger.ZERO) ? "true" : "false");
            case INT, FLOAT -> {}
            case STR -> expression = expression.quoteBody();
            default -> throw typecastError;
        }
        return expression;
    }

    @Nonnull
    /* package */ static Translation typecastFloat(
            @Nonnull final Type castTo,
            @Nonnull Translation expression,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        // parse expression as a BigDecimal
        BigDecimal expressionValue;
        try {
            expressionValue = new BigDecimal(expression.body());
        } catch (final NumberFormatException e) {
            throw new UserError("Couldn't parse %s to a FLOAT".formatted(expression.body()), lineNumber);
        }

        // cast
        switch (castTo) {
            case BOOL -> expression =
                    expression.body(expressionValue.compareTo(BigDecimal.ZERO) != 0 ? "true" : "false");
            case INT -> expression = expression.body(expressionValue.toBigInteger().toString());
            case FLOAT -> {}
            case STR -> expression = expression.quoteBody();
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static Translation typecastStr(
            @Nonnull final Type castTo,
            @Nonnull Translation expression,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        switch (castTo) {
            case BOOL -> {
                expression = expression.unquoteBody();
                if (Type.isNumberString(expression.body())) {
                    expression = typecastFloat(castTo, expression, lineNumber, typecastError);
                } else if (expression.body().equalsIgnoreCase("true")
                        || expression.body().equalsIgnoreCase("false")) {
                    expression = expression.body(expression.body().toLowerCase());
                } else {
                    throw new TypeError("""
                            Could not cast STR to BOOL.
                            Only 'true', 'false' and numbers in Strings allowed.
                            Text was %s.""".formatted(expression.body()), lineNumber);
                }
            }
            case INT -> {
                // no automatic rounding for things like `"2.5":int`
                expression = expression.unquoteBody();
                final Type foundType = Type.parseNumberString(expression.body());
                if (!INT.equals(foundType)) {
                    throw new TypeError("""
                        Could not cast FLOAT value in STR to INT.  Try casting to float first.  Text was %s."""
                            .formatted(expression.body()), lineNumber);
                }
            }
            case FLOAT -> {
                expression = expression.unquoteBody();
                // verify the body parses as a valid number
                try {
                    Type.parseNumberString(expression.body());
                } catch (NumberFormatException e) {
                    throw new TypeError("""
                        Could not cast STR to FLOAT.  Is not a FLOAT.  Text was %s."""
                            .formatted(expression.body()), lineNumber);
                }
            }
            case STR -> {}
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static void typecastUnknown(@Nonnull final Type castTo, @Nonnull final TypeError typecastError) {
        switch (castTo) {
            case BOOL, INT, FLOAT, STR -> {}
            default -> throw typecastError;
        }
    }
}
