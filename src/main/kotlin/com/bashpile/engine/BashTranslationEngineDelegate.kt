package com.bashpile.engine

import com.bashpile.Asserts
import com.bashpile.BashpileParser
import com.bashpile.BashpileParser.UnaryPrimaryExpressionContext
import com.bashpile.Strings
import com.bashpile.engine.BashTranslationHelper.*
import com.bashpile.engine.Translation.toStringTranslation
import com.bashpile.engine.Unwinder.Companion.unwindNested
import com.bashpile.engine.strongtypes.FunctionTypeInfo
import com.bashpile.engine.strongtypes.TranslationMetadata.*
import com.bashpile.engine.strongtypes.Type
import com.bashpile.engine.strongtypes.TypeStack
import com.bashpile.exceptions.BashpileUncheckedException
import com.bashpile.exceptions.TypeError
import com.bashpile.exceptions.UserError
import com.google.common.collect.Iterables
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.Objects.requireNonNull
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream


/** For the BashTranslationEngine to have complex code be in Kotlin */
class BashTranslationEngineDelegate(private val visitor: BashpileVisitor) {

    companion object {

        private val unaryPrimaryTranslations = mapOf(
            Pair("not", "!"),
            Pair("unset", "-z"),
            Pair("isset", "-n"),
            Pair("isEmpty", "-z"),
            Pair("isNotEmpty", "-n"),
            Pair("fileExists", "-e"),
            Pair("regularFileExists", "-f"),
            Pair("directoryExists", "-d")
        )

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
            throw UserError(message, lineNumber(ctx))
        }

        // regular processing -- no forward declaration

        // register function param types and return type
        val typeList = ctx.paramaters().typedId()
            .map { Type.valueOf(it.type()!!) }
            .toList()
        val retType = if (ctx.type() != null) { Type.valueOf(ctx.type()) } else Type.NA_TYPE
        typeStack.putFunctionTypes(functionName, FunctionTypeInfo(typeList, retType))

        // Create final translation and variables
        return typeStack.pushFrame().use { _ ->

            // register local variable types
            ctx.paramaters().typedId().forEach {
                val mainType = Type.valueOf(it.type())
                typeStack.putVariableType(it.Id().text, mainType, lineNumber(ctx))
            }

            // create Translations
            val comment = createCommentTranslation("function declaration", lineNumber(ctx))
            val i = AtomicInteger(1)
            // the empty string or ...
            var namedParams = if (ctx.paramaters().typedId().isNotEmpty()) {
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
                BashTranslationEngine.TAB + paramDeclarations + "\n"
            } else {
                BashTranslationEngine.TAB + "# no parameters to function" + "\n"
            }
            val blockStatements = streamContexts(
                ctx.functionBlock().statement(), ctx.functionBlock().returnPsudoStatement()
            )
                .map { visitor.visit(it) }
                .map { tr: Translation -> tr.lambdaBodyLines { BashTranslationEngine.TAB + it }
                    .lambdaBody { it.replace("exit 1", "return 1") }
                }.reduce { obj: Translation, other: Translation? -> obj.add(other!!) }
                .orElseThrow()
                .assertEmptyPreamble()
            namedParams = Asserts.assertIsLine(namedParams).removeSuffix("\n")
            // 2nd+ lines of blockbody has bad indent, but that's why we go over with shfmt
            val blockBody = Asserts.assertIsParagraph(blockStatements.body()).removeSuffix("\n")
            val functionText = """
                $functionName () {
                $namedParams
                $blockBody
                }
                """.trimMargin() + "\n"
            val functionDeclaration = toStringTranslation(functionText)
            comment.add(functionDeclaration)
        }
    }

    fun printStatement(ctx: BashpileParser.PrintStatementContext): Translation {
        LOG.trace("In printStatement")
        // guard
        val argList = ctx.argumentList() ?: return toStringTranslation("""
            printf "\n"
            
            """.trimIndent())

        // body
        val comment = createCommentTranslation("print statement", lineNumber(ctx))
        val arguments: Translation = argList.expression().stream()
            .map(requireNonNull(visitor)::visit)
            .map{ tr: Translation -> tr.inlineAsNeeded { unwindNested(it) } }
            .map { tr: Translation -> unwindNested(tr) }
            .map { tr: Translation ->
                if (tr.isBasicType && !tr.isListAccess && !tr.metadata().contains(CONDITIONAL)) {
                    tr.body("""
                        printf -- "${tr.unquoteBody().body()}\n"
                        
                        """.trimIndent()
                    )
                } else if (tr.isBasicType && !tr.isListAccess /* and a CONDITIONAL */) {
                    // body will already contain [ ... -eq 1 ]
                    tr.body("""
                        if ${tr.unquoteBody().body()}; then printf -- "true"; else printf -- "false"; fi
                        """.trimIndent()
                    )
                } else {
                    // list or contains $@ or [@]
                    // change the Internal Field Separator to a space just for this subshell (parens)
                    tr.body("""
                            (declare -x IFS=${'$'}' '; printf -- "%s\n" "${tr.toStringArray().unquoteBody().body()}")
                            
                            """.trimIndent()
                    )
                }
            }
            .reduce { tr: Translation, otherTranslation: Translation? -> tr.add(otherTranslation!!) }
            .orElseThrow()
        val subcomment = subcommentTranslationOrDefault(arguments.hasPreamble(), "print statement body")
        return comment.add(subcomment.add(arguments).mergePreamble())

    }

    fun returnPsudoStatement(ctx: BashpileParser.ReturnPsudoStatementContext, typeStack: TypeStack): Translation {
        LOG.trace("In returnPsudoStatement")
        val exprExists = ctx.expression() != null

        // guard: check return matches with function declaration
        val enclosingFunction = ctx.parent.parent as BashpileParser.FunctionDeclarationStatementContext
        val functionName = enclosingFunction.Id().text
        val functionTypes: FunctionTypeInfo = typeStack.getFunctionTypes(functionName)
        var exprTranslation =
            if (exprExists) requireNonNull(visitor).visit(ctx.expression()) else Translation.EMPTY_TRANSLATION
        Asserts.assertTypesCoerce(
            functionTypes.returnType,
            exprTranslation.type(),
            functionName,
            lineNumber(ctx)
        )

        // guard: check that the expression exists
        if (!exprExists) {
            return Translation.UNKNOWN_TRANSLATION
        }

        // body

        val comment = createCommentTranslation("return statement", lineNumber(ctx))
        val returnLineLambda = { str: String ->
            if (functionTypes.returnsStr() || ctx.expression() is BashpileParser.NumberExpressionContext) {
                "printf -- \"${Strings.unquote(str)}\"\n"
            } else if (exprTranslation.type() == Type.INT_TYPE && exprTranslation.metadata().contains(CALCULATION)) {
                // Avoid interpreting $(( )) results as a command
                "printf -- $str\n"
            } else {
                str + "\n"
            }
        }
        exprTranslation = exprTranslation.body(Strings.lambdaLastLine(exprTranslation.body(), returnLineLambda))
        return comment.add(exprTranslation.mergePreamble())
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
                tr.inlineAsNeeded { tr1: Translation? -> unwindNested(tr1!!) }
            }
            .toList()

        // child translations in the format of 'expr operator expr', so we are only interested in the first and last
        val first = childTranslations[0]
        val second = Iterables.getLast(childTranslations)

        return if (Translation.areIntExpressions(first, second)) {
            // Integers, we can use the $(( )) syntax
            childTranslations = childTranslations.map {
                val ret = it.lambdaBody { body -> body.removeSurrounding("$(( ", " ))") }
                if (ret.metadata().contains(PARENTHESIZED)) {
                    ret.metadata(it.metadata() - PARENTHESIZED).parenthesizeBody()
                } else ret
            }
            val translationsString = childTranslations.joinToString(" ") { it.body() }
            Translation(translationsString, Type.INT_TYPE, listOf(CALCULATION))
                .body("$(( $translationsString ))")
        } else if (Translation.areNumericExpressions(first, second)) {
            // Numbers -- We need the Basic Calculator to process
            childTranslations = childTranslations.map {
                if (it.metadata().contains(CALCULATION) && it.type() != Type.INT_TYPE) { unwrapCalculation(it) } else it
            }.map {
                if (it.metadata().contains(PARENTHESIZED)) {
                    it.metadata(it.metadata() - PARENTHESIZED).parenthesizeBody()
                } else it
            }
            // first happy path executed, assume no nesting
            val translationsString = childTranslations.joinToString(" ") { it.body() }
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
            throw UserError(message, lineNumber(ctx))
        } else {
            // throw type error for all others
            val message = "Incompatible types in calc: ${first.type()} and ${second.type()}"
            throw TypeError(message, lineNumber(ctx))
        }
    }

    private fun unwrapCalculation(translation: Translation): Translation {
        // in nested calc, first or second may be wrapped in inlines "$()" or parenthesis or "$( ())"
        var ret = translation.lambdaBody { body2 -> body2
            .removeSurrounding("$(", ")")
            .trim()
        }
        var parens = false
        if (ret.body().startsWith("(") && ret.body().endsWith(")")) {
            parens = true
            ret = ret.lambdaBody { body -> body.removeSurrounding("(", ")") }
        }
        ret = ret.lambdaBody { body -> body.removeSurrounding("bc <<< \"","\"") }
        if (parens) ret = ret.parenthesizeBody()
        return ret
    } // end of calculationExpression helpers

    fun unaryPrimaryExpression(ctx: UnaryPrimaryExpressionContext): Translation {
        LOG.trace("In unaryPrimaryExpression")
        var primary = ctx.unaryPrimary().text
        var valueBeingTested: Translation
        // right now all implemented primaries are string tests
        valueBeingTested = visitor.visit(ctx.expression())
            .inlineAsNeeded { tr: Translation? -> unwindNested(tr!!) }

        // for isset (-n) and unset (-z) '+default' will evaluate to nothing if unset, and 'default' if set
        // see https://stackoverflow.com/questions/3601515/how-to-check-if-a-variable-is-set-in-bash for details
        val isSetCheck = listOf("isset", "unset").contains(primary)
        if (isSetCheck) {
            // remove ${ and } as needed
            var modifiedValueBeingTested = StringUtils.removeStart(valueBeingTested.body(), "$")
            modifiedValueBeingTested = StringUtils.removeStart(modifiedValueBeingTested, "{")
            modifiedValueBeingTested = StringUtils.removeEnd(modifiedValueBeingTested, "}")
            valueBeingTested = valueBeingTested.body("\${$modifiedValueBeingTested+default}")
        }

        // translate Bashpile token to Bash and insert.  Special handling for ! ("not")
        primary = unaryPrimaryTranslations.getOrDefault(primary, primary)
        val body = if (primary != "!") {
            // put into portable [ ] test expression
            "[ $primary \"${valueBeingTested.unquoteBody().body()}\" ]"
        } else {
            // valueBeingTested will have [ ] if needed
            "$primary ${valueBeingTested.unquoteBody().body()}"
        }
        return Translation(valueBeingTested.preamble(), body, Type.STR_TYPE, listOf(CONDITIONAL))
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
            var ret = it.inlineAsNeeded { tr: Translation? -> unwindNested(tr!!) }
            if (ret.metadata().contains(PARENTHESIZED)) {
                // wrap in a block and add an end-of-statement
                ret = ret.body("{ ${ret.body()}; }")
            }
            ret
        }

        val body = "${translations[0].unquoteBody().body()} $operator ${translations[1].unquoteBody().body()}"
        return toStringTranslation(body)
            .addPreamble(translations[0].preamble())
            .addPreamble(translations[1].preamble())
            .metadata(CONDITIONAL)
    }
}