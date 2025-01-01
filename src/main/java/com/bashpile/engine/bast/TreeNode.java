package com.bashpile.engine.bast;

public interface TreeNode<T> {
    T getData();
    TreeNode<T> add(TreeNode<T> child);
}
