package com.bashpile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Abstract Syntax Tree node.  Analogous to the DOM in front-end programming.
 */
public class AstNode <T> {

    // TODO impl
    public List<AstNode<T>> children = new ArrayList<>();

    private final T value;

    public final Supplier<AstNode<T>> logic;

    public AstNode(T value) {
        this.value = value;
        this.logic = null;
    }

    public AstNode(Supplier<AstNode<T>> logic) {
        this.value = null;
        this.logic = logic;
    }

    public T getValue() {
        if (value != null) {
            return value;
        } // else
        assert logic != null;
        return logic.get().getValue();
    }
}
