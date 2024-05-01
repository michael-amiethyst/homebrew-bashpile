package com.bashpile.engine

import com.bashpile.Asserts
import com.bashpile.BashpileParser
import com.bashpile.BashpileParser.CalculationExpressionContext
import com.bashpile.Strings
import com.bashpile.engine.strongtypes.TranslationMetadata.*
import com.bashpile.engine.strongtypes.Type
import com.bashpile.exceptions.BashpileUncheckedException
import com.bashpile.exceptions.TypeError
import com.bashpile.exceptions.UserError
import com.google.common.collect.Iterables
import org.antlr.v4.runtime.tree.ParseTree
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.stream.Collectors
import java.util.stream.Stream

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
            ret.metadata(ret.metadata() + PARENTHESIZED)
        }
        return ret
    }

    fun calculationExpression(ctx: CalculationExpressionContext): Translation {
        LOG.trace("In calculationExpression")
        // get the child translations
        val childTranslations: List<Translation> = ctx.children
            .map { tree: ParseTree? -> visitor.visit(tree) }
            .map { tr: Translation ->
                tr.inlineAsNeeded { tr1: Translation? -> BashTranslationHelper.unwindNested(tr1!!) }
            }
            .toList()

        // child translations in the format of 'expr operator expr', so we are only interested in the first and last
        val first = childTranslations[0]
        val second = Iterables.getLast(childTranslations)
        // types section
        return if (Translation.areNumericExpressions(first, second) && BashTranslationHelper.inCalc(ctx)) {
            Translation.toTranslation(childTranslations.stream(), Type.NUMBER_TYPE, NORMAL)
        } else if (Translation.areNumericExpressions(first, second)) {
            val translationsString = childTranslations.stream()
                .map { obj: Translation -> obj.body() }.collect(Collectors.joining(" "))
            Translation.toTranslation(childTranslations.stream(), Type.NUMBER_TYPE, NEEDS_INLINING_OFTEN)
                .body("bc <<< \"$translationsString\"")
        } else if (Translation.areStringExpressions(first, second)) {
            val op = ctx.op.text
            Asserts.assertEquals("+", op, "Only addition is allowed on Strings, but got $op")
            Translation.toTranslation(Stream.of(first, second)
                    .map { it.unquoteBody() }
                    .map { it.lambdaBody { str: String? -> Strings.unparenthesize(str!!) } })
        } else if (first.isNotFound || second.isNotFound) {
            // found no matching types
            val message = "`${first.body()}` or `${second.body()}` are undefined"
            throw UserError(message, BashTranslationHelper.lineNumber(ctx))
        } else {
            // throw type error for all others
            val message = "Incompatible types in calc: ${first.type()} and ${second.type()}"
            throw TypeError(message, BashTranslationHelper.lineNumber(ctx))
        }
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
            if (ret.metadata().contains(PARENTHESIZED)) {
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
            .metadata(CONDITIONAL)
    }
}