package com.bashpile.engine.bast;

import java.util.stream.Stream;

import com.bashpile.engine.strongtypes.TranslationMetadata;
import org.junit.jupiter.api.Test;

import static com.bashpile.engine.bast.Translation.toStringTranslation;
import static org.junit.jupiter.api.Assertions.*;

class TranslationTest {

    @Test
    public void addWorks() {
        final Translation t1 = toStringTranslation("Hello ");
        final Translation t2 = toStringTranslation("World");
        assertEquals("Hello World", t1.add(t2).toString());
        assertEquals("Hello World", t1.add(t2).getData());
    }

    @Test
    public void addOptionWithMetadataWorks() {
        final Translation option1 = toStringTranslation("-r").metadata(TranslationMetadata.OPTION);
        final Translation option2 = toStringTranslation("-x").metadata(TranslationMetadata.OPTION);
        assertEquals("-rx", option1.add(option2).toString());
        assertEquals("-rxx", option1.add(option2).getData());
    }

    @Test
    public void lambdaBodyPreservesChildren() {
        Translation t1 = new Translation("printf \"aoeu\" >/dev/null\n");
        t1 = (Translation) t1.addAll(Stream.of(new Translation("ls\n")));
        Translation t2 = t1.lambdaBody(body -> body.replace(">/dev/null", ""));

        assertFalse(t1.getChildren().isEmpty());
        assertFalse(t2.getChildren().isEmpty());
        assertEquals(t1.getChildren().size(), t2.getChildren().size());
    }

    @Test
    public void lambdaBodyAppliesToChildren() {
        Translation t1 = new Translation("printf \"aoeu\" >/dev/null\n");
        t1 = (Translation) t1.addAll(Stream.of(new Translation("ls\n")));
        Translation t2 = t1.lambdaBody(body -> body.replace("ls", ""));

        assertFalse(t1.getChildren().isEmpty());
        assertFalse(t2.getChildren().isEmpty());
        assertEquals(t1.getChildren().size(), t2.getChildren().size());
        assertFalse(t2.getChildren().getFirst().getData().contains("ls"));
    }

    @Test
    public void lambdaBodyLinesPreservesChildren() {
        Translation t1 = new Translation("printf \"aoeu\" >/dev/null\n");
        t1 = (Translation) t1.addAll(Stream.of(new Translation("ls\n")));
        Translation t2 = t1.lambdaBodyLines(body -> body.replace("ls", ""));

        assertFalse(t1.getChildren().isEmpty());
        assertFalse(t2.getChildren().isEmpty());
        assertEquals(t1.getChildren().size(), t2.getChildren().size());
        assertFalse(t2.getChildren().getFirst().getData().contains("ls"));
    }
}