package com.bashpile.engine.strongtypes;

import com.bashpile.engine.Translation;

import java.util.function.Function;

/**
 * Additional information about a Translation's Type.
 * So we can have a Command that evaluates to an int for example.
 */
public enum TranslationMetadata {
    /** Just a translation with no special handing needed */
    NORMAL,
    /**
     * Calc expressions (`bc`) frequently need to be inlines, but not always.
     * @see Translation#inlineAsNeeded(Function)
     */
    NEEDS_INLINING_OFTEN,
    /**
     * Our name for Bash command substitution, i.e. $(commands).
     * An inline translation may need the inline logic to be moved into the preamble still.
     */
    INLINE,
    /** Something like "[ 4 < 5 ]" */
    CONDITIONAL
}
