package com.bashpile.engine;

import com.bashpile.engine.strongtypes.TranslationMetadata;
import com.bashpile.engine.strongtypes.Type;
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

    @Test @Order(20)
    public void unwindAllWithParenthesisWorks() {
        Translation tr =
                new Translation("(which ls 1>/dev/null)", Type.BOOL, TranslationMetadata.NEEDS_INLINING_OFTEN);
        tr = tr.inlineAsNeeded(BashTranslationHelper::unwindAll);
        assertFalse(tr.body().contains("})"), "Bad parenthesis found");
    }
}