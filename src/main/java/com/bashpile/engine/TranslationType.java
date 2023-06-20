package com.bashpile.engine;

public enum TranslationType {
    /** Just a string with no special handing usually needed */
    STRING,
    /** Bash example: (commands) */
    SUBSHELL_COMPOUND,
    /** Bash example: $(commands) */
    SUBSHELL_SUBSTITUTION

    // Maybe int, float, list, hash?
}
