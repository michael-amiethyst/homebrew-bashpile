package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.shell.BashShell;
import com.bashpile.shell.ExecutionResults;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;

public class Lexers {

    private static final ArrayList<String> commands = new ArrayList<>(100);

    static {
        try {
            // TODO cache `compgen -A function -abck`
            commands.addAll(BashShell.runAndJoin("compgen -b").stdoutLines());
            commands.add("ls");
            commands.add("xargs");
            commands.add("find");
        } catch (IOException e) {
            throw new BashpileUncheckedException(e);
        }
    }

    // TODO document
    // TODO move DenterHelper here
    public static boolean isLinuxCommand(@Nonnull final String str) {
        final String command = str.split("[ \t\\n]", 2)[0];
        // TODO uncomment?
//        return commands.contains(command);
        try {
            return BashShell.runAndJoin("which " + command).exitCode() == ExecutionResults.SUCCESS;
        } catch (IOException e) {
            return false;
        }
    }
}
