package com.bashpile.engine.strongtypes;

/**
 * Additional information about a Translation's Type.
 * So we can have a Command that evaluates to an int for example.
 */
public enum TypeMetadata {
    /** Just a translation with no special handing needed */
    NORMAL,
    /** Bash example: (commands) */
    SUBSHELL,
    /**
     * Our name for Bash command substitution, i.e. $(commands).
     * An inline translation may need the inline logic to be moved into the preamble still.
     */
    INLINE
}
