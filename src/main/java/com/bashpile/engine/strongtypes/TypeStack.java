package com.bashpile.engine.strongtypes;

import com.bashpile.BashpileParser;
import com.bashpile.engine.BashTranslationEngine;
import com.bashpile.exceptions.UserError;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import static com.bashpile.Asserts.assertMapDoesNotContainKey;
import static com.bashpile.engine.strongtypes.Type.NOT_FOUND_TYPE;

/**
 * A call stack but just for Type information to implement strong typing.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Call_stack">Wikipedia - Call Stack</a>
 */
public class TypeStack {
    private final Stack<TypeStackframe> frames;

    public TypeStack() {
        frames = new Stack<>();
        frames.push(TypeStackframe.of());
    }

    /** Puts the variable's type into the current stackframe.  lineNumber is needed for error information */
    public void putVariableType(
            @Nonnull final String variableName, @Nonnull final Type type, final int lineNumber) {
        final Map<String, Type> typeMap = frames.peek().variables();
        assertMapDoesNotContainKey(variableName, typeMap, new UserError(
                "%s is already declared as a %s".formatted(variableName, type.name()), lineNumber));
        typeMap.put(variableName, type);
    }

    /** Gets the type of the variable, or NOT_FOUND */
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
        return NOT_FOUND_TYPE;
    }

    /** Checks if the variable is defined */
    public boolean containsVariable(@Nonnull final String variableName) {
        final Type variableType = getVariableType(variableName);
        return variableType.isFound();
    }

    /** Puts the function's type information into the current stackframe */
    public void putFunctionTypes(@Nonnull final String functionName, @Nonnull final FunctionTypeInfo functionTypeInfo) {
        frames.peek().functions().put(functionName, functionTypeInfo);
    }

    /** Gets the type information for the function, or {@link FunctionTypeInfo#EMPTY}. */
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

    /** Checks if the function is defined */
    public boolean containsFunction(@Nonnull final String functionName) {
        final FunctionTypeInfo foundFunction = getFunctionTypes(functionName);
        return foundFunction != FunctionTypeInfo.EMPTY;
    }

    /**
     * Push a new stack frame.
     * <br>
     * See {@link BashTranslationEngine#functionDeclarationStatement(BashpileParser.FunctionDeclarationStatementContext)}
     * for example usage.
     *
     * @return a type to pop the frame after it is out of scope with a try-with-resources statement.
     * @see <a href=https://www.techopedia.com/definition/22304/stack-frame>Stack Frame definition</a>
     */
    public @Nonnull TypeStackClosable pushFrame() {
        return new TypeStackClosable(this);
    }

    /* package */ void push() {
        frames.push(TypeStackframe.of());
    }

    /* package */ void pop() {
        frames.pop();
    }
}
