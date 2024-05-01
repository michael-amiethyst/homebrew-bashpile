package com.bashpile.engine;

import com.bashpile.Strings;
import com.bashpile.engine.strongtypes.TranslationMetadata;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.BashpileUncheckedAssertionException;
import com.google.common.collect.Streams;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.bashpile.Asserts.*;
import static com.bashpile.Strings.lambdaAllLines;
import static com.bashpile.engine.strongtypes.Type.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * A target shell (e.g. Bash) translation of some Bashpile script.  Immutable.
 */
// TODO in body() ensure that preamble is empty
public class Translation {

    // static constants

    /**
     * The Bashpile version of NIL or NULL
     */
    public static final Translation EMPTY_TRANSLATION =
            new Translation("", EMPTY_TYPE, TranslationMetadata.NORMAL);

    /**
     * An empty translation with an empty string an UNKNOWN type
     */
    public static final Translation UNKNOWN_TRANSLATION =
            new Translation("", UNKNOWN_TYPE, TranslationMetadata.NORMAL);

    /**
     * A '\n' as a Translation
     */
    public static final Translation NEWLINE = toLineTranslation("\n");

    private static final Pattern NUMBER = Pattern.compile("[\\d().]+");

    @Nonnull private final String preamble;

    @Nonnull private final String body;

    @Nonnull private final Type type;

    @Nonnull private final List<TranslationMetadata> metadata;

    /**
     * @param preamble This is text that needs to be emitted before the rest of the translation.<br>
     *                 This is to handle the case of a nested command substitution, since they are not supported well
     *                 in Bash (errored exit codes are ignored) we need to assign the inner command substitution to a
     *                 variable and have the <code>body</code> just be the variable.
     * @param body     The target shell script (e.g. Bash) literal text.
     * @param type     The Bashpile type.  For Shell Strings and Command Substitutions this is the type of the result.
     *                 E.g. $(expr 1 + 1) could have a type of int.
     * @param metadata Further information on the type (e.g. is this a subshell?)
     */
    public Translation(
            @Nonnull String preamble,
            @Nonnull String body,
            @Nonnull Type type,
            @Nonnull List<TranslationMetadata> metadata) {
        this.preamble = preamble;
        this.body = body;
        this.type = type;
        this.metadata = metadata;
    }

    // static methods

    /**
     * Are ony translations Strings (STR) or UNKNOWN?
     */
    public static boolean areStringExpressions(@Nonnull final Translation... translations) {
        // if all strings the stream of not-strings will be empty
        return Stream.of(translations)
                .map(Translation::parseUnknown)
                .allMatch(Translation::isStr);
    }

    /**
     * Are any translations numeric (number, int or float) or UNKNOWN?
     */
    public static boolean areNumericExpressions(@Nonnull final Translation... translations) {
        // if all numbers the stream of not-numbers will be empty
        return Stream.of(translations).map(Translation::parseUnknown).allMatch(x -> x.type().isNumeric());
    }

    // static initializers

    /**
     * Asserts text is a collection of lines, with each ending with '\n', or the empty string.
     *
     * @return A NORMAL STR Translation.
     */
    public static @Nonnull Translation toParagraphTranslation(@Nonnull final String text) {
        return new Translation(assertIsParagraph(text), STR_TYPE, TranslationMetadata.NORMAL);
    }

    /**
     * Asserts text is a single line ending with '\n', or the empty string.
     *
     * @return A NORMAL STR Translation.
     */
    public static @Nonnull Translation toLineTranslation(@Nonnull final String text) {
        return new Translation(assertIsLine(text), STR_TYPE, TranslationMetadata.NORMAL);
    }

    /**
     * Asserts text has no linebreaks at all
     *
     * @return A NORMAL STR Translation.
     */
    public static @Nonnull Translation toStringTranslation(@Nonnull final String text) {
        assertNoMatch(text, Pattern.compile("[^\n]*\n"));
        return new Translation(text, STR_TYPE, TranslationMetadata.NORMAL);
    }

    // toPhraseTranslation not used/needed

    // constructors

    public Translation(@Nonnull final String text) {
        this(text, UNKNOWN_TYPE, List.of());
    }

    public Translation(
            @Nonnull final String text,
            @Nonnull final Type type,
            @Nonnull final TranslationMetadata translationMetadata) {
        this("", text, type, List.of(translationMetadata));
    }

    public Translation(
            @Nonnull final String text,
            @Nonnull final Type type,
            @Nonnull final List<TranslationMetadata> translationMetadata) {
        this("", text, type, translationMetadata);
    }

    /**
     * Accumulates all the stream translations' preambles and bodies into the result
     */
    public static @Nonnull Translation toTranslation(
            @Nonnull final Stream<Translation> stream,
            @Nonnull final Type type,
            @Nonnull final TranslationMetadata translationMetadata) {
        final Translation joined = stream.reduce(Translation::add).orElseThrow();
        return new Translation(joined.preamble, joined.body, type, List.of(translationMetadata));
    }

    /**
     * Accumulates all the stream translations' preambles and bodies into the result
     */
    public static @Nonnull Translation toTranslation(@Nonnull final Stream<Translation> stream) {
        return stream.reduce(Translation::add).orElseThrow();
    }

    // instance methods

    /**
     * Concatenates other's preamble and body to this preamble and body
     */
    public @Nonnull Translation add(@Nonnull final Translation other) {
        final List<TranslationMetadata> nextMetadata =
                Streams.concat(metadata.stream(), other.metadata.stream()).toList();
        // favor anything over UNKNOWN
        Type nextType = type;
        nextType = nextType.isUnknown() ? other.type : nextType;
        // favor INT or FLOAT over NUMBER
        nextType = nextType.isGenericNumberType() && other.type.isNumeric() ? other.type : nextType;
        return new Translation(preamble + other.preamble, body + other.body, nextType, nextMetadata);
    }

    // preamble instance methods

    /**
     * Appends additionalPreamble to this object's preamble
     */
    public @Nonnull Translation addPreamble(@Nonnull final String additionalPreamble) {
        return new Translation(preamble + additionalPreamble, body, type, metadata);
    }

    /**
     * Ensures this translation has no preamble
     */
    public @Nonnull Translation assertEmptyPreamble() {
        if (hasPreamble()) {
            throw new BashpileUncheckedAssertionException("Found preamble in translation: " + this.body);
        }
        return this;
    }

    /**
     * Checks if this translation has a preamble
     */
    public boolean hasPreamble() {
        return !isEmpty(preamble);
    }

    /**
     * Apply arbitrary function to every line in the body.  A function is specified by the `str -> str` syntax.
     */
    public @Nonnull Translation lambdaPreambleLines(@Nonnull final Function<String, String> lambda) {
        return new Translation(lambdaAllLines(preamble, lambda), body, type, metadata);
    }

    /**
     * Prepends the preamble to the body
     */
    public @Nonnull Translation mergePreamble() {
        return new Translation(preamble + body, type, metadata);
    }

    // body instance methods

    /**
     * Replaces the body
     */
    public @Nonnull Translation body(@Nonnull final String nextBody) {
        return new Translation(preamble, nextBody, type, metadata);
    }

    /**
     * See {@link Strings#unescape(String)}
     */
    public @Nonnull Translation unescapeBody() {
        return lambdaBody(Strings::unescape);
    }

    /**
     * Put quotes around body
     */
    public @Nonnull Translation quoteBody() {
        return lambdaBody("\"%s\""::formatted);
    }

    /**
     * Remove quotes around body
     */
    public @Nonnull Translation unquoteBody() {
        return lambdaBody(Strings::unquote);
    }

    /**
     * Put parenthesis around body
     */
    public @Nonnull Translation parenthesizeBody() {
        return lambdaBody("(%s)"::formatted);
    }

    /**
     * Adds to the start of the current options or creates the option at the start
     */
    public @Nonnull Translation addOption(final String additionalOption) {
        return lambdaBody(str ->
                str.contains("-") ? str.replace("-", "-" + additionalOption) : "-" + additionalOption + str);
    }

    /**
     * Change index from one string with all data to a true array.
     * @see <a href="https://stackoverflow.com/questions/52590446/bash-array-using-vs-difference-between-the-two">StackOverflow, Bash Arrays -- * vs @</a>
     * @return this
     */
    public @Nonnull Translation toTrueArray() {
        Translation ret = lambdaBody(x -> x.replace("$*", "$@"));
        if (type.isList()) {
            return ret.lambdaBody(x -> x.replace("[*]", "[@]"));
        } // else
        return ret;
    }

    /**
     * Change index from one string with all data to a true array.
     * @see <a href="https://stackoverflow.com/questions/52590446/bash-array-using-vs-difference-between-the-two">StackOverflow, Bash Arrays -- * vs @</a>
     * @return this
     */
    public @Nonnull Translation toStringArray() {
        Translation ret = lambdaBody(x -> x.replace("$@", "$*"));
        return ret.lambdaBody(x -> x.replace("[@]", "[*]"));
    }

    /**
     * Apply arbitrary function to body.  E.g. `str -> str`.
     */
    public @Nonnull Translation lambdaBody(@Nonnull final Function<String, String> lambda) {
        return new Translation(preamble, lambda.apply(body), type, metadata);
    }

    /**
     * Apply arbitrary function to every line in the body.  A function is specified by the `str -> str` syntax.
     */
    public @Nonnull Translation lambdaBodyLines(@Nonnull final Function<String, String> lambda) {
        return this.body(lambdaAllLines(body, lambda));
    }

    /**
     * Ensures body is a paragraph
     */
    public @Nonnull Translation assertParagraphBody() {
        assertIsParagraph(body);
        return this;
    }

    /**
     * Ensures body has no empty or blank lines and is not the empty string
     */
    public @Nonnull Translation assertNoBlankLinesInBody() {
        assertNoBlankLines(body);
        return this;
    }

    // type and typeMetadata instance methods

    /**
     * Replaces the type.
     */
    public @Nonnull Translation type(@Nonnull final Type typecastType) {
        return new Translation(preamble, body, typecastType, metadata);
    }

    /** Is the type basic (e.g. not a List, Hash or Ref)? */
    public boolean isBasicType() {
        return type.isBasic();
    }

    /** Is this a list / Bash Array? */
    public boolean isList() {
        return type.isList();
    }

    /** Is this a ListOf translation?  (E.g. created by the syntax `listOf(...)`)*/
    public boolean isListOf() {
        return this instanceof ListOfTranslation;
    }

    /** Is the type UNKNOWN? */
    public boolean isUnknown() {
        return type.isUnknown();
    }

    /** Is the type NOT_FOUND? */
    public boolean isNotFound() {
        return type.isNotFound();
    }

    /** Is the type a String? */
    public boolean isStr() {
        return type.isStr();
    }

    /**
     * Replaces the type metadata
     */
    public @Nonnull Translation metadata(@Nonnull final TranslationMetadata meta) {
        return new Translation(preamble, body, type, List.of(meta));
    }

    /**
     * Replaces the type metadata
     */
    public @Nonnull Translation metadata(@Nonnull final List<TranslationMetadata> meta) {
        return new Translation(preamble, body, type, meta);
    }

    /**
     * Create an inline Translation if this is a {@link TranslationMetadata#NEEDS_INLINING_OFTEN} translation.
     *
     * @param bodyLambda How to unwind if we need to add a command substitution.
     * @return Converts body to an inline and change the type metadata to {@link TranslationMetadata#INLINE}.
     */
    public @Nonnull Translation inlineAsNeeded(
            @Nonnull final Function<Translation, Translation> bodyLambda) {
        if (metadata.contains(TranslationMetadata.NEEDS_INLINING_OFTEN)) {
            // in Bash $((subshell)) is an arithmetic operator in Bash but $( (subshell) ) isn't
            String nextBody = Strings.addSpacesAroundParenthesis(body);
            // function calls may have redirect to /dev/null if only side effects needed
            nextBody = Strings.removeEnd(nextBody, ">/dev/null").stripTrailing();
            return bodyLambda.apply(
                    new Translation(preamble, "$(%s)".formatted(nextBody), type, List.of(TranslationMetadata.INLINE)));
        } // else
        return this;
    }

    @Override
    public String toString() {
        return assertEmptyPreamble().body;
    }

    // helpers

    private static @Nonnull Translation parseUnknown(Translation tr) {
        if (tr.isUnknown() && NUMBER.matcher(tr.body).matches()) {
            return tr.type(NUMBER_TYPE);
        } else if (tr.isUnknown()) {
            return tr.type(STR_TYPE);
        } else {
            return tr;
        }
    }

    public @Nonnull String preamble() {
        return preamble;
    }

    public @Nonnull String body() {
        return body;
    }

    public @Nonnull Type type() {
        return type;
    }

    public @Nonnull List<TranslationMetadata> metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Translation) obj;
        return Objects.equals(this.preamble, that.preamble) &&
                Objects.equals(this.body, that.body) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preamble, body, type, metadata);
    }

}
