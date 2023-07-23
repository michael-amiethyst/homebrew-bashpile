package com.bashpile.engine;

import com.bashpile.engine.strongtypes.Type;
import com.bashpile.engine.strongtypes.TypeMetadata;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.bashpile.StringUtils.join;
import static com.bashpile.StringUtils.unescape;

/**
 * Decorator pattern for a String.
 *
 * @param body The target shell script (e.g. Bash) literal text.
 * @param type The Bashpile type.  For Shell Strings and Command Substitutions this is the type of the result.
 *             E.g. $(expr 1 + 1) could have a type of int.
 * @param typeMetadata Further information of the type (e.g. is this a subshell?)
 * @param preamble This is text that needs to be emitted before the rest of the translation.<br>
 *                          This is to handle the case of a nested command substitution, since they are not supported
 *                          in Bash we need to assign the inner command substitution to a variable (while handling bad
 *                          exit codes, another work-around) and have the <code>body</code> just be the variable.
 */
public record Translation(
        @Nonnull String preamble,
        @Nonnull String body,
        @Nonnull Type type,
        @Nonnull TypeMetadata typeMetadata) {

    public static final Translation EMPTY_TYPE = new Translation("", Type.EMPTY, TypeMetadata.NORMAL);
    public static final Translation EMPTY_TRANSLATION = new Translation("", Type.UNKNOWN, TypeMetadata.NORMAL);

    private static final Pattern STRING_QUOTES = Pattern.compile("^\"|\"$");

    public Translation(@Nonnull final String text, @Nonnull final Type type, @Nonnull final TypeMetadata typeMetadata) {
        this("", text, type, typeMetadata);
    }

    public static Translation toTranslation(
            @Nonnull final Stream<Translation> stream,
            @Nonnull final Type type,
            @Nonnull final TypeMetadata typeMetadata) {
        final Translation joined = stream.reduce(Translation::add).orElseThrow();
        return new Translation(joined.preamble, joined.body, type, typeMetadata);
    }

    public static Translation toStringTranslation(final String... text) {
        return new Translation(join(text), Type.STR, TypeMetadata.NORMAL);
    }

    public static boolean areStringExpressions(@Nonnull final Translation... translations) {
        // if all strings the stream of not-strings will be empty
        return Stream.of(translations)
                .filter(x -> x.type() != Type.STR && x.type() != Type.UNKNOWN)
                .findAny()
                .isEmpty();
    }

    public static boolean areNumberExpressions(@Nonnull final Translation... translations) {
        // if all numbers the stream of not-numbers will be empty
        return Stream.of(translations)
                .filter(x -> !x.type().isNumeric() && x.type() != Type.UNKNOWN)
                .findAny()
                .isEmpty();
    }

    public Translation add(@Nonnull final Translation other) {
        return new Translation(preamble + other.preamble, body + other.body, type, typeMetadata);
    }

    public boolean isInlineOrSubshell() {
        return typeMetadata.equals(TypeMetadata.SUBSHELL) || typeMetadata.equals(TypeMetadata.INLINE);
    }

    public Translation unescapeText() {
        return new Translation(preamble, unescape(body), type, typeMetadata);
    }

    public Translation type(@Nonnull final Type typecastType) {
        return new Translation(preamble, body, typecastType, typeMetadata);
    }

    public Translation typeMetadata(@Nonnull final TypeMetadata meta) {
        return new Translation(preamble, body, type, meta);
    }

    public Translation appendPreamble(@Nonnull final String append) {
        return new Translation(preamble + append, body, type, typeMetadata);
    }

    public Translation body(@Nonnull final String nextBody) {
        return new Translation(preamble, nextBody, type, typeMetadata);
    }

    public Translation quoteBody() {
        return new Translation(preamble, "\"" + body + "\"", type, typeMetadata);
    }

    public Translation unquoteBody() {
        return new Translation(preamble, STRING_QUOTES.matcher(body).replaceAll(""), type, typeMetadata);
    }

    public Translation mergePreamble() {
        return new Translation(preamble + body, type, typeMetadata);
    }
}
