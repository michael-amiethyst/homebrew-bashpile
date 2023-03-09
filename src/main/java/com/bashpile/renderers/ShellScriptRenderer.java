package com.bashpile.renderers;

import com.bashpile.AstNode;

/** Analogous to the renderer in front-end programming */
public interface ShellScriptRenderer <T> {
    T render(AstNode<T> root);
}
