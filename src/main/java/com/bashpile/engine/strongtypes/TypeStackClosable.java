package com.bashpile.engine.strongtypes;

import javax.annotation.Nonnull;
import java.io.Closeable;

/**
 * Adapter for TypeStack to the Closable interface.
 * Will automatically push and pop the TypeStack in a try-with-resources block
 */
public class TypeStackClosable implements Closeable {

    private final TypeStack typeStack;

    public TypeStackClosable(@Nonnull final TypeStack typeStack) {
        this.typeStack = typeStack;
        this.typeStack.push();
    }

    @Override
    public void close() {
        typeStack.pop();
    }
}
