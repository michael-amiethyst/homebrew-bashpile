package com.bashpile.engine;

import com.bashpile.Strings;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.engine.strongtypes.TypeMetadata;
import com.bashpile.exceptions.BashpileUncheckedAssertionException;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.bashpile.Asserts.*;
import static com.bashpile.Strings.lambdaAllLines;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * A target shell (e.g. Bash) translation of some Bashpile script.  Immutable.
 *
 * @param preamble This is text that needs to be emitted before the rest of the translation.<br>
 *                 This is to handle the case of a nested command substitution, since they are not supported well
 *                 in Bash (errored exit codes are ignored) we need to assign the inner command substitution to a
 *                 variable and have the <code>body</code> just be the variable.
 * @param body The target shell script (e.g. Bash) literal text.
 * @param type The Bashpile type.  For Shell Strings and Command Substitutions this is the type of the result.
 *             E.g. $(expr 1 + 1) could have a type of int.
 * @param typeMetadata Further information on the type (e.g. is this a subshell?)
 */
public record Translation(
        @Nonnull String preamble,
        @Nonnull String body,
        @Nonnull Type type,
        @Nonnull TypeMetadata typeMetadata) {

    // static constants

    /** The Bashpile version of NIL or NULL */
    public static final Translation EMPTY_TYPE = new Translation("", Type.EMPTY, TypeMetadata.NORMAL);

    /** An empty translation with an empty string an UNKNOWN type */
    public static final Translation EMPTY_TRANSLATION = new Translation("", Type.UNKNOWN, TypeMetadata.NORMAL);

    /** A '\n' as a Translation */
    public static final Translation NEWLINE = toLineTranslation("\n");

    /** A pattern of starting and ending double quotes */
    public static final Pattern STRING_QUOTES = Pattern.compile("^[\"']|[\"']$");

    // static methods

    /** Are ony translations Strings (STR) or UNKNOWN? */
    public static boolean maybeStringExpressions(@Nonnull final Translation... translations) {
        // if all strings the stream of not-strings will be empty
        return Stream.of(translations).allMatch(x -> x.type() == Type.STR || x.type() == Type.UNKNOWN);
    }

    /** Are any translations numeric (number, int or float) or UNKNOWN? */
    public static boolean maybeNumericExpressions(@Nonnull final Translation... translations) {
        // if all numbers the stream of not-numbers will be empty
        return Stream.of(translations).allMatch(x -> x.type().isNumeric() || x.type() == Type.UNKNOWN);
    }

    // static initializers

    /**
     * Asserts text is a collection of lines, with each ending with '\n', or the empty string.
     * @return A NORMAL STR Translation.
     */
    public static @Nonnull Translation toParagraphTranslation(@Nonnull final String text) {
        return new Translation(assertIsParagraph(text), Type.STR, TypeMetadata.NORMAL);
    }

    /**
     * Asserts text is a single line ending with '\n', or the empty string.
     * @return A NORMAL STR Translation.
     */
    public static @Nonnull Translation toLineTranslation(@Nonnull final String text) {
        return new Translation(assertIsLine(text), Type.STR, TypeMetadata.NORMAL);
    }

    // toPhraseTranslation not used/needed

    // constructors

    public Translation(@Nonnull final String text, @Nonnull final Type type, @Nonnull final TypeMetadata typeMetadata) {
        this("", text, type, typeMetadata);
    }

    /** Accumulates all the stream translations' preambles and bodies into the result */
    public static @Nonnull Translation toTranslation(
            @Nonnull final Stream<Translation> stream,
            @Nonnull final Type type,
            @Nonnull final TypeMetadata typeMetadata) {
        final Translation joined = stream.reduce(Translation::add).orElseThrow();
        return new Translation(joined.preamble, joined.body, type, typeMetadata);
    }

    // instance methods

    /** Concatenates other's preamble and body to this preamble and body */
    public @Nonnull Translation add(@Nonnull final Translation other) {
        return new Translation(preamble + other.preamble, body + other.body, type, typeMetadata);
    }

    // preamble instance methods

    /** Appends additionalPreamble to this object's preamble */
    public @Nonnull Translation addPreamble(@Nonnull final String additionalPreamble) {
        return new Translation(preamble + additionalPreamble, body, type, typeMetadata);
    }

    /** Ensures this translation has no preamble */
    public @Nonnull Translation assertEmptyPreamble() {
        if (hasPreamble()) {
            throw new BashpileUncheckedAssertionException("Found preamble in translation: " + this.body);
        }
        return this;
    }

    /** Checks if this translation has a preamble */
    public boolean hasPreamble() {
        return !isEmpty(preamble);
    }

    /** Apply arbitrary function to every line in the body.  A function is specified by the `str -> str` syntax. */
    public @Nonnull Translation lambdaPreambleLines(@Nonnull final Function<String, String> lambda) {
        return new Translation(lambdaAllLines(preamble, lambda), body, type, typeMetadata);
    }

    /** Prepends the preamble to the body */
    public @Nonnull Translation mergePreamble() {
        return new Translation(preamble + body, type, typeMetadata);
    }

    // body instance methods

    /** Replaces the body */
    public @Nonnull Translation body(@Nonnull final String nextBody) {
        return new Translation(preamble, nextBody, type, typeMetadata);
    }

    /** See {@link Strings#unescape(java.lang.String)} */
    public @Nonnull Translation unescapeBody() {
        return lambdaBody(Strings::unescape);
    }

    /** Put quotes around body */
    public @Nonnull Translation quoteBody() {
        return lambdaBody("\"%s\""::formatted);
    }

    /** Remove quotes around body */
    public @Nonnull Translation unquoteBody() {
        return lambdaBody(body -> STRING_QUOTES.matcher(body).replaceAll(""));
    }

    /** Put parenthesis around body */
    public @Nonnull Translation parenthesizeBody() {
        return lambdaBody("(%s)"::formatted);
    }

    /** Apply arbitrary function to body.  E.g. `str -> str`. */
    public @Nonnull Translation lambdaBody(@Nonnull final Function<String, String> lambda) {
        return new Translation(preamble, lambda.apply(body), type, typeMetadata);
    }

    /** Apply arbitrary function to every line in the body.  A function is specified by the `str -> str` syntax. */
    public @Nonnull Translation lambdaBodyLines(@Nonnull final Function<String, String> lambda) {
        return this.body(lambdaAllLines(body, lambda));
    }

    /** Ensures body is a paragraph */
    public @Nonnull Translation assertParagraphBody() {
        assertIsParagraph(body);
        return this;
    }

    /** Ensures body has no empty or blank lines and is not the empty string */
    public @Nonnull Translation assertNoBlankLinesInBody() {
        assertNoBlankLines(body);
        return this;
    }

    // type and typeMetadata instance methods

    /** Replaces the type */
    public @Nonnull Translation type(@Nonnull final Type typecastType) {
        return new Translation(preamble, body, typecastType, typeMetadata);
    }

    /** Replaces the type metadata */
    public @Nonnull Translation typeMetadata(@Nonnull final TypeMetadata meta) {
        return new Translation(preamble, body, type, meta);
    }

    /**
     * Create an inline Translation if this is a {@link TypeMetadata#NEEDS_INLINING_OFTEN} translation.
     * @return Converts body to an inline and change the type metadata to {@link TypeMetadata#INLINE}.
     */
    public @Nonnull Translation inlineAsNeeded(
            @Nonnull final Function<Translation, Translation> bodyLambda) {
        if (typeMetadata.equals(TypeMetadata.NEEDS_INLINING_OFTEN)) {
            return bodyLambda.apply(new Translation(preamble, "$(%s)".formatted(body), type, TypeMetadata.INLINE));
        } // else
        return this;
    }

    @Override
    public String toString() {
        return assertEmptyPreamble().body;
    }
}
