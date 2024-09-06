package com.bashpile.engine;

import com.bashpile.exceptions.TypeError;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static com.bashpile.engine.strongtypes.Type.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BashTranslationHelperTest {

    private final TypeError error = new TypeError("", 0);

    @Test
    @Order(10)
    public void boolToStrTypecastWorks() {
        Translation bool = new Translation("false", BOOL_TYPE, List.of());
        Translation converted = BashTranslationHelper.typecastFromBool(bool, STR_TYPE, error);
        assertEquals(STR_TYPE, converted.type());
    }

    @Test
    @Order(20)
    public void intToFloatTypecastWorks() {
        Translation intTranslation = new Translation("1", INT_TYPE, List.of());
        Translation converted = BashTranslationHelper.typecastFromInt(intTranslation, FLOAT_TYPE, 0, error);
        assertEquals(FLOAT_TYPE, converted.type());
    }

    @Test
    @Order(30)
    public void intToStrTypecastWorks() {
        Translation str = new Translation("1", INT_TYPE, List.of());
        Translation converted = BashTranslationHelper.typecastFromInt(str, STR_TYPE, 0, error);
        assertEquals(STR_TYPE, converted.type());
    }

    @Test
    @Order(40)
    public void floatToIntTypecastWorks() {
        Translation str = new Translation("1.0", FLOAT_TYPE, List.of());
        Translation converted = BashTranslationHelper.typecastFromFloat(str, INT_TYPE, error);
        assertEquals(INT_TYPE, converted.type());
    }

    @Test
    @Order(50)
    public void floatToStrTypecastWorks() {
        Translation str = new Translation("1.0", FLOAT_TYPE, List.of());
        Translation converted = BashTranslationHelper.typecastFromFloat(str, STR_TYPE, error);
        assertEquals(STR_TYPE, converted.type());
    }

}