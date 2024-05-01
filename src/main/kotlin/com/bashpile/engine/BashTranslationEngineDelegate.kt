package com.bashpile.engine

import com.bashpile.BashpileParser
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
        if (ret.type().isPossiblyNumeric && BashTranslationHelper.inCalc(ctx)) {
            ret = ret.parenthesizeBody()
        }
        return ret
    }
}