package com.bashpile.engine.strongtypes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/** Holds all the functions and variables for the current context */
public record TypeStackframe(@Nonnull Map<String, FunctionTypeInfo> functions, @Nonnull Map<String, Type> variables) {

    /** Creates a new TypeStackFrame */
    public static @Nonnull TypeStackframe of() {
        return new TypeStackframe(HashMap.newHashMap(10), HashMap.newHashMap(10));
    }
}
