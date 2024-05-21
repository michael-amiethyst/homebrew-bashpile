package com.bashpile.engine

import com.bashpile.Asserts
import com.bashpile.engine.Unwinder.Companion.unwindAll
import com.bashpile.engine.Unwinder.Companion.unwindNested
import com.bashpile.engine.strongtypes.TranslationMetadata
import com.bashpile.engine.strongtypes.Type
import org.apache.logging.log4j.LogManager
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Has a concept of unwinding command substitutions.  Either only nested ones with [unwindNested] or
 * all with [unwindAll].  This is to prevent errored exit codes from being suppressed.  As examples,
 * in Bash `$(echo $(echo hello; exit 1))` will suppress the error code and `[ -z $(echo hello; exit 1) ]` will as well.
 */
class Unwinder {

    companion object {

        // TODO 0.22.0 replace REGEX with manual computation, str -> List<Str> of length 3 or 0
        /**
         * This single-line Pattern has three matching groups.
         * They are the start of the outer command substitution, the inner command substitution and the remainder of
         * the outer command substitution.  The first two groups have a negative lookahead, "(?!\()", to ignore
         * the arithmetic built-in $(( ))
         */
        private val NESTED_COMMAND_SUBSTITUTION =
            Pattern.compile("(?s)(\\$\\(.*?)(\\$\\((?!\\().*?\\))(.*?\\))")

        /** Used to ensure variable names are unique  */
        private var subshellWorkaroundCounter = 0
        private val LOG = LogManager.getLogger(Unwinder::class)

        @JvmStatic
        fun unwindAll(tr: Translation): Translation {
            var ret = tr
            while (splitOnCommandSubstitution(ret.body()).isNotEmpty()) {
                ret = unwindOnMatch(ret, null)
            }
            return ret
        }

        @JvmStatic
        fun unwindNested(tr: Translation): Translation {
            var ret = tr
            while (NESTED_COMMAND_SUBSTITUTION.matcher(ret.body()).find()) {
                ret = unwindOnMatch(ret, NESTED_COMMAND_SUBSTITUTION)
            }
            return ret
        }

        /**
         * Returns three substrings: everything before the inline, the inline, and everything after.
         * Ignores the arithmetic built-in $(( ))
         */
        private fun splitOnCommandSubstitution(str: String): List<String> {
            val ret = mutableListOf("", "", "")
            var i = 0
            var afterDollar = false
            var afterDollarParen = false
            var parenCount = 0
            var foundCommandSubstitution = false

            // add to ret[0] until "$(", add to ret[1] until matching paren, add to ret[2] until done
            str.forEach { ch ->
                when (ch) {
                    '$' -> {
                        if (afterDollarParen) {
                            // change to middle
                            ret[0] = ret[0].removeSuffix("$(")
                            ret[1] = ret[1] + "$("
                            parenCount++
                            foundCommandSubstitution = true
                            i = 1
                        }
                        afterDollarParen = false
                        afterDollar = true
                        ret[i] = ret[i] + ch
                    }
                    '(' -> {
                        when (i) {
                            0 -> {
                                if (afterDollarParen) {
                                    afterDollarParen = false
                                }
                                if (afterDollar) {
                                    afterDollarParen = true
                                }
                                ret[i] = ret[i] + ch
                            }
                            1 -> {
                                parenCount++
                                ret[i] = ret[i] + ch
                            }
                            2 -> {ret[i] = ret[i] + ch}
                        }
                        afterDollar = false
                    }
                    ')' -> {
                        if (i == 1) {
                            parenCount--
                            ret[i] = ret[i] + ch
                            if (parenCount == 0) {
                                i = 2
                            }
                        } else ret[i] = ret[i] + ch
                    }
                    else -> {
                        if (afterDollarParen) {
                            // change to middle
                            ret[0] = ret[0].removeSuffix("$(")
                            ret[1] = ret[1] + "$("
                            parenCount++
                            foundCommandSubstitution = true
                            i = 1
                        }
                        ret[i] = ret[i] + ch
                    }
                }
            }
            return if (foundCommandSubstitution) ret else listOf()
        }

        private fun unwindOnMatch(tr: Translation, pattern: Pattern?): Translation {
            var ret = tr
            // extract inner command substitution
            var middleGroup: String
            var toProcess = ret.body()
            var processedBodyPrefix = ""
            do {
                val bodyMatcher = pattern?.matcher(toProcess)
                val parts = if (pattern == null) splitOnCommandSubstitution(toProcess) else listOf()
                // if bodyMatcher or splitOnCommandSubstitution is not a match
                if ((bodyMatcher != null && !bodyMatcher.find()) || (bodyMatcher == null && parts.isEmpty())) {
                    return ret
                }
                middleGroup = bodyMatcher?.group(2) ?: parts[1]
                if (!mismatchedParenthesis(middleGroup)) {
                    // parenthesis match and no outer `$(`
                    val unnested = unwindBody(
                        Translation(
                            middleGroup,
                            Type.STR_TYPE,
                            TranslationMetadata.NORMAL
                        )
                    )
                    // replace group
                    val unnestedBody = if (bodyMatcher != null) {
                        Matcher.quoteReplacement(unnested.body())
                    } else unnested.body()
                    LOG.debug("Replacing with {}", unnestedBody)
                    val nextBody = if (bodyMatcher != null) {
                        bodyMatcher.replaceFirst("$1$unnestedBody$3")
                    } else parts[0] + unnestedBody + parts[2]
                    ret = ret.body(nextBody)
                    ret = ret.addPreamble(unnested.preamble())
                } else if ((bodyMatcher != null && bodyMatcher.group(1).isNotEmpty())
                        || (bodyMatcher == null && parts[0].isNotEmpty())) {
                    // discard group 1
                    toProcess = if (bodyMatcher != null) bodyMatcher.replaceFirst("$2$3") else parts[1] + parts[2]
                    processedBodyPrefix = if (bodyMatcher != null) bodyMatcher.group(1) else parts[0]
                } else {
                    // whole string is a command substitution with enclosed parenthesis?
                    Asserts.assertTrue(
                        !mismatchedParenthesis(ret.body()),
                        "Could not unwind " + ret.body()
                    )
                    return unwindBody(ret)
                }
            } while (mismatchedParenthesis(middleGroup))
            return ret.body(processedBodyPrefix + ret.body())
        }

        private fun mismatchedParenthesis(str: String): Boolean {
            var unmatchedCount = 0
            for (ch in str.toCharArray()) {
                if (ch == '(') {
                    unmatchedCount++
                } else if (ch == ')') {
                    unmatchedCount--
                }
            }
            return unmatchedCount != 0
        }

        /**
         * Subshell and inline errored exit codes are ignored in Bash despite all configurations.
         * This workaround explicitly propagates errored exit codes.
         * Unnests one level.
         *
         * @param tr The base translation.
         * @return A Translation where the preamble is `tr`'s body and the work-around.
         * The body is a Command Substitution of a created variable
         * that holds the results of executing `tr`'s body.
         */
        private fun unwindBody(tr: Translation): Translation {
            // guard to check if unnest not needed
            if (splitOnCommandSubstitution(tr.body()).isEmpty()) {
                LOG.debug("Skipped unnest for " + tr.body())
                return tr
            }
            if (NESTED_COMMAND_SUBSTITUTION.matcher(tr.body()).find()) {
                LOG.debug("Found nested command substitution in unnest: {}", tr.body())
                return unwindNested(tr)
            }

            // assign Strings to use in translations
            val subshellReturn = "__bp_subshellReturn$subshellWorkaroundCounter"
            val exitCodeName = "__bp_exitCode${subshellWorkaroundCounter++}"

            // create 5 lines of translations
            val subcomment = Translation.toStringTranslation("## unnest for ${tr.body()}\n")
            val export = Translation.toStringTranslation("export $subshellReturn\n")
            val assign = Translation.toStringTranslation("$subshellReturn=${tr.body()}\n")
            val exitCode = Translation.toStringTranslation("$exitCodeName=$?\n")
            val check = Translation.toStringTranslation(
                """
                    if [ "${'$'}$exitCodeName" -ne 0 ]; then exit "${'$'}$exitCodeName"; fi
                    
                    """.trimIndent()
            )

            // add the lines up
            val preambles = subcomment.add(export).add(assign).add(exitCode).add(check)

            // add the preambles and swap the body
            return tr.addPreamble(preambles.body()).body("\${$subshellReturn}")
        }

    }
}