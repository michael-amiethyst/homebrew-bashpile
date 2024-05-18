package com.bashpile.engine

import com.bashpile.Asserts
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
        /** Used to ensure variable names are unique  */
        private var subshellWorkaroundCounter = 0
        private val LOG = LogManager.getLogger(Unwinder::class)

        @JvmStatic
        fun unwindAll(tr: Translation): Translation {
            var ret = tr
            while (BashTranslationHelper.COMMAND_SUBSTITUTION.matcher(ret.body()).find()) {
                ret = unwindOnMatch(ret, BashTranslationHelper.COMMAND_SUBSTITUTION)
            }
            return ret
        }

        @JvmStatic
        fun unwindNested(tr: Translation): Translation {
            var ret = tr
            while (BashTranslationHelper.NESTED_COMMAND_SUBSTITUTION.matcher(ret.body()).find()) {
                ret = unwindOnMatch(ret, BashTranslationHelper.NESTED_COMMAND_SUBSTITUTION)
            }
            return ret
        }

        private fun unwindOnMatch(tr: Translation, pattern: Pattern): Translation {
            var ret = tr
            // extract inner command substitution
            var middleGroup: String
            var toProcess = ret.body()
            var processedBodyPrefix = ""
            do {
                val bodyMatcher = pattern.matcher(toProcess)
                if (!bodyMatcher.find()) {
                    return ret
                }
                middleGroup = bodyMatcher.group(2)
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
                    val unnestedBody = Matcher.quoteReplacement(unnested.body())
                    LOG.debug("Replacing with {}", unnestedBody)
                    ret = ret.body(bodyMatcher.replaceFirst("$1$unnestedBody$3"))
                    ret = ret.addPreamble(unnested.preamble())
                } else if (!bodyMatcher.group(1).isEmpty()) {
                    // discard group 1
                    toProcess = bodyMatcher.replaceFirst("$2$3")
                    processedBodyPrefix = bodyMatcher.group(1)
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
            if (!BashTranslationHelper.COMMAND_SUBSTITUTION.matcher(tr.body()).find()) {
                LOG.debug("Skipped unnest for " + tr.body())
                return tr
            }
            if (BashTranslationHelper.NESTED_COMMAND_SUBSTITUTION.matcher(tr.body()).find()) {
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