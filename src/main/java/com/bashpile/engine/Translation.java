package com.bashpile.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import com.bashpile.Strings;
import com.bashpile.engine.strongtypes.TranslationMetadata;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.BashpileUncheckedAssertionException;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.StringUtils;

import static com.bashpile.Asserts.assertIsParagraph;
import static com.bashpile.Strings.lambdaAllLines;
import static com.bashpile.engine.strongtypes.Type.*;

/**
 * A target shell (e.g. Bash) translation of some Bashpile script.  Immutable.
 */
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
    public static final Translation NEWLINE = toStringTranslation("\n");

    private static final Pattern INT_PATTERN = Pattern.compile("\\d+");

    private static final Pattern FLOAT_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");

    @Nonnull private final String body;

    @Nonnull private final Type type;

    @Nonnull private final List<TranslationMetadata> metadata;

    // static initializers

    /**
     * @return A NORMAL STR Translation.
     */
    public static @Nonnull Translation toStringTranslation(@Nonnull final String text) {
        return new Translation(text, STR_TYPE, TranslationMetadata.NORMAL);
    }

    // static methods

    /**
     * Are ony translations Strings (STR) or UNKNOWN?
     */
    public static boolean areStringExpressions(@Nonnull final Translation... translations) {
        // if all strings the stream of not-strings will be empty
        return Stream.of(translations)
                .map(Translation::convertUnknownToDetectedType)
                .allMatch(Translation::isStr);
    }

    /**
     * Are all translations INTs?
     */
    public static boolean areIntExpressions(@Nonnull final Translation... translations) {
        return Stream.of(translations)
                .map(Translation::convertUnknownToDetectedType)
                .allMatch(x -> x.type().isInt());
    }

    /**
     * Are all translations numeric (number, int or float)?
     */
    public static boolean areNumericExpressions(@Nonnull final Translation... translations) {
        return Stream.of(translations)
                .map(Translation::convertUnknownToDetectedType)
                .allMatch(x -> x.type().isNumeric());
    }

    // constructors

    public Translation(@Nonnull final String text) {
        this(text, UNKNOWN_TYPE, List.of());
    }

    public Translation(
            @Nonnull final String text,
            @Nonnull final Type type,
            @Nonnull final TranslationMetadata translationMetadata) {
        this(text, type, List.of(translationMetadata));
    }

    /**
     * @param body     The target shell script (e.g. Bash) literal text.
     * @param type     The Bashpile type.  For Shell Strings and Command Substitutions this is the type of the result.
     *                 E.g. $(expr 1 + 1) could have a type of int.
     * @param metadata Further information on the type (e.g. is this a subshell?)
     */
    public Translation(
            @Nonnull final String body,
            @Nonnull final Type type,
            @Nonnull final List<TranslationMetadata> metadata) {
        this.body = body;
        this.type = type;
        this.metadata = metadata;
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
        nextType = nextType.isNumber() && other.type.isNumeric() ? other.type : nextType;
        return new Translation(body + other.body, nextType, nextMetadata);
    }

    // preamble instance methods

    /**
     * Appends additionalPreamble to this object's preamble
     */
    public @Nonnull Translation addPreamble(@Nonnull final String additionalPreamble) {
        return new Translation(body, type, metadata);
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
        return false;
    }

    /**
     * Prepends the preamble to the body
     */
    public @Nonnull Translation mergePreamble() {
        return new Translation(body, type, metadata);
    }

    // body instance methods

    /**
     * Replaces the body
     */
    public @Nonnull Translation body(@Nonnull final String nextBody) {
        return new Translation(nextBody, type, metadata);
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
     * Remove surrounding `${}`s.
     */
    public @Nonnull Translation removeVariableBrackets() {
        return lambdaBody(body -> {
            final String nextBody = StringUtils.stripStart(body, "${");
            return StringUtils.stripEnd(nextBody, "}");
        });
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
        return new Translation(lambda.apply(body), type, metadata);
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

    // type and typeMetadata instance methods

    /**
     * Replaces the type.
     */
    public @Nonnull Translation type(@Nonnull final Type typecastType) {
        return new Translation(body, typecastType, metadata);
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

    /** Does this expand a list or reference all elements of a list? */
    public boolean isListAccess() {
        return body().contains("$@") || body().contains("[@]");
    }

    /** Is the type UNKNOWN? */
    public boolean isUnknown() {
        return type.isUnknown();
    }

    /** Is the type NOT_FOUND? */
    public boolean isNotFound() {
        return type.isNotFound();
    }

    /** Is the type an integer? */
    public boolean isInt() {
        return type.isInt();
    }

    /** Is the type a number, but we don't know if it's an Int or a Float? */
    public boolean isNumeric() {
        return type.isNumeric();
    }

    /** Is the type a String? */
    public boolean isStr() {
        return type.isStr();
    }

    /**
     * Replaces the type metadata
     */
    public @Nonnull Translation metadata(@Nonnull final TranslationMetadata meta) {
        return new Translation(body, type, List.of(meta));
    }

    /**
     * Replaces the type metadata
     */
    public @Nonnull Translation metadata(@Nonnull final List<TranslationMetadata> meta) {
        return new Translation(body, type, meta);
    }

    /**
     * Create an inline Translation if this is a {@link TranslationMetadata#NEEDS_INLINING_OFTEN} translation.
     * Does some other processing as well.
     *
     * @return Converts body to an inline and change the type metadata to {@link TranslationMetadata#INLINE}.
     */
    public @Nonnull Translation inlineAsNeeded() {
        if (metadata.contains(TranslationMetadata.NEEDS_INLINING_OFTEN)) {
            // function calls may have redirect to /dev/null if only side effects needed
            String nextBody = Strings.remove(body, ">/dev/null").stripTrailing();
            // add INLINE and remove NEEDS INLINING OFTEN
            var nextMetadata = new ArrayList<>(List.of(TranslationMetadata.INLINE));
            nextMetadata.addAll(metadata);
            nextMetadata.remove(TranslationMetadata.NEEDS_INLINING_OFTEN);
            // in Bash $((subshell)) is an arithmetic operator in Bash but $( (subshell) ) isn't
            return new Translation("$( %s )".formatted(nextBody), type, nextMetadata);
        } // else
        return this;
    }

    @Override
    public String toString() {
        return assertEmptyPreamble().body;
    }

    // helpers

    /** Tries to match tr's body to an INT or a NUMBER.  Defaults to String.  Doesn't modify non-unknown translations */
    private static @Nonnull Translation convertUnknownToDetectedType(Translation tr) {
        if (tr.isUnknown() && INT_PATTERN.matcher(tr.body).matches()) {
            return tr.type(INT_TYPE);
        } else if (tr.isUnknown() && FLOAT_PATTERN.matcher(tr.body).matches()) {
            return tr.type(FLOAT_TYPE);
        } else if (tr.isUnknown()) {
            return tr.type(STR_TYPE);
        } else {
            return tr;
        }
    }

    /**
     * Drains the preamble; blanks out the preamble as a side effect to indicate that it has been handled and not lost.
     */
    public @Nonnull String preamble() {
        return "";
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
        return Objects.equals(this.body, that.body) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(body, type, metadata);
    }

}
