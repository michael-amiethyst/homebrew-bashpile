package com.bashpile.engine.strongtypes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public record TypeStackframe(Map<String, FunctionTypeInfo> functions, Map<String, Type> variables) {
    public static @Nonnull TypeStackframe of() {
        return new TypeStackframe(HashMap.newHashMap(10), HashMap.newHashMap(10));
    }
}
