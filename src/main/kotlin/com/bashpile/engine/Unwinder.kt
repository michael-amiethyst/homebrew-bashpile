package com.bashpile.engine

import com.bashpile.engine.Unwinder.Companion.unwindAll
import com.bashpile.engine.Unwinder.Companion.unwindNested

/**
 * Has a concept of unwinding command substitutions.  Either only nested ones with [unwindNested] or
 * all with [unwindAll].  This is to prevent errored exit codes from being suppressed.  As examples,
 * in Bash `$(echo $(echo hello; exit 1))` will suppress the error code and `[ -z $(echo hello; exit 1) ]` will as well.
 */
class Unwinder {

    companion object {

        /**
         * For when [tr] is going into a command substitution or test ('[' ... ']'].
         *
         * A test will also disregard a failing exit code.
         */
        // TODO remove
        @JvmStatic
        fun unwindAll(tr: Translation): Translation {
            return tr
        }

        // TODO remove
        @JvmStatic
        fun unwindNested(tr: Translation): Translation {
            return tr
        }
    }
}