package com.bashpile.engine;

import com.bashpile.exceptions.TypeError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.bashpile.engine.strongtypes.Type.*;
import static org.junit.jupiter.api.Assertions.*;

class BashTranslationHelperTest {

    private final TypeError error = new TypeError("", 0);

    @Test
    public void boolToStrTypecastWorks() {
        Translation bool = new Translation("false", BOOL_TYPE, List.of());
        Translation converted = BashTranslationHelper.typecastFromBool(bool, STR_TYPE, error);
        assertEquals(STR_TYPE, converted.type());
    }

    @Test
    public void intToFloatTypecastWorks() {
        Translation intTranslation = new Translation("1", INT_TYPE, List.of());
        Translation converted = BashTranslationHelper.typecastFromInt(intTranslation, FLOAT_TYPE, 0, error);
        assertEquals(FLOAT_TYPE, converted.type());
    }

    @Test
    public void intToStrTypecastWorks() {
        Translation str = new Translation("1", INT_TYPE, List.of());
        Translation converted = BashTranslationHelper.typecastFromInt(str, STR_TYPE, 0, error);
        assertEquals(STR_TYPE, converted.type());
    }

}