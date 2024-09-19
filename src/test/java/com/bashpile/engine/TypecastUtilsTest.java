package com.bashpile.engine;

import com.bashpile.exceptions.TypeError;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static com.bashpile.engine.strongtypes.Type.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TypecastUtilsTest {

    private final TypeError error = new TypeError("", 0);

    @Test
    @Order(10)
    public void boolToStrTypecastWorks() {
        Translation bool = new Translation("false", BOOL_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromBool(bool, STR_TYPE, error);
        assertEquals(STR_TYPE, converted.type());
    }

    @Test
    @Order(20)
    public void intToFloatTypecastWorks() {
        Translation intTranslation = new Translation("1", INT_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromInt(intTranslation, FLOAT_TYPE, 0, error);
        assertEquals(FLOAT_TYPE, converted.type());
    }

    @Test
    @Order(30)
    public void intToStrTypecastWorks() {
        Translation intTr = new Translation("1", INT_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromInt(intTr, STR_TYPE, 0, error);
        assertEquals(STR_TYPE, converted.type());
    }

    @Test
    @Order(40)
    public void floatToIntTypecastWorks() {
        Translation floatTr = new Translation("1.0", FLOAT_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromFloat(floatTr, INT_TYPE, 1, error);
        assertEquals(INT_TYPE, converted.type());
    }

    @Test
    @Order(50)
    public void floatToStrTypecastWorks() {
        Translation floatTr = new Translation("1.0", FLOAT_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromFloat(floatTr, STR_TYPE, 1, error);
        assertEquals(STR_TYPE, converted.type());
    }

    @Test
    @Order(60)
    public void strToBoolTypecastWorks() {
        Translation str = new Translation("TRUE", STR_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromStr(str, BOOL_TYPE, 0, error);
        assertEquals(BOOL_TYPE, converted.type());
    }

    @Test
    @Order(70)
    public void strToIntTypecastWorks() {
        Translation str = new Translation("1", STR_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromStr(str, INT_TYPE, 0, error);
        assertEquals(INT_TYPE, converted.type());
    }

    @Test
    @Order(71)
    public void strWithFloatToIntTypecastWorks() {
        Translation str = new Translation("1.5", STR_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromStr(str, INT_TYPE, 0, error);
        assertEquals(INT_TYPE, converted.type());
        assertEquals("1", converted.body());
    }

    @Test
    @Order(80)
    public void strToFloatTypecastWorks() {
        Translation str = new Translation("1.0", STR_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromStr(str, FLOAT_TYPE, 0, error);
        assertEquals(FLOAT_TYPE, converted.type());
    }

    @Test
    @Order(90)
    public void unknownToBoolTypecastWorks() {
        Translation unknown = new Translation("1.0", UNKNOWN_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromUnknown(unknown, BOOL_TYPE, 1, error);
        assertEquals(BOOL_TYPE, converted.type());
    }

    @Test
    @Order(90)
    public void unknownToIntTypecastWorks() {
        Translation unknown = new Translation("1", UNKNOWN_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromUnknown(unknown, INT_TYPE, 1, error);
        assertEquals(INT_TYPE, converted.type());
    }

    @Test
    @Order(90)
    public void unknownToFloatTypecastWorks() {
        Translation unknown = new Translation("1.0", UNKNOWN_TYPE, List.of());
        Translation converted = TypecastUtils.typecastFromUnknown(unknown, FLOAT_TYPE, 1, error);
        assertEquals(FLOAT_TYPE, converted.type());
    }

}