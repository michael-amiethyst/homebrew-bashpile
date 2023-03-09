package com.bashpile;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Syntax Tree node.  Just has children and value for now.
 */
public class AstNode {

    public List<AstNode> children = new ArrayList<>();

    public Integer value;

    public AstNode(Integer in) {
        value = in;
    }
}
