package com.bashpile.engine.bast;

import java.util.List;

public interface TreeNode<T, U extends TreeNode<T, U>> {
    T render();
    U addChild(U child);
    U addAllChildren(List<U> stream);
}
