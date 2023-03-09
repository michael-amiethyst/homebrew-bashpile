package com.bashpile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Abstract Syntax Tree node.  Analogous to the DOM in front-end programming.
 */
public class AstNode {

    // TODO impl
    public List<AstNode> children = new ArrayList<>();

    private final Integer value;

    public final Supplier<AstNode> logic;

    public AstNode(Integer value) {
        this.value = value;
        this.logic = null;
    }

    public AstNode(Supplier<AstNode> logic) {
        this.value = null;
        this.logic = logic;
    }

    public Integer getValue() {
        if (value != null) {
            return value;
        } // else
        assert logic != null;
        return logic.get().getValue();
    }
}
