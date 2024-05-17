package com.bashpile.engine;

import com.bashpile.engine.strongtypes.SimpleType;
import com.bashpile.engine.strongtypes.Type;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static com.bashpile.engine.strongtypes.TranslationMetadata.NEEDS_INLINING_OFTEN;
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
                new Translation("(which ls 1>/dev/null)", Type.of(SimpleType.BOOL), NEEDS_INLINING_OFTEN);
        tr = tr.inlineAsNeeded(BashTranslationHelper::unwindAll);
        assertFalse(tr.body().contains("})"), "Bad parenthesis found");
    }

    @Test @Order(30)
    public void unwindAllWithDoubleInlineWorks() {
        Translation tr =
                new Translation("""
                        $(bc <<< "$(circleArea "${r1}") + $(circleArea "${r2}")")
                        """, Type.FLOAT_TYPE, NEEDS_INLINING_OFTEN);
        tr = tr.inlineAsNeeded(BashTranslationHelper::unwindNested);
        assertFalse(tr.body().contains("})"), "Bad parenthesis found");
        assertFalse(tr.preamble().isEmpty());
        assertFalse(tr.preamble().contains("$( $(bc <<< \"$(circleArea \"${r1}\")\n"), "Mismatched parens");
        assertFalse(tr.preamble().contains("$(bc <<< \"$(circleArea \"${r1}\")\n"), "Mismatched inner parens");

    }
}