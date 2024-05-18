package com.bashpile.engine

import com.bashpile.engine.Unwinder.Companion.unwindAll
import com.bashpile.engine.Unwinder.Companion.unwindNested
import com.bashpile.engine.strongtypes.TranslationMetadata.NEEDS_INLINING_OFTEN
import com.bashpile.engine.strongtypes.Type
import org.junit.jupiter.api.*

@Order(3)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UnwinderTest {
    @Test
    @Order(10)
    fun unwindAllWithFunctionCallWorks() {
        var tr = Translation("$(ret0)")
        tr = unwindAll(tr)
        Assertions.assertFalse(tr.body().contains("$("), "Unwound command substitution found")
    }

    @Test
    @Order(20)
    fun unwindAllWithParenthesisWorks() {
        var tr = Translation("(which ls 1>/dev/null)", Type.BOOL_TYPE, NEEDS_INLINING_OFTEN)
        tr = tr.inlineAsNeeded { unwindAll(it) }
        Assertions.assertFalse(tr.body().contains("})"), "Bad parenthesis found")
    }

    @Test
    @Order(30)
    fun unwindAllWithDoubleInlineWorks() {
        var tr = Translation("""
                ${'$'}(bc <<< "${'$'}(circleArea "${'$'}{r1}") + ${'$'}(circleArea "${'$'}{r2}")")
                
                """.trimIndent(), Type.FLOAT_TYPE, NEEDS_INLINING_OFTEN
        )
        tr = tr.inlineAsNeeded { unwindNested(it) }
        Assertions.assertFalse(tr.body().contains("})"), "Bad parenthesis found")
        Assertions.assertFalse(tr.preamble().isEmpty())
        Assertions.assertFalse(
            tr.preamble().contains("$( $(bc <<< \"$(circleArea \"\${r1}\")\n"),
            "Mismatched outer parens"
        )
        Assertions.assertFalse(
            tr.preamble().contains("$(bc <<< \"$(circleArea \"\${r1}\")\n"),
            "Mismatched inner parens"
        )
    }
}