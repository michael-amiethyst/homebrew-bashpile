package com.bashpile.engine;

import com.bashpile.engine.strongtypes.SimpleType;
import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.TypeError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BashTranslationHelperTest {

    private final TypeError error = new TypeError("", 0);

    @Test
    public void boolToStrTypecastWorks() {
        Translation bool = new Translation("false", Type.BOOL_TYPE, List.of());
        Translation converted = BashTranslationHelper.typecastFromBool(SimpleType.STR, bool, error);
        assertEquals(Type.STR_TYPE, converted.type());
    }

}