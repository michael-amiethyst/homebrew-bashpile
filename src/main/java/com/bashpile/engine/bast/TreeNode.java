package com.bashpile.engine.bast;

import java.util.stream.Stream;

public interface TreeNode<T> {
    T render();
    TreeNode<T> addChild(TreeNode<T> child);
    TreeNode<T> addAllChildren(Stream<TreeNode<T>> stream);
}
