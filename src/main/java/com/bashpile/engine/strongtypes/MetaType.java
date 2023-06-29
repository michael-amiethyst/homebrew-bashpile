package com.bashpile.engine.strongtypes;

/** The type of the type.  So we can have Strings that represent sub-shell substitutions for example. */
public enum MetaType {
    /** Just a string with no special handing usually needed */
    NORMAL,
    /** Bash example: (commands) */
    SUBSHELL_COMPOUND,
    /** Bash example: $(commands) */
    SUBSHELL_SUBSTITUTION

    // Maybe int, float, list, hash?
}
