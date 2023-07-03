package com.bashpile.engine.strongtypes;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

/**
 * A call stack but just for Type information to implement strong typing.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Call_stack">Wikipedio - Call Stack</a>
 */
public class TypeStack {
    private final Stack<TypeStackframe> frames;

    public TypeStack() {
        frames = new Stack<>();
        frames.push(TypeStackframe.of());
    }

    public void putVariableType(@Nonnull final String variableName, @Nonnull final Type type) {
        frames.peek().variables().put(variableName, type);
    }

    public @Nonnull Type getVariableType(@Nonnull final String variableName) {
        // foreach stack frame search for variableName in the variableMap
        Optional<Map<String, Type>> topmostOptional = frames.stream()
                .map(TypeStackframe::variables)
                .filter(x -> x.containsKey(variableName))
                // .stream() starts at the bottom of the stack so, we need to get the last match
                .reduce((first, second) -> second);
        if (topmostOptional.isPresent()) {
            return topmostOptional.get().get(variableName);
        } // else
        return Type.NOT_FOUND;
    }

    public boolean containsVariable(@Nonnull final String variableName) {
        final Type foundType = getVariableType(variableName);
        return foundType != Type.NOT_FOUND;
    }

    public void putFunctionTypes(@Nonnull final String functionName, @Nonnull final FunctionTypeInfo functionTypeInfo) {
        frames.peek().functions().put(functionName, functionTypeInfo);
    }

    public @Nonnull FunctionTypeInfo getFunctionTypes(@Nonnull final String functionName) {
        // foreach stack frame search for variableName in the variableMap
        Optional<Map<String, FunctionTypeInfo>> topmostOptional = frames.stream()
                .map(TypeStackframe::functions)
                .filter(x -> x.containsKey(functionName))
                // .stream() starts at the bottom of the stack, so we need to get the last match
                .reduce((first, second) -> second);
        if (topmostOptional.isPresent()) {
            return topmostOptional.get().get(functionName);
        } // else
        return FunctionTypeInfo.EMPTY;
    }

    public boolean containsFunction(@Nonnull final String functionName) {
        final FunctionTypeInfo foundFunction = getFunctionTypes(functionName);
        return foundFunction != FunctionTypeInfo.EMPTY;
    }

    public void push() {
        frames.push(TypeStackframe.of());
    }

    public void pop() {
        frames.pop();
    }
}
