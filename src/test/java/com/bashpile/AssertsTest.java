package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.junit.jupiter.api.Test;

import static com.bashpile.Asserts.assertIsParagraph;
import static com.bashpile.Asserts.assertIsLine;
import static org.junit.jupiter.api.Assertions.*;

class AssertsTest {

    @Test
    void assertTextBlockCanPass() {
        final String test = """
                something
                with a
                trailing
                newline
                """;
        assertIsParagraph(test);
    }

    @Test
    void assertTextBlockCanPassWithBlankLine() {
        final String test = """
                something
                with a
                blank
                
                line
                """;
        assertIsParagraph(test);
    }

    @Test
    void assertTextBlockCanPassWithForwardDeclaration() {
        final String test = """
                # function forward declaration, Bashpile line 1
                # function declaration, Bashpile line 6 (hoisted)
                circleArea () {
                    local r=$1;
                    # return statement, Bashpile line 7 (hoisted)
                    echo $(bc <<< "3.14*$r*$r")
                }
                                
                # function declaration, Bashpile line 3
                twoCircleArea () {
                    local r1=$1; local r2=$2;
                    # return statement, Bashpile line 4
                    local __bp_0=$(circleArea $r1)
                    local __bp_1=$(circleArea $r2)
                    echo $(bc <<< "$__bp_0+$__bp_1")
                }
                # print statement, Bashpile line 9
                __bp_textReturn=$(twoCircleArea 1 -1)
                __bp_exitCode=$?
                if [ "$__bp_exitCode" -ne 0 ]; then exit "$__bp_exitCode"; fi
                echo "$__bp_textReturn";
                
                """;
        assertIsParagraph(test);
    }

    @Test
    void assertTextBlockCanFail() {
        final String test = """
                something
                without a
                trailing
                newline""";
        assertThrows(BashpileUncheckedException.class, () -> assertIsParagraph(test));
    }

    @Test
    void assertTextLineCanPass() {
        final String test = "one line with newline\n";
        assertIsLine(test);
    }

    @Test
    void assertTextLineCanFail() {
        final String test = "no newline";
        assertThrows(BashpileUncheckedException.class, () -> assertIsLine(test));
    }

    @Test
    void assertTextLineCanFailTwoLines() {
        final String test = "line one\nline two\n";
        assertThrows(BashpileUncheckedException.class, () -> assertIsLine(test));
    }
}