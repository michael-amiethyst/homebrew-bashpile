package com.bashpile.engine;

import java.util.List;

public record FunctionTypeInfo(List<Type> parameterTypes, Type returnType) {
}
