package com.bashpile.maintests;

import com.bashpile.BashpileMainHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;

@Order(1000)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullScriptsBashpileTest extends BashpileTest {

    private static final Logger LOG = LogManager.getLogger(ConditionalsBashpileTest.class);

    @Test
    @Order(10)
    public void bpcCompiles() throws IOException {
        final String bashpileScript = """
// TODO this should be able to throw an error message and have it be displayed

// find the jar location
bpHome: str
if unset BASHPILE_HOME:
    bpWhere: str = #(whereis bashpile | cut -d ' ' -f 2)
    bpHome = #(dirname $bpWhere)
else:
    BASHPILE_HOME: str
    bpHome = BASHPILE_HOME + "/bin"
jarPath: str = bpHome + "/bashpile.jar"

// process args
// case "-" not working with Bash for some reason
if isset arguments[1] and arguments[1] == "-":
    commandString: str = #(cat -)
    #(java -jar "$jarPath" -c "$commandString")
else:
    #(java -jar "$jarPath" "$@")
                """;
        final String results = BashpileMainHelper.transpileScript(bashpileScript);
        LOG.info("BPC: " + results);
    }

}
