package com.bashpile.engine.bast;

import java.util.List;

import com.bashpile.engine.strongtypes.Type;
import org.junit.jupiter.api.Test;

import static com.bashpile.engine.bast.Translation.toStringTranslation;
import static org.junit.jupiter.api.Assertions.*;

class ListOfTranslationTest {

    @Test
    void addChildWorks() {
        Translation list = new ListOfTranslation(Type.STR_TYPE);
        list = list.addChild(toStringTranslation("Hello")).addChild(toStringTranslation("World"));
        assertEquals("(Hello World)", list.toString());
        assertEquals("(Hello World)", list.render());
    }

    @Test
    void addChildAllWorks() {
        ListOfTranslation list = new ListOfTranslation(Type.STR_TYPE);
        list = list.addAll(List.of(toStringTranslation("Hello"), toStringTranslation("World")));
        assertEquals("(Hello World)", list.toString());
        assertEquals("(Hello World)", list.render());
    }
}