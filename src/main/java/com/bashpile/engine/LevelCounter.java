package com.bashpile.engine;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.util.HashMap;

/** All instances of the same label share static data */
public class LevelCounter implements Closeable {

    // static constants and variables

    /** A label for changing lexical scopes */
    public static final String BLOCK_LABEL = "block";

    /** A command substitution label */
    public static final String CALC_LABEL = "calc";

    private static final HashMap<String, Integer> counters = HashMap.newHashMap(20);

    // static methods

    /** are we in any level of indention for this label */
    public static boolean in(@Nonnull final String name) {
        return counters.containsKey(name);
    }

    /** How many levels are we in for label? */
    public static int get(@Nonnull final String label) {
        return counters.getOrDefault(label, 0);
    }

    // class fields, constructors and methods

    private final String label;

    public LevelCounter(@Nonnull final String label) {
        this.label = label;
        if (counters.containsKey(label)) {
            // increment
            counters.put(label, counters.get(label) + 1);
        } else {
            counters.put(label, 1);
        }

    }

    @Override
    public void close() {
        int count = counters.get(label);
        count--;
        if (count <= 0) {
            counters.remove(label);
        } else {
            counters.put(label, count);
        }
    }
}
