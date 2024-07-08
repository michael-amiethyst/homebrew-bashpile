package com.bashpile;

import com.bashpile.maintests.BashpileTest;
import com.bashpile.shell.BashShell;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Order(3)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LexersTest extends BashpileTest {

    @Test @Order(10)
    public void cdIsLinuxCommand() {
        assertTrue(Lexers.isLinuxCommand("cd ~"));
    }

    @Test @Order(20)
    public void awkIsLinuxCommand() {
        assertTrue(Lexers.isLinuxCommand("""
                awk 'BEGIN{RS="\\1";ORS="";getline;gsub("\\r","");print>ARGV[1]}' filename"""));
    }

    @Test @Order(30)
    public void awkWithPreambleIsLinuxCommand() {
        assertTrue(Lexers.isLinuxCommand("""
                a=36 TEST='true' _test4="yes" awk 'BEGIN{RS="\\1";ORS="";getline;gsub("\\r","");print>ARGV[1]}' filename
                """));
    }

    @Test @Order(40)
    public void functionIsNotLinuxCommand() {
        assertFalse(Lexers.isLinuxCommand("function times2point5:float(x:float):"));
    }

    @Test @Order(50)
    public void relativeCommandIsLinuxCommand() throws IOException {
        String command = "src/test/resources/scripts/my_ls.bash";
        // must be executable to register as a command
        assertSuccessfulExitCode(BashShell.runAndJoin("chmod +x " + command));
        assertTrue(Lexers.isLinuxCommand(command));
    }

    @Test @Order(60)
    public void relativeCommandWithArgumentIsLinuxCommand() {
        assertTrue(Lexers.isLinuxCommand("src/test/resources/scripts/my_ls.bash escapedString.bps"));
    }

    @Test @Order(70)
    public void relativeCommandWithDotSlashIsLinuxCommand() {
        assertTrue(Lexers.isLinuxCommand("./src/test/resources/scripts/my_ls.bash escapedString.bps"));
    }

    @Test @Order(80)
    public void absoluteCommandIsLinuxCommand() {
        String absolutePath = Path.of("./src/test/resources/scripts/my_ls.bash").toAbsolutePath().toString();
        assertTrue(Lexers.isLinuxCommand(absolutePath), "%s was not a command".formatted(absolutePath));
    }

    @Test @Order(90)
    public void absoluteCommandWithDashesIsLinuxCommand() {
        String absolutePath = Path.of("./src/test/resources/scripts/ls-with-dashes.bash-with-dashes")
                .toAbsolutePath().toString();
        assertTrue(Lexers.isLinuxCommand(absolutePath), "%s was not a command".formatted(absolutePath));
    }
}
