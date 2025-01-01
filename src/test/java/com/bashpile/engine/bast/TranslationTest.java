package com.bashpile.engine.bast;

import com.bashpile.engine.strongtypes.TranslationMetadata;
import org.junit.jupiter.api.Test;

import static com.bashpile.engine.bast.Translation.toStringTranslation;
import static org.junit.jupiter.api.Assertions.*;

class TranslationTest {

    @Test
    void addWorks() {
        final Translation t1 = toStringTranslation("Hello ");
        final Translation t2 = toStringTranslation("World");
        assertEquals("Hello World", t1.add(t2).toString());
        assertEquals("Hello World", t1.add(t2).getData());
    }

    @Test
    void addOptionWorks() {
        final Translation noOption = toStringTranslation("Hello");
        final Translation option = toStringTranslation("-x");
        assertEquals("-iHello", noOption.addOption("i").toString());
        assertEquals("-ix", option.addOption("i").getData());
    }

    @Test
    void addOptionWithMetadataWorks() {
        final Translation option1 = toStringTranslation("-r").metadata(TranslationMetadata.OPTION);
        final Translation option2 = toStringTranslation("-x").metadata(TranslationMetadata.OPTION);
        assertEquals("-rx", option1.add(option2).toString());
        assertEquals("-rxx", option1.add(option2).getData());
    }
}