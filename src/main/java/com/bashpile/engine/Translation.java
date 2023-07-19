package com.bashpile.engine;

import com.bashpile.engine.strongtypes.Type;
import com.bashpile.engine.strongtypes.TypeMetadata;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.join;

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
        @Nonnull String body,
        @Nonnull Type type,
        @Nonnull TypeMetadata typeMetadata,
        @Nonnull String preamble) {

    public static final Translation EMPTY_TYPE = new Translation("", Type.EMPTY, TypeMetadata.NORMAL);
    public static final Translation EMPTY_STRING = toStringTranslation("");

    public Translation(@Nonnull final String text, @Nonnull final Type type, @Nonnull final TypeMetadata typeMetadata) {
        this(text, type, typeMetadata, "");
    }

    public static Translation toTranslation(
            @Nonnull final Stream<Translation> stream,
            @Nonnull final Type type,
            @Nonnull final TypeMetadata typeMetadata) {
        final Translation joined = stream.reduce(Translation::add).orElseThrow();
        return new Translation(joined.body, type, typeMetadata, joined.preamble);
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
        return Stream.of(translations).filter(x -> !x.type().isNumeric()).findAny().isEmpty();
    }

    public Translation add(@Nonnull final String appendText) {
        return new Translation(body + appendText, type, typeMetadata, preamble);
    }

    public Translation add(@Nonnull final Translation other) {
        return new Translation(body + other.body, type, typeMetadata, preamble + other.preamble);
    }

    public boolean isNotSubshell() {
        return !typeMetadata.equals(TypeMetadata.SUBSHELL) && !typeMetadata.equals(TypeMetadata.INLINE);
    }

    public Translation unescapeText() {
        return new Translation(StringEscapeUtils.unescapeJava(body), type, typeMetadata, preamble);
    }

    public Translation type(@Nonnull final Type typecastType) {
        return new Translation(body, typecastType, typeMetadata, preamble);
    }

    public Translation typeMetadata(@Nonnull final TypeMetadata meta) {
        return new Translation(body, type, meta, preamble);
    }

    public Translation appendPreamble(@Nonnull final String append) {
        return new Translation(body, type, typeMetadata, preamble + append);
    }

    public Translation body(@Nonnull final String nextBody) {
        return new Translation(nextBody, type, typeMetadata, preamble);
    }

    public Translation mergePreamble() {
        return new Translation(preamble + body, type, typeMetadata);
    }
}
