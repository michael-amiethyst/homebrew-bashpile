package com.bashpile;

import org.junit.jupiter.api.*;

import static com.bashpile.Strings.inParentheses;
import static com.bashpile.Strings.unquote;
import static org.junit.jupiter.api.Assertions.*;

@Order(2)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StringsTest {

    @Test @Order(10)
    public void removeEndGroupsWorks() {
        final String test = "\"test\"";
        assertEquals("test", unquote(test));

        final String test2 = "test2";
        assertEquals(test2, unquote(test2));
    }

    @Test @Order(20)
    public void inParenthesisWorks() {
        assertTrue(inParentheses("(some text or numbers like 23454536)"));
        assertTrue(inParentheses("((some nested text)or(numbers like 23454536))"));
    }

    @Test @Order(30)
    public void inParenthesisCanWorkUnhappy() {
        assertFalse(inParentheses("(some nested text)or(numbers like 23454536)"));
    }

}