package com.bashpile.engine;

import com.bashpile.Asserts;
import com.bashpile.engine.strongtypes.TranslationMetadata;
import com.bashpile.engine.strongtypes.Type;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bashpile.engine.strongtypes.SimpleType.LIST;
import static com.bashpile.engine.strongtypes.TranslationMetadata.NORMAL;

// TODO figure out why some expressions are type LIST but not a ListTranslation
public class ListTranslation extends Translation {

    // static section

    public static @Nonnull ListTranslation of(@Nonnull final List<Translation> listIn) {
        Asserts.assertNotEmpty(listIn);
        final ListTranslation ret = new ListTranslation(listIn.get(0).type());
        return ret.addAll(listIn);
    }

    // class section

    /** For our Bash array elements */
    private List<Translation> translations = new ArrayList<>();

    /** We convert data to body at the time of the .body() call, so this determines if we quote it then or not. */
    private boolean quoteBody = false;

    public ListTranslation(@Nonnull Type type) {
        super(
                "()",
                new Type(LIST, type.mainType().isBasic() ? type.mainType() : type.contentsType()),
                NORMAL
        );
    }

    @Override
    public @Nonnull String body() {
        String data = translations.stream().map(Translation::body).collect(Collectors.joining(" "));
        if (quoteBody) {
            data = "\"%s\"".formatted(data);
        }
        return "(%s)".formatted(data);
    }

    @Override
    public @Nonnull Translation add(@Nonnull Translation other) {
        translations.add(other);
        return this;
    }

    public @Nonnull ListTranslation addAll(@Nonnull List<Translation> other) {
        translations.addAll(other);
        return this;
    }

    @Nonnull
    @Override
    public Translation addPreamble(@Nonnull String additionalPreamble) {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation assertEmptyPreamble() {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Override
    public boolean hasPreamble() {
        return translations.stream().map(Translation::hasPreamble).reduce(true, (a, b) -> a && b);
    }

    @Nonnull
    @Override
    public Translation lambdaPreambleLines(@Nonnull Function<String, String> lambda) {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation mergePreamble() {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation body(@Nonnull String nextBody) {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation unescapeBody() {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Override
    public @Nonnull Translation quoteBody() {
        quoteBody = true;
        return this;
    }

    @Override
    public @Nonnull Translation unquoteBody() {
        quoteBody = false;
        return this;
    }

    @Override
    public @Nonnull Translation parenthesizeBody() {
        // always parenthesized
        return this;
    }

    @Nonnull
    @Override
    public Translation addOption(String additionalOption) {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation lambdaBody(@Nonnull Function<String, String> lambda) {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation lambdaBodyLines(@Nonnull Function<String, String> lambda) {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation assertParagraphBody() {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation assertNoBlankLinesInBody() {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation type(@Nonnull Type typecastType) {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation metadata(@Nonnull TranslationMetadata meta) {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation metadata(@Nonnull List<TranslationMetadata> meta) {
        throw new UnsupportedOperationException("Not supported for ListTranslations");
    }

    @Nonnull
    @Override
    public Translation inlineAsNeeded(@Nonnull Function<Translation, Translation> bodyLambda) {
        translations = translations.stream().map(it -> it.inlineAsNeeded(bodyLambda)).toList();
        return this;
    }

    @Override
    public String toString() {
        return "ListTranslation{" +
                "translations=" + translations +
                '}';
    }

    @Override
    public @Nonnull String preamble() {
        return translations.stream().map(Translation::preamble).collect(Collectors.joining("\n")).trim();
    }

    @Override
    public  @Nonnull Type type() {
        return super.type();
    }

    @Override
    public @Nonnull List<TranslationMetadata> metadata() {
        return super.metadata();
    }
}
