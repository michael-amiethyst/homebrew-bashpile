package com.bashpile;

import com.bashpile.exceptions.BashpileUncheckedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BashpileMainHelperTest {

    @Test
    void transpileScript() {
        assertThrows(BashpileUncheckedException.class,
                () -> BashpileMainHelper.transpileScript("@", false));
    }
}