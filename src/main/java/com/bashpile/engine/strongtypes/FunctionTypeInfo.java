package com.bashpile.engine.strongtypes;

import java.util.List;

/** Holds the type information for a single function -- the types of the parameters (in order) and the return type */
public record FunctionTypeInfo(List<Type> parameterTypes, Type returnType) {
    public static final FunctionTypeInfo EMPTY = new FunctionTypeInfo(List.of(), Type.NA);
}
