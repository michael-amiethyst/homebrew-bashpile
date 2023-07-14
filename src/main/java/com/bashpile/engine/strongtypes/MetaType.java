package com.bashpile.engine.strongtypes;

/** The type of the type.  So we can have Strings that represent sub-shell substitutions for example. */
public enum MetaType {
    /** Just a string with no special handing usually needed */
    NORMAL,
    /** A shell command, e.g. `echo filename | cat` */
    COMMAND,
    /** Bash example: (commands) */
    SUBSHELL,
    /** Bash example: $(commands) */
    COMMAND_SUBSTITUTION

    // Maybe int, float, list, hash?
}
