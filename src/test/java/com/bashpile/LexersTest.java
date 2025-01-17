package com.bashpile;

import java.io.IOException;
import java.nio.file.Path;

import com.bashpile.maintests.BashpileTest;
import com.bashpile.shell.BashShell;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(3)
public class LexersTest extends BashpileTest {

    @Test
    public void cdIsLinuxCommand() {
        assertTrue(Lexers.isLinuxCommand("cd ~"));
    }

    @Test
    public void awkIsLinuxCommand() {
        assertTrue(Lexers.isLinuxCommand("""
                awk 'BEGIN{RS="\\1";ORS="";getline;gsub("\\r","");print>ARGV[1]}' filename"""));
    }

    @Test
    public void awkWithPreambleIsLinuxCommand() {
        assertTrue(Lexers.isLinuxCommand("""
                a=36 TEST='true' _test4="yes" awk 'BEGIN{RS="\\1";ORS="";getline;gsub("\\r","");print>ARGV[1]}' filename
                """));
    }

    @Test
    public void functionIsNotLinuxCommand() {
        assertFalse(Lexers.isLinuxCommand("function times2point5:float(x:float):"));
    }

    @Test
    public void relativeCommandIsLinuxCommand() throws IOException {
        String command = "src/test/resources/scripts/my_ls.bash";
        // must be executable to register as a command
        assertSuccessfulExitCode(BashShell.runAndJoin("chmod +x " + command));
        assertTrue(Lexers.isLinuxCommand(command));
    }

    @Test
    public void relativeCommandWithArgumentIsLinuxCommand() {
        assertTrue(Lexers.isLinuxCommand("src/test/resources/scripts/my_ls.bash escapedString.bps"));
    }

    @Test
    public void relativeCommandWithDotSlashIsLinuxCommand() {
        assertTrue(Lexers.isLinuxCommand("./src/test/resources/scripts/my_ls.bash escapedString.bps"));
    }

    @Test
    public void absoluteCommandIsLinuxCommand() {
        String absolutePath = Path.of("./src/test/resources/scripts/my_ls.bash").toAbsolutePath().toString();
        assertTrue(Lexers.isLinuxCommand(absolutePath), "%s was not a command".formatted(absolutePath));
    }

    @Test
    public void absoluteCommandWithDashesIsLinuxCommand() {
        String absolutePath = Path.of("./src/test/resources/scripts/ls-with-dashes.bash-with-dashes")
                .toAbsolutePath().toString();
        assertTrue(Lexers.isLinuxCommand(absolutePath), "%s was not a command".formatted(absolutePath));
    }

    @Test
    public void elseIfIsNotLinuxCommand() {
        assertFalse(Lexers.isLinuxCommand("else-if check:"), "'else-if check:' was a command");
    }

    @Test
    public void printlnIsNotLinuxCommand() {
        assertFalse(Lexers.isLinuxCommand("println"), "'println' was a command");
    }
}
