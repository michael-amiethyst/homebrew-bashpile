package com.bashpile.engine.bast;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import com.bashpile.engine.strongtypes.TranslationMetadata;
import com.bashpile.engine.strongtypes.Type;

import static com.bashpile.Asserts.assertNotEmpty;
import static com.bashpile.engine.strongtypes.TranslationMetadata.NORMAL;

/** Subclass for listOf(...) where we have the translations of the list elements */
public class ListOfTranslation extends Translation {

    // static section

    public static @Nonnull ListOfTranslation of(@Nonnull final List<Translation> listIn) {
        final ListOfTranslation ret = new ListOfTranslation(assertNotEmpty(listIn).get(0).type());
        return ret.addAll(listIn);
    }

    // class section

    /** For our Bash array elements */
    private List<Translation> translations = new ArrayList<>();

    /** We convert data to body at the time of the .body() call, so this determines if we quote it then or not. */
    private boolean quoteBody = false;

    public ListOfTranslation(@Nonnull Type type) {
        super(
                "()",
                new Type(Type.TypeNames.LIST, Optional.of(type)),
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
    public @Nonnull Translation add(@Nonnull final TreeNode<String> other) {
        translations.add((Translation) other);
        return this;
    }

    public @Nonnull ListOfTranslation addAll(@Nonnull List<Translation> other) {
        translations.addAll(other);
        return this;
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
    public Translation inlineAsNeeded() {
        translations = translations.stream().map(Translation::inlineAsNeeded).toList();
        return this;
    }

    @Override
    public String toString() {
        return body();
    }

    @Override
    public String getData() {
        return body();
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
