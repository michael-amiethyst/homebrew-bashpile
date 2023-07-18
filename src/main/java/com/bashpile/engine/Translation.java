package com.bashpile.engine;

import com.bashpile.engine.strongtypes.TypeMetadata;
import com.bashpile.engine.strongtypes.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;

/**
 * Decorator pattern for a String.
 *
 * @param text The target shell script (e.g. Bash) literal text.
 * @param type The Bashpile type.
 * @param typeMetadata Further information of the type (e.g. is this a subshell?)
 * @param preamble This is text that needs to be emitted before the rest of the translation.<br>
 *                          This is to handle the case of a nested command substitution, since they are not supported
 *                          in Bash we need to assign the inner command substitution to a variable (while handling bad
 *                          exit codes, another work-around) and have the <code>text</code> just be the variable.
 */
public record Translation(
        @Nonnull String text,
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
        return new Translation(joined.text, type, typeMetadata, joined.preamble);
    }

    public static Translation toStringTranslation(final String... text) {
        return new Translation(join(text), Type.STR, TypeMetadata.NORMAL);
    }

    public Translation add(@Nonnull final String appendText) {
        return new Translation(text + appendText, this.type, typeMetadata, preamble);
    }

    public Translation add(@Nonnull final Translation other) {
        return new Translation(text + other.text, type, typeMetadata, preamble + other.preamble);
    }

    public boolean isNotSubshell() {
        return !typeMetadata.equals(TypeMetadata.SUBSHELL) && !typeMetadata.equals(TypeMetadata.COMMAND_SUBSTITUTION);
    }

    public Translation unescapeText() {
        return new Translation(StringEscapeUtils.unescapeJava(text), type, typeMetadata, preamble);
    }
}
