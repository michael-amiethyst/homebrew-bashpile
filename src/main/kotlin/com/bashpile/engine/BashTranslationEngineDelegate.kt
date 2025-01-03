package com.bashpile.engine

import com.bashpile.Asserts
import com.bashpile.BashpileParser
import com.bashpile.BashpileParser.LiteralContext
import com.bashpile.BashpileParser.UnaryPrimaryExpressionContext
import com.bashpile.Strings
import com.bashpile.engine.BashTranslationHelper.*
import com.bashpile.engine.bast.Translation
import com.bashpile.engine.bast.Translation.toStringTranslation
import com.bashpile.engine.strongtypes.FunctionTypeInfo
import com.bashpile.engine.strongtypes.ParameterInfo
import com.bashpile.engine.strongtypes.TranslationMetadata.*
import com.bashpile.engine.strongtypes.Type
import com.bashpile.engine.strongtypes.TypeStack
import com.bashpile.exceptions.BashpileUncheckedException
import com.bashpile.exceptions.TypeError
import com.bashpile.exceptions.UserError
import com.google.common.collect.Iterables
import org.antlr.v4.runtime.tree.ParseTree
import org.apache.commons.lang3.StringUtils.removeEnd
import org.apache.commons.lang3.StringUtils.removeStart
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.Objects.requireNonNull
import java.util.stream.Stream

fun LiteralContext.getDefaultValue(): String {
    return if (Empty() == null) {
        text ?: ""
    } else {
        /* empty token translates to the empty string */
        ""
    }
}


/** For the [BashTranslationEngine] to have complex code be in Kotlin */
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

    /** [BashTranslationEngine.functionCallExpression] */
    fun functionDeclarationStatement(
        ctx: BashpileParser.FunctionDeclarationStatementContext,
        foundForwardDeclarations: Set<String>,
        typeStack: TypeStack
    ): Translation {
        LOG.trace("In functionDeclarationStatement")
        // guard - avoid translating twice if was part of a forward declaration
        val functionName = ctx.Id().text
        if (foundForwardDeclarations.contains(functionName)) {
            return Translation.UNKNOWN_TRANSLATION
        }

        // guard - check for double declaration
        if (typeStack.containsFunction(functionName)) {
            val message = "$functionName was declared twice (function overloading is not supported)"
            throw UserError(message, lineNumber(ctx))
        }

        // body

        // register function param types and return type in previous stackframe
        val parameters = ctx.paramaters().typedId()
        val defaultedParameters = ctx.paramaters().defaultedTypedId()
        val typeList: List<ParameterInfo> =
            parameters.map { ParameterInfo(it.Id().text, Type.valueOf(it.complexType()!!), "") } +
                    defaultedParameters.map {
                        val type = Type.valueOf(it.typedId().complexType()!!)
                        ParameterInfo(it.typedId().Id().text, type, it.literal().getDefaultValue())
                    }.toList()
        val retType: Type = Type.valueOf(ctx.complexType())
        typeStack.putFunctionTypes(functionName, FunctionTypeInfo(typeList, retType))

        // Create final translation and variables
        return typeStack.pushFrame().use { _ ->

            // unify regular parameters and optional parameters with defaults
            var joinedParams: List<Pair<String, String?>> = parameters.map { Pair(it.Id().text, null) }
            joinedParams = joinedParams +
                    defaultedParameters.map { Pair(it.typedId().Id().text, it.literal().getDefaultValue()) }

            // declare parameter names, include default values as needed
            var namedParams = if (joinedParams.isNotEmpty()) {
                // local var1=$1; local var2=$2; etc
                val paramDeclarations = joinedParams
                    .mapIndexed { index, idDefaultPair ->
                        val i: Int = index + 1
                        val varName: String = idDefaultPair.first
                        val type: Type = typeStack.getVariableType(varName)

                        // special handling for lists with 'read -a'
                        if (type.isList) {
                            return@mapIndexed "declare -x IFS=$' ';" +
                                    " read -r -a $varName <<< \"$$i\"; declare -x IFS=$'\\n\\t';"
                        }

                        // normal processing
                        // don't add 'i' for Bash integer, that munges an empty optional argument to 0 automatically
                        "declare $varName=$$i; $varName=${'$'}{$varName:=${idDefaultPair.second}};"
                    }.joinToString(" ", "set +u; ", "set -u;") // some args may be unset
                BashTranslationEngine.TAB + paramDeclarations + "\n"
            } else {
                BashTranslationEngine.TAB + "# no parameters to function" + "\n"
            }

            // create statements for the body of the function
            val blockStatements = streamContexts(
                ctx.functionBlock().statement(), ctx.functionBlock().returnPsudoStatement()
            )
                .map { visitor.visit(it) }
                .map { tr: Translation -> tr.lambdaBodyLines { BashTranslationEngine.TAB + it }
                    .lambdaBody { it.replace("exit 1", "return 1") }
                }.reduce { obj: Translation, other: Translation? -> obj.add(other!!) }
                .orElseThrow()

            // put it all together in one big translation
            namedParams = Asserts.assertIsLine(namedParams).removeSuffix("\n")
            val blockBody = Asserts.assertIsParagraph(blockStatements.body()).removeSuffix("\n")
            // 2nd+ lines of blockbody will have a bad indent, but that's why we go over with shfmt
            val functionText = """
                $functionName () {
                $namedParams
                $blockBody
                }
                """.trimMargin() + "\n"
            val functionDeclaration = toStringTranslation(functionText)
            val comment = createCommentTranslation("function declaration", lineNumber(ctx))
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
            .map{ tr: Translation -> tr.inlineAsNeeded() }
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
        return comment.add(arguments)
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
            } else if (exprTranslation.isNumeric && exprTranslation.metadata().contains(NORMAL)) {
                // plain number type such as int or float equaling 42
                "printf -- $str\n"
            } else {
                str + "\n"
            }
        }
        exprTranslation = exprTranslation.body(Strings.lambdaLastLine(exprTranslation.body(), returnLineLambda))
        return comment.add(exprTranslation)
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
            .map { tr: Translation -> tr.inlineAsNeeded() }.toList()

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
        valueBeingTested = visitor.visit(ctx.expression()).inlineAsNeeded()

        // for isset (-n) and unset (-z) '+default' will evaluate to nothing if unset, and 'default' if set
        // see https://stackoverflow.com/questions/3601515/how-to-check-if-a-variable-is-set-in-bash for details
        val isSetCheck = listOf("isset", "unset").contains(primary)
        if (isSetCheck) {
            // remove ${ and } as needed
            var modifiedValueBeingTested = removeStart(valueBeingTested.body(), "$")
            modifiedValueBeingTested = removeStart(modifiedValueBeingTested, "{")
            modifiedValueBeingTested = removeEnd(modifiedValueBeingTested, "}")
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
        return Translation(body, Type.STR_TYPE, listOf(CONDITIONAL))
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
            var ret = it.inlineAsNeeded()
            if (ret.metadata().contains(PARENTHESIZED)) {
                // wrap in a block and add an end-of-statement
                ret = ret.body("{ ${ret.body()}; }")
            }
            ret
        }

        val body = "${translations[0].unquoteBody().body()} $operator ${translations[1].unquoteBody().body()}"
        return toStringTranslation(body).metadata(CONDITIONAL)
    }
}