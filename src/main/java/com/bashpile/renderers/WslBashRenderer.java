package com.bashpile.renderers;

import com.bashpile.AstNode;

import java.util.List;

/** For Windows Subsystem for Linux Bash 5.1.16 with Ubuntu */
public class WslBashRenderer implements ShellScriptRenderer<List<Integer>> {
    public List<Integer> render(AstNode<List<Integer>> root) {
        return root.getValue();
    }
}
