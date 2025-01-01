package com.bashpile.engine.bast;

import java.util.List;

import com.bashpile.engine.strongtypes.Type;
import org.junit.jupiter.api.Test;

import static com.bashpile.engine.bast.Translation.toStringTranslation;
import static org.junit.jupiter.api.Assertions.*;

class ListOfTranslationTest {

    @Test
    void addWorks() {
        Translation list = new ListOfTranslation(Type.STR_TYPE);
        list = list.add(toStringTranslation("Hello")).add(toStringTranslation("World"));
        assertEquals("(Hello World)", list.toString());
        assertEquals("(Hello World)", list.getData());
    }

    @Test
    void addAllWorks() {
        ListOfTranslation list = new ListOfTranslation(Type.STR_TYPE);
        list = list.addAll(List.of(toStringTranslation("Hello"), toStringTranslation("World")));
        assertEquals("(Hello World)", list.toString());
        assertEquals("(Hello World)", list.getData());
    }
}