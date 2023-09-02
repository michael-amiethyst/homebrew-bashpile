package com.bashpile.engine;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Order(3)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BashTranslationHelperTest {

    @Test @Order(10)
    public void unwindAllWithFunctionCallWorks() {
        Translation tr = new Translation("$(ret0)");
        tr = BashTranslationHelper.unwindAll(tr);
        assertFalse(tr.body().contains("$("), "Unwound command substitution found");
    }
}