package com.bashpile.renderers;

import com.bashpile.AstNode;

/** For Windows Subsystem for Linux Bash 5.1.16 with Ubuntu */
public class WslBashRenderer implements ShellScriptRenderer<Integer> {
    public Integer render(AstNode root) {
        return root.value;
    }
}
