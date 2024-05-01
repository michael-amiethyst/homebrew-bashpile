package com.bashpile.engine

import com.bashpile.BashpileParser
import com.bashpile.engine.strongtypes.TranslationMetadata
import com.bashpile.exceptions.BashpileUncheckedException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/** For the BashTranslationEngine to have complex code be in Kotlin */
class BashTranslationEngineDelegate(private val visitor: BashpileVisitor) {

    companion object {
        private val LOG: Logger = LogManager.getLogger(BashTranslationEngineDelegate::class)
    }

    fun parenthesisExpression(ctx: BashpileParser.ParenthesisExpressionContext): Translation {
        LOG.trace("In parenthesisExpression")
        // drop parenthesis
        var ret: Translation = visitor.visit(ctx.expression())

        // only keep parenthesis for necessary operations (e.g. "(((5)))" becomes "5" outside of a calc)
        ret = if (ret.type().isPossiblyNumeric && BashTranslationHelper.inCalc(ctx)) {
            ret.parenthesizeBody()
        } else {
            ret.metadata(ret.metadata() + TranslationMetadata.PARENTHESIZED)
        }
        return ret
    }

    fun combiningExpression(ctx: BashpileParser.CombiningExpressionContext): Translation {
        LOG.trace("In combiningExpression")
        val operator = when (ctx.combiningOperator().text) {
            "and" -> "&&"
            "or" -> "||"
            else -> throw BashpileUncheckedException("Unexpected combiningExpression: ${ctx.combiningOperator().text}")
        }
        var translations = listOf(visitor.visit(ctx.getChild(0)), visitor.visit(ctx.getChild(2)))
        translations = translations.map {
            var ret = it.inlineAsNeeded { tr: Translation? -> BashTranslationHelper.unwindNested(tr!!) }
            if (ret.metadata().contains(TranslationMetadata.PARENTHESIZED)) {
                // wrap in a block and add an end-of-statement
                ret = ret.body("{ ${ret.body()}; }")
            }
            ret
        }

        val body = "${translations[0].unquoteBody().body()} $operator ${translations[1].unquoteBody().body()}"
        return Translation
            .toStringTranslation(body)
            .addPreamble(translations[0].preamble())
            .addPreamble(translations[1].preamble())
            .metadata(TranslationMetadata.CONDITIONAL)
    }
}