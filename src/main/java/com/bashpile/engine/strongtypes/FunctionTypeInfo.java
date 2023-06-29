package com.bashpile.engine.strongtypes;

import java.util.List;

public record FunctionTypeInfo(List<Type> parameterTypes, Type returnType) {
    public static final FunctionTypeInfo EMPTY = new FunctionTypeInfo(List.of(), Type.NA);
}
