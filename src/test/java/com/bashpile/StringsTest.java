package com.bashpile;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static com.bashpile.Strings.unquote;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}