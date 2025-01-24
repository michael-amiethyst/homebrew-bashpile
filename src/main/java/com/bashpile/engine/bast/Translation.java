package com.bashpile.engine.bast;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import com.bashpile.Strings;
import com.bashpile.engine.strongtypes.TranslationMetadata;
import com.bashpile.engine.strongtypes.Type;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.bashpile.Asserts.assertIsParagraph;
import static com.bashpile.Strings.lambdaAllLines;
import static com.bashpile.engine.strongtypes.TranslationMetadata.*;
import static com.bashpile.engine.strongtypes.Type.*;
import static org.apache.commons.lang3.StringUtils.stripStart;

/**
 * A target shell (e.g. Bash) translation of some Bashpile script.  Immutable.
 */
public class Translation implements TreeNode<String> {

    // static constants

    /**
     * The Bashpile version of NIL or NULL
     */
    public static final Translation EMPTY_TRANSLATION =
            new Translation("", EMPTY_TYPE, NORMAL);

    /**
     * An empty translation with an empty string an UNKNOWN type
     */
    public static final Translation UNKNOWN_TRANSLATION =
            new Translation("", UNKNOWN_TYPE, NORMAL);

    /**
     * A '\n' as a Translation
     */
    public static final Translation NEWLINE = toStringTranslation("\n");

    private static final Pattern INT_PATTERN = Pattern.compile("\\d+");

    private static final Pattern FLOAT_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");

    private static final Logger LOG = LogManager.getLogger(Translation.class);

    // class fields

    @Nonnull private final String body;

    @Nonnull private final Type type;

    @Nonnull private final Set<TranslationMetadata> metadata;

    @Nonnull private final List<Translation> children;

    // static initializers

    /**
     * @return A NORMAL STR Translation.
     */
    public static @Nonnull Translation toStringTranslation(@Nonnull final String text) {
        return new Translation(text, STR_TYPE, NORMAL);
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
        this(text, UNKNOWN_TYPE, Set.of());
    }

    public Translation(
            @Nonnull final String text,
            @Nonnull final Type type,
            @Nonnull final TranslationMetadata translationMetadata) {
        this(text, type, Set.of(translationMetadata));
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
            @Nonnull final Set<TranslationMetadata> metadata) {
        this(body, type, metadata, List.of());
    }

    /**
     * @param body     The target shell script (e.g. Bash) literal text.
     * @param type     The Bashpile type.  For Shell Strings and Command Substitutions this is the type of the result.
     *                 E.g. $(expr 1 + 1) could have a type of int.
     * @param metadata Further information on the type (e.g. is this a subshell?)
     * @param children The tree-children.
     */
    public Translation(
            @Nonnull final String body,
            @Nonnull final Type type,
            @Nonnull final Set<TranslationMetadata> metadata,
            @Nonnull final List<Translation> children) {
        this.body = body;
        this.type = type;
        this.metadata = metadata;
        this.children = List.copyOf(children);
    }

    /**
     * Accumulates all the stream translations' bodies into the result
     */
    public static @Nonnull Translation toTranslation(@Nonnull final Stream<Translation> stream) {
        return stream.reduce(Translation::add).orElseThrow();
    }

    // instance methods

    /**
     * Concatenates other's body, type and metadata to this object's
     */
    public @Nonnull Translation add(@Nonnull final TreeNode<String> node) {
        final List<Translation> modifiedChildren =
                Streams.concat(children.stream(), Stream.of((Translation) node)).toList();
        return new Translation(this.body, this.type, this.metadata, modifiedChildren);
    }

    @Override
    public TreeNode<String> addAll(Stream<TreeNode<String>> stream) {
        final List<Translation> modifiedChildren =
                Streams.concat(children.stream(), stream.map(x->(Translation) x)).toList();
        return new Translation(this.body, this.type, this.metadata, modifiedChildren);
    }

    @VisibleForTesting
    public List<TreeNode<String>> getChildren() {
        return children.stream().map(x -> (TreeNode<String>) x).toList();
    }

    // body instance methods

    /**
     * Replaces the body
     */
    public @Nonnull Translation body(@Nonnull final String nextBody) {
        // do not include children, assume that they were consumed during the creation of nextBody
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
            final String nextBody = stripStart(body, "${");
            return StringUtils.stripEnd(nextBody, "}");
        });
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
    // TODO feature/bast queue up lambdaBody methods to apply during getData/render so it can be re-rendered when the metadata changes
    public @Nonnull Translation lambdaBody(@Nonnull final Function<String, String> lambda) {
        final List<Translation> modifiedChildren = children.stream().map(tr -> tr.lambdaBody(lambda)).toList();
        // KEEP metadata
        return new Translation(lambda.apply(body), type, metadata, modifiedChildren);
    }

    /**
     * Apply arbitrary function to every line in the body.  A function is specified by the `str -> str` syntax.
     */
    public @Nonnull Translation lambdaBodyLines(@Nonnull final Function<String, String> lambda) {
        final List<Translation> modifiedChildren = children.stream().map(tr -> tr.lambdaBodyLines(lambda)).toList();
        // KEEP metadata
        return new Translation(lambdaAllLines(body, lambda), type, metadata, modifiedChildren);
    }

    /**
     * Ensures body is a paragraph
     */
    public @Nonnull Translation assertParagraphBody() {
        assertIsParagraph(body);
        return this;
    }

    /////////////////////////////////////////
    // type related instance methods
    /////////////////////////////////////////

    /**
     * Replaces the type.
     */
    public @Nonnull Translation type(@Nonnull final Type typecastType) {
        return new Translation(body, typecastType, metadata, children);
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

    ////////////////////////////////////
    // metadata related instance methods
    ////////////////////////////////////

    /** Returns a deep copy of the metadata */
    public @Nonnull Set<TranslationMetadata> getMetadata() {
        return new TreeSet<>(metadata);
    }

    /**
     * Replaces the type metadata
     */
    public @Nonnull Translation replaceMetadata(@Nonnull final TranslationMetadata meta) {
        return new Translation(body, type, Set.of(meta), children);
    }

    public @Nonnull Translation addMetadata(@Nonnull final TranslationMetadata meta) {
        return new Translation(body, type, Sets.union(metadata, Set.of(meta)), children);
    }

    public @Nonnull Translation removeMetadata(@Nonnull final TranslationMetadata meta) {
        var nextMetadata = new TreeSet<>(metadata);
        nextMetadata.remove(meta);
        return new Translation(body, type, nextMetadata, children);
    }

    public boolean hasMetadata() {
        return metadata.isEmpty();
    }

    public boolean hasMetadata(@Nonnull final TranslationMetadata meta) {
        return metadata.contains(meta);
    }

    public boolean metadataOnlyHas(@Nonnull final TranslationMetadata meta) {
        return metadata.size() == 1 && hasMetadata(meta);
    }

    /////////////////////////
    // Other instance methods
    /////////////////////////

    /**
     * Create an inline Translation if this is a {@link TranslationMetadata#NEEDS_INLINING} translation.
     * Does some other processing as well.
     *
     * @return Converts body to an inline and change the type metadata to {@link TranslationMetadata#INLINE}.
     */
    // TODO feature/bast - Remove external calls to make private
    public @Nonnull Translation inlineAsNeeded() {
        if (metadata.contains(NEEDS_INLINING)) {
            // function calls may have redirect to /dev/null if only side effects needed
            String nextBody = Strings.remove(body, ">/dev/null").stripTrailing();
            // add INLINE and remove NEEDS INLINING OFTEN
            var nextMetadata = new TreeSet<>(List.of(INLINE));
            nextMetadata.addAll(metadata);
            nextMetadata.remove(NEEDS_INLINING);
            // in Bash $((subshell)) is an arithmetic operator in Bash but $( (subshell) ) isn't
            return new Translation("$( %s )".formatted(nextBody), type, nextMetadata);
        } // else
        return this;
    }

    /** Only add quotes (as metadata) as needed */
    public @Nonnull Translation ensureQuoted() {
        Translation tr = new Translation(body, type, metadata, children);
        Predicate<TreeNode<String>> alreadyQuoted = c -> {
            final String renderedBody = ((Translation) c).body();
            return renderedBody.startsWith("\"") && renderedBody.endsWith("\"");
        };
        boolean allQuoted = tr.getChildren().stream().allMatch(alreadyQuoted);
        if (tr.hasMetadata(CALCULATION) || tr.body.equals("$@")) {
            allQuoted &= alreadyQuoted.test(tr);
        }
        return allQuoted ? tr : tr.addMetadata(TranslationMetadata.QUOTE);
    }

    @Override
    public String toString() {
        return getData();
    }

    @Override
    public String getData() {
        LOG.trace("Processing getData for hash {}", body.hashCode());
        if (metadata.contains(OPTION) && children.stream().allMatch(tr -> tr.metadata.contains(OPTION))) {
            final String stripChars = " -";
            return "-" + stripStart(body, stripChars)
                    + children.stream().map(tr -> stripStart(tr.body, stripChars)).collect(Collectors.joining());
        }
        final String processedBody = new Translation(body, type, metadata).inlineAsNeeded().body;
        String ret = processedBody + children.stream().map(Translation::getData).collect(Collectors.joining());
        if (!metadata.contains(QUOTE)) {
            return ret;
        } else {
            return """
                    "%s\"""".formatted(processedBody);
        }
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

    public @Nonnull String body() {
        return getData();
    }

    public @Nonnull Type type() {
        return type;
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
