package com.bashpile.engine;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.util.HashMap;

/** All instances of the same label share static data */
public class LevelCounter implements Closeable {

    /** LevelCounter label */
    public static final String BLOCK_LABEL = "block";

    /** LevelCounter label */
    public static final String CALC_LABEL = "calc";

    /** LevelCounter label */
    public static final String INLINE_LABEL = "inline";

    /** LevelCounter label */
    public static final String FORWARD_DECL_LABEL = "forwardDecl";

    /** LevelCounter label */
    public static final String PRINT_LABEL = "print";

    private static final HashMap<String, Integer> counters = HashMap.newHashMap(20);

    /** are we in any level of indention for this label */
    public static boolean in(@Nonnull final String name) {
        return counters.containsKey(name);
    }

    /** Are we in anything implemented with a Bash Command Substitution? */
    public static boolean inCommandSubstitution() {
        return LevelCounter.in(CALC_LABEL) || LevelCounter.in(LevelCounter.INLINE_LABEL);
    }

    public static int get(@Nonnull final String name) {
        return counters.getOrDefault(name, 0);
    }

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
