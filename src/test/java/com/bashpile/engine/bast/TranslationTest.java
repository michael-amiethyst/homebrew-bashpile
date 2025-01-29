package com.bashpile.engine.bast;

import java.util.stream.Stream;

import com.bashpile.Strings;
import com.bashpile.engine.strongtypes.TranslationMetadata;
import com.bashpile.engine.strongtypes.Type;
import org.junit.jupiter.api.Test;

import static com.bashpile.engine.bast.Translation.toStringTranslation;
import static com.bashpile.engine.strongtypes.TranslationMetadata.NEEDS_INLINING;
import static org.junit.jupiter.api.Assertions.*;

class TranslationTest {

    @Test
    public void addChildWorks() {
        final Translation t1 = toStringTranslation("Hello ");
        final Translation t2 = toStringTranslation("World");
        assertEquals("Hello World", t1.addChild(t2).toString());
        assertEquals("Hello World", t1.addChild(t2).render());
    }

    @Test
    public void addChildOptionWithReplaceMetadataWorks() {
        final Translation option1 = toStringTranslation("-r").replaceMetadata(TranslationMetadata.OPTION);
        final Translation option2 = toStringTranslation("-x").replaceMetadata(TranslationMetadata.OPTION);
        assertEquals("-rx", option1.addChild(option2).toString());
        assertEquals("-rx", option1.addChild(option2).render());
    }

    @Test
    public void lambdaBodyPreservesChildren() {
        Translation t1 = new Translation("printf \"aoeu\" >/dev/null\n");
        t1 = (Translation) t1.addAllChildren(Stream.of(new Translation("ls\n")));
        Translation t2 = t1.lambdaBody(body -> body.replace(">/dev/null", ""));

        assertFalse(t1.getChildren().isEmpty());
        assertFalse(t2.getChildren().isEmpty());
        assertEquals(t1.getChildren().size(), t2.getChildren().size());
    }

    @Test
    public void lambdaBodyAppliesToChildren() {
        Translation t1 = new Translation("printf \"aoeu\" >/dev/null\n");
        t1 = (Translation) t1.addAllChildren(Stream.of(new Translation("ls\n")));
        Translation t2 = t1.lambdaBody(body -> body.replace("ls", ""));

        assertFalse(t1.getChildren().isEmpty());
        assertFalse(t2.getChildren().isEmpty());
        assertEquals(t1.getChildren().size(), t2.getChildren().size());
        assertFalse(t2.getChildren().getFirst().render().contains("ls"));
    }

    @Test
    public void lambdaBodyLinesPreservesChildren() {
        Translation t1 = new Translation("printf \"aoeu\" >/dev/null\n");
        t1 = (Translation) t1.addAllChildren(Stream.of(new Translation("ls\n")));
        Translation t2 = t1.lambdaBodyLines(body -> body.replace("ls", ""));

        assertFalse(t1.getChildren().isEmpty());
        assertFalse(t2.getChildren().isEmpty());
        assertEquals(t1.getChildren().size(), t2.getChildren().size());
        assertFalse(t2.getChildren().getFirst().render().contains("ls"));
    }

    @Test
    public void renderCanBeCalledRepeatedly() {
        final Translation tr = new Translation("ls", Type.STR_TYPE, NEEDS_INLINING);
        // should have a single $ at start of string
        String[] parts = tr.render().split("\\$");
        assertEquals(2, parts.length);
        assertTrue(Strings.isBlank(parts[0]));
        // even if called twice
        parts = tr.render().split("\\$");
        assertEquals(2, parts.length);
        assertTrue(Strings.isBlank(parts[0]));
    }

    @Test
    public void lambdaBodyCanRerender() {
        Translation tr = new Translation("ls", Type.STR_TYPE, NEEDS_INLINING);
        tr = tr.lambdaBody("%s > /dev/null"::formatted);
        assertNotEquals(tr.render(), tr.body());
        assertEquals("$( ls > /dev/null )", tr.render());
        tr = tr.addMetadata(TranslationMetadata.QUOTE);
        assertEquals("\"$( ls > /dev/null )\"", tr.render());
        tr = tr.removeMetadata(NEEDS_INLINING);
        assertEquals("\"ls > /dev/null\"", tr.render());
    }

    // TODO feature/bast write test to ensure that lambdaBody only changes the render and not the body field
    // TODO feature/bast write test to ensure that append only changes the render and not the body field.  Why is append needed?  addChild should have the same result
}