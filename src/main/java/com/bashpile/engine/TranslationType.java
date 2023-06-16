package com.bashpile.engine;

public enum TranslationType {
    STRING,
    // Bash example: (commands)
    SUBSHELL_COMPOUND,
    // Bash example: $(commands)
    SUBSHELL_SUBSTITUTION

    // Maybe int, float, list, hash?
}
