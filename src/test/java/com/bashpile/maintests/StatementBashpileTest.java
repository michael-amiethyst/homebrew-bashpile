package com.bashpile.maintests;

import com.bashpile.exceptions.BashpileUncheckedException;
import com.bashpile.exceptions.TypeError;
import com.bashpile.exceptions.UserError;
import com.bashpile.shell.BashShell;
import com.bashpile.shell.ExecutionResults;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Order(30)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatementBashpileTest extends BashpileTest {

    @Test @Order(10)
    public void assignBoolWorks() {
        final ExecutionResults results = runText("""
                var: bool = false
                print(var)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("false\n", results.stdout());
    }

    @Test @Order(20)
    public void assignIntWorks() {
        final ExecutionResults results = runText("""
                var: int = 42
                print(var)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("42\n", results.stdout());
    }

    @Test @Order(30)
    public void assignIntExpressionWorks() {
        final ExecutionResults results = runText("""
                someVar: int = 1 + 1
                print(someVar + 2)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("4\n", results.stdout());
    }

    /**
     * References an undeclared variable.
     */
    @Test @Order(40)
    public void duplicateIntAssignmentThrows() {
        assertThrows(UserError.class, () -> runText("""
                someVar: int = 1 + 1
                someVar: str = "2"
                """));
    }

    @Test @Order(41)
    public void assignFloatToIntThrows() {
        assertThrows(TypeError.class, () -> runText("""
                someVar: int = 2.2
                print(someVar + 2)"""));
    }

    @Test @Order(50)
    public void unassignedIntExpressionThrows() {
        assertThrows(UserError.class, () -> runText("""
                someVar: int = 1 + 1
                someOtherVar + 2"""));
    }

    @Test @Order(60)
    public void reassignIntExpressionWorks() {
        final ExecutionResults results = runText("""
                someVar: int = 1 + 1
                someVar = 3
                print(someVar + 2)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("5\n", results.stdout());
    }

    @Test @Order(70)
    public void floatWorks() {
        final ExecutionResults results = runText("""
                var: float = 4000000.999
                print(var)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("4000000.999\n", results.stdout());
    }

    @Test @Order(80)
    public void stringWorks() {
        final ExecutionResults results = runText("""
                world:str="world"
                print(world)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("world\n", results.stdout());
    }

    @Test @Order(81)
    public void stringConcatWorks() {
        final ExecutionResults results = runText("""
                world:str="world"
                print("hello " + world)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("hello world\n", results.stdout());
    }

    @Test @Order(82)
    public void stringBadOperatorWorks() {
        assertThrows(BashpileUncheckedException.class, () -> runText("""
                worldStr:str="world"
                print("hello " * worldStr)"""));
    }

    @Test @Order(90)
    public void blockWorks() {
        final ExecutionResults results = runText("""
                print((3 + 5) * 3)
                block:
                    print(32000 + 32000)
                    block:
                        print(64 + 64)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        final List<String> expected = List.of("24", "64000", "128");
        assertEquals(3, lines.size());
        assertEquals(expected, lines);
    }

    @Test @Order(91)
    public void blockWithNestedInlinesWork() {
        final ExecutionResults results = runText("""
                print((3 + 5) * 3)
                block:
                    print($(echo "$(echo "Captain's log, stardate...")"))
                    block:
                        print(64 + 64)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        final List<String> lines = results.stdoutLines();
        final List<String> expected = List.of("24", "Captain's log, stardate...", "128");
        assertEquals(3, lines.size());
        assertEquals(expected, lines);
    }

    @Test @Order(100)
    public void lexicalScopingWorks() {
        assertThrows(UserError.class, () -> runText("""
                print((38 + 5) * 3)
                block:
                    x: int = 5
                    block:
                        y: int = 7
                print(x * x)"""));
    }

    @Test @Order(110)
    public void commentsWork() {
        final ExecutionResults results = runText("""
                print((38. + 4) * .5)
                // anonymous block
                block:
                    x: float = 5.5 // lexical scoping
                    print(x * 3)
                x: float = 7.7
                print(x - 0.7)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        final List<String> stdoutLines = results.stdoutLines();
        final List<String> expected = List.of("21.0", "16.5", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(results.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(120)
    public void blockCommentsWork() {
        final ExecutionResults results = runText("""
                print((38. + 4) * .5)
                /* anonymous block */
                block:
                    x: float = /* inside of a statement comment */ 5.5
                    /*
                     * extended comment on how fantastic this line is
                     */
                    print(x + x)
                x: float = 7.7
                print(x - 0.7)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        final List<String> stdoutLines = results.stdoutLines();
        final List<String> expected = List.of("21.0", "11.0", "7.0");
        assertEquals(3, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(results.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(130)
    public void bashpileDocsWork() {
        final ExecutionResults results = runText("""
                /**
                    This language is
                    really starting to shape up.
                    It will replace Bash.
                */
                                
                print((38. + 4) * .5)
                myString : str
                // anonymous block
                block:
                    /**
                     * More docs on this inner block
                     */
                    block:
                        x: str = "To boldly"
                        y: str = " go"
                        myString = x + y
                    x: float = 5.5 // lexical scoping
                    print(x - x)
                x: float = 7.7
                print(x - 0.7)
                print(myString)
                """);
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        final List<String> stdoutLines = results.stdoutLines();
        final List<String> expected = List.of("21.0", "0", "7.0", "To boldly go");
        assertEquals(4, stdoutLines.size(),
                "Expected 3 lines but got: [%s]".formatted(results.stdout()));
        assertEquals(expected, stdoutLines);
    }

    @Test @Order(140)
    public void createStatementsWork() {
        final ExecutionResults results = runText("""
                #(rm -f captainsLog.txt || true)
                contents: str
                #(echo "Captain's log, stardate..." > captainsLog.txt) creates "captainsLog.txt":
                    contents = $(cat captainsLog.txt)
                print(contents)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("Captain's log, stardate...\n", results.stdout());
        assertFalse(Files.exists(Path.of("captainsLog.txt")), "file not deleted");
    }

    @Test @Order(150)
    public void createStatementsCanFail() throws IOException {
        try {
            final ExecutionResults results = runText("""
                    #(touch captainsLog.txt || true)
                    contents: str
                    #(echo "Captain's log, stardate..." > captainsLog.txt) creates "captainsLog.txt":
                        contents = $(cat captainsLog.txt)
                    print(contents)""");
            assertCorrectFormatting(results);
            assertFailedExitCode(results);
        } finally {
            Files.deleteIfExists(Path.of("captainsLog.txt"));
        }
    }

    @Test @Order(160)
    public void createStatementTrapsWorks() {
        final String bashpileScript = """
                #(rm -f captainsLog.txt || true)
                contents: str
                #(echo "Captain's log, stardate..." > captainsLog.txt) creates "captainsLog.txt":
                    #(sleep 1)
                    contents = $(cat captainsLog.txt)
                print(contents)""";
        try(final BashShell shell = runTextAsync(bashpileScript)) {
            shell.sendTerminationSignal();
            final ExecutionResults results = shell.join();
            assertCorrectFormatting(results);
            assertFailedExitCode(results);
            assertEquals("", results.stdout());
            assertFalse(Files.exists(Path.of("captainsLog.txt")), "file not deleted");
        }
    }

    @Test @Order(170)
    public void createStatementsWithSimpleNestedInlinesWork() {
        /*
        With inlines this tokenized as
        [@0,0:1='#(',<'#('>,1:0]
        [@1,2:7='echo "',<ShellStringText>,1:2]
        [@2,8:9='$(',<'$('>,1:8]
        [@3,10:14='echo ',<InlineText>,1:10]
        [@4,15:16='$(',<'$('>,1:15]
        [@5,17:49='echo "Captain's log, stardate..."',<InlineText>,1:17]
        [@6,50:50=')',<CParen>,1:50]
        [@7,51:51=')',<CParen>,1:51]
        [@8,52:70='" > captainsLog.txt',<ShellStringText>,1:52]
        [@9,71:71=')',<CParen>,1:71]
        [@10,72:71='newline',<Newline>,1:72]
        [@11,72:71='<EOF>',<EOF>,1:72]
         */
        final ExecutionResults results = runText("""
                #(echo "$(echo $(echo "Captain's log, stardate..."))")""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("Captain's log, stardate...\n", results.stdout());
        assertFalse(Files.exists(Path.of("captainsLog.txt")), "file not deleted");
    }

    @Test @Order(171)
    public void createStatementsWithNestedInlinesWork() {
        final ExecutionResults results = runText("""
                #(rm -f captainsLog.txt || true)
                contents: str
                #(echo "$(echo $(echo "Captain's log, stardate..."))" > captainsLog.txt) creates "captainsLog.txt":
                    contents = $(cat $(echo captainsLog.txt))
                print(contents)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("Captain's log, stardate...\n", results.stdout());
        assertFalse(Files.exists(Path.of("captainsLog.txt")), "file not deleted");
    }

    @Test @Order(180)
    public void nestedCreateStatementsWithNestedInlinesWork() {
        final ExecutionResults results = runText("""
                #(rm -f captainsLog.txt || true)
                #(rm -f captainsLog2.txt || true)
                contents: str
                contents2: str
                #(echo "$(echo $(echo "Captain's log, stardate..."))" > captainsLog.txt) creates "captainsLog.txt":
                    contents = $(cat $(echo captainsLog.txt))
                    #(echo "$(echo $(echo "Captain's log, stardate..."))" > captainsLog2.txt) creates "captainsLog2.txt":
                        contents2 = $(cat $(echo captainsLog2.txt))
                print(contents)
                print(contents2)""");
        assertCorrectFormatting(results);
        assertSuccessfulExitCode(results);
        assertEquals("Captain's log, stardate...\nCaptain's log, stardate...\n", results.stdout());
        assertFalse(Files.exists(Path.of("captainsLog.txt")), "file not deleted");
        assertFalse(Files.exists(Path.of("captainsLog2.txt")), "file2 not deleted");
        assertFalse(results.stdinLines().stream().anyMatch(str -> END_OF_LINE_COMMENT.matcher(str).matches()));
    }

    @Test @Order(190)
    public void nestedCreateStatementTrapsWorks() throws IOException, InterruptedException {
        final String bashpileScript = """
                #(rm -f captainsLog.txt || true)
                #(rm -f captainsLog2.txt || true)
                contents: str
                contents2: str
                #(echo "$(echo $(echo "Captain's log, stardate..."))" > captainsLog.txt) creates "captainsLog.txt":
                    contents = $(cat $(echo captainsLog.txt))
                    #(echo "$(echo $(echo "Captain's log, stardate..."))" > captainsLog2.txt) creates "captainsLog2.txt":
                        contents2 = $(cat $(echo captainsLog2.txt))
                        #(sleep 3)
                    #(sleep 3)
                print(contents)
                print(contents2)""";
        final Path innerFile = Path.of("captainsLog2.txt");
        final Path outerFile = Path.of("captainsLog.txt");
        try(final BashShell shell = runTextAsync(bashpileScript)) {
            Thread.sleep(Duration.ofMillis(100));
            shell.sendTerminationSignal();
            final ExecutionResults results = shell.join();
            assertCorrectFormatting(results);
            assertFailedExitCode(results);
            // TERM signals wipe STDOUT -- unknown why
            assertEquals("", results.stdout());
            assertFalse(Files.exists(innerFile), "inner trap file not deleted");
            assertFalse(Files.exists(outerFile), "outer trap file not deleted");
        } finally {
            Files.deleteIfExists(innerFile);
            Files.deleteIfExists(outerFile);
        }
    }

    @Test @Order(191)
    public void nestedCreateStatementTrapsInAnonymousBlockWorks() throws IOException, InterruptedException {
        final String bashpileScript = """
                #(rm -f captainsLog.txt || true)
                #(rm -f captainsLog2.txt || true)
                block:
                    contents: str
                    contents2: str
                    #(echo "$(echo $(echo "Captain's log, stardate..."))" > captainsLog.txt) creates "captainsLog.txt":
                        contents = $(cat $(echo captainsLog.txt))
                        #(echo "$(echo $(echo "Captain's log, stardate..."))" > captainsLog2.txt) creates "captainsLog2.txt":
                            contents2 = $(cat $(echo captainsLog2.txt))
                            #(sleep 3)
                        #(sleep 3)
                    print(contents)
                    print(contents2)""";
        final Path innerFile = Path.of("captainsLog2.txt");
        final Path outerFile = Path.of("captainsLog.txt");
        try(final BashShell shell = runTextAsync(bashpileScript)) {
            Thread.sleep(Duration.ofMillis(100));
            shell.sendTerminationSignal();
            final ExecutionResults results = shell.join();
            assertCorrectFormatting(results);
            assertFailedExitCode(results);
            // TERM signals wipe STDOUT -- unknown why
            assertEquals("", results.stdout());

            // it can take a while for the deletes in the script percolate (at least with WSL)
            boolean innerFileExists = Files.exists(innerFile);
            int i = 0;
            while (innerFileExists && i++ < 10) {
                Thread.sleep(Duration.ofSeconds(1));
                innerFileExists = Files.exists(innerFile);
            }
            assertFalse(innerFileExists, "inner trap file not deleted");

            boolean outerFileExists = true;
            i = 0;
            while (outerFileExists && i++ < 10) {
                Thread.sleep(Duration.ofSeconds(1));
                outerFileExists = Files.exists(outerFile);
            }
            assertFalse(outerFileExists, "outer trap file not deleted");
        } finally {
            Files.deleteIfExists(innerFile);
            Files.deleteIfExists(outerFile);
        }
    }

    @Test @Order(200)
    public void createStatementWithIdWorks() throws IOException {
        final String bashpileScript = """
                log: str = "output" + #(printf "%d" $$):str + ".txt"
                print(log)
                #(printf "test" > "$log") creates log:
                    #(cat "$log")
                """;
        Path jarLog = null;
        try {
            final ExecutionResults results = runText(bashpileScript);
            assertCorrectFormatting(results);
            assertSuccessfulExitCode(results);
            jarLog = Path.of(results.stdoutLines().get(0));
            assertFalse(Files.exists(jarLog), "trap file not deleted");
        } finally {
            if (jarLog != null) {
                Files.deleteIfExists(jarLog);
            }
        }
    }

    @Test @Order(210)
    public void multilineCreateStatementWorks() throws IOException {
        final String bashpileScript = """
                log: str = #(
                    filename=$(printf "%d.txt" $$)
                    printf "%s" "$filename" > "$filename"
                    printf "%s" "$filename"
                ) creates log:
                    #(cat "$log")
                """;
        Path log = null;
        try {
            final ExecutionResults results = runText(bashpileScript);
            assertCorrectFormatting(results);
            assertSuccessfulExitCode(results);
            assertTrue(results.stdoutLines().get(0).matches("\\d+\\.txt"));
            log = Path.of(results.stdoutLines().get(0));
            assertFalse(Files.exists(log), "trap file not deleted");
        } finally {
            if (log != null) {
                Files.deleteIfExists(log);
            }
        }
    }
}
