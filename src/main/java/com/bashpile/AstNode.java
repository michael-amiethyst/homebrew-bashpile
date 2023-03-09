package com.bashpile;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Syntax Tree node.  Analogous to the DOM in front-end programming.
 */
public class AstNode {

    public List<AstNode> children = new ArrayList<>();

    public Integer value;

    public AstNode(Integer in) {
        value = in;
    }
}
