package com.bashpile.engine.bast;

import java.util.stream.Stream;

public interface TreeNode<T> {
    T getData();
    TreeNode<T> add(TreeNode<T> child);
    TreeNode<T> addAll(Stream<TreeNode<T>> stream);
}
