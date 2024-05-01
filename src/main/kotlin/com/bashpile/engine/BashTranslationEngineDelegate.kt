package com.bashpile.engine

import com.bashpile.Asserts
import com.bashpile.BashpileParser
import com.bashpile.Strings
import com.bashpile.engine.BashTranslationHelper.createCommentTranslation
import com.bashpile.engine.strongtypes.FunctionTypeInfo
import com.bashpile.engine.strongtypes.SimpleType
import com.bashpile.engine.strongtypes.TranslationMetadata.*
import com.bashpile.engine.strongtypes.Type
import com.bashpile.engine.strongtypes.TypeStack
import com.bashpile.exceptions.BashpileUncheckedException
import com.bashpile.exceptions.TypeError
import com.bashpile.exceptions.UserError
import com.google.common.collect.Iterables
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import java.util.stream.Stream

/** For the BashTranslationEngine to have complex code be in Kotlin */
class BashTranslationEngineDelegate(private val visitor: BashpileVisitor) {

    companion object {
        private val LOG: Logger = LogManager.getLogger(BashTranslationEngineDelegate::class)
    }

    fun functionDeclarationStatement(
        ctx: BashpileParser.FunctionDeclarationStatementContext,
        foundForwardDeclarations: Set<String>,
        typeStack: TypeStack
    ): Translation {
        LOG.trace("In functionDeclarationStatement")
        // avoid translating twice if was part of a forward declaration
        val functionName = ctx.Id().text
        if (foundForwardDeclarations.contains(functionName)) {
            return Translation.UNKNOWN_TRANSLATION
        }

        // check for double declaration
        if (typeStack.containsFunction(functionName)) {
            val message = "$functionName was declared twice (function overloading is not supported)"
            throw UserError(message, BashTranslationHelper.lineNumber(ctx))
        }

        // regular processing -- no forward declaration

        // register function param types and return type
        val typeList = ctx.paramaters().typedId()
            .map { SimpleType.valueOf(it!!) }
            .map { Type.of(it) }
            .toList()
        val retType = if (ctx.type() != null) { Type.valueOf(ctx.type()) } else Type.NA_TYPE
        typeStack.putFunctionTypes(functionName, FunctionTypeInfo(typeList, retType))

        // Create final translation and variables
        return typeStack.pushFrame().use { _ ->

            // register local variable types
            ctx.paramaters().typedId().forEach {
                val mainType = Type.valueOf(it.type())
                typeStack.putVariableType(it.Id().text, mainType, BashTranslationHelper.lineNumber(ctx))
            }

            // create Translations
            val comment = createCommentTranslation("function declaration", BashTranslationHelper.lineNumber(ctx))
            val i = AtomicInteger(1)
            // the empty string or ...
            var namedParams = ""
            if (ctx.paramaters().typedId().isNotEmpty()) {
                // local var1=$1; local var2=$2; etc
                val paramDeclarations = ctx.paramaters().typedId()
                    .map{ it.Id() }
                    .map { obj: TerminalNode -> obj.text }
                    .map { x: String ->
                        val type: Type = typeStack.getVariableType(x)

                        // special handling for lists with 'read -a'
                        if (type.isList) {
                            return@map "declare -x IFS=$' ';" +
                                    " read -r -a $x <<< \"$${i.getAndIncrement()}\"; declare -x IFS=$'\\n\\t';"
                        }

                        // normal processing
                        var opts = "-r" // read only
                        if (type.isInt) {
                            opts += "i" // Bash integer
                        }
                        "declare $opts $x=$${i.getAndIncrement()};"
                    }.joinToString(" ")
                namedParams = BashTranslationEngine.TAB + paramDeclarations + "\n"
            }
            val blockStatements = BashTranslationHelper.streamContexts(
                ctx.functionBlock().statement(), ctx.functionBlock().returnPsudoStatement()
            )
                .map { visitor.visit(it) }
                .map { tr: Translation -> tr.lambdaBodyLines { BashTranslationEngine.TAB + it } }
                .reduce { obj: Translation, other: Translation? -> obj.add(other!!) }
                .orElseThrow()
                .assertEmptyPreamble()
            Asserts.assertIsLine(namedParams)
            Asserts.assertIsParagraph(blockStatements.body())
            val functionDeclaration = Translation.toParagraphTranslation(
                "$functionName () {\n" +
                        "$namedParams${blockStatements.body()}\n"
            )
            comment.add(functionDeclaration)
        }
    }

    fun parenthesisExpression(ctx: BashpileParser.ParenthesisExpressionContext): Translation {
        LOG.trace("In parenthesisExpression")
        // drop parenthesis
        val ret: Translation = visitor.visit(ctx.expression())

        // only add parenthesis back in for necessary operations (e.g. "(((5)))" becomes "5" outside of a calc)
        return ret.metadata(ret.metadata() + PARENTHESIZED)
    }

    fun calculationExpression(ctx: BashpileParser.CalculationExpressionContext): Translation {
        LOG.trace("In calculationExpression")
        // get the child translations
        var childTranslations: List<Translation> = ctx.children
            .map { tree: ParseTree? -> visitor.visit(tree) }
            .map { tr: Translation ->
                tr.inlineAsNeeded { tr1: Translation? -> BashTranslationHelper.unwindNested(tr1!!) }
            }
            .toList()

        // child translations in the format of 'expr operator expr', so we are only interested in the first and last
        val first = childTranslations[0]
        val second = Iterables.getLast(childTranslations)

        return if (Translation.areNumericExpressions(first, second)) {
            // Numbers -- We need the Basic Calculator to process
            childTranslations = childTranslations.map {
                if (it.metadata().contains(CALCULATION)) { unwrapCalculation(it) } else it
            }.map {
                if (it.metadata().contains(PARENTHESIZED)) {
                    it.metadata(it.metadata() - PARENTHESIZED).parenthesizeBody()
                } else it
            }
            // first happy path executed, assume no nesting
            val translationsString = childTranslations.stream()
                .map { obj: Translation -> obj.body() }.collect(Collectors.joining(" "))
            Translation(translationsString, Type.NUMBER_TYPE, listOf(NEEDS_INLINING_OFTEN, CALCULATION))
                .body("bc <<< \"$translationsString\"")
        } else if (Translation.areStringExpressions(first, second)) {
            // Strings -- only addition supported
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

    private fun unwrapCalculation(translation: Translation): Translation {
        // in nested calc, first or second may be wrapped in inlines "$()" or parenthesis
        var ret = translation.lambdaBody { body2 -> body2
            .removePrefix("$(")
            .removePrefix(" ")
            .removeSuffix(")")
        }
        var parens = false
        if (ret.body().startsWith("(") && ret.body().endsWith(")")) {
            parens = true
            ret = ret.lambdaBody { body -> body.removePrefix("(").removeSuffix(")") }
        }
        ret = ret.lambdaBody { body -> body.removePrefix("bc <<< \"").removeSuffix("\"") }
        if (parens) ret = ret.parenthesizeBody()
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