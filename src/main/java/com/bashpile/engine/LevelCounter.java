package com.bashpile.engine;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.util.HashMap;

/** All instances of the same label share static data */
public class LevelCounter implements Closeable {

    /** LevelCounter label */
    public static final String BLOCK = "block";

    /** LevelCounter label */
    public static final String CALC = "calc";

    /** LevelCounter label */
    public static final String FORWARD_DECL = "forwardDecl";

    private static final HashMap<String, Integer> counters = HashMap.newHashMap(20);

    /**
     * Returns our current indention level.
     *
     * @return whole numbers (integers always positive or 0)
     */
    public static int getIndent() {
        return counters.getOrDefault(BLOCK, 0);
    }

    /** are we in any level of indention for this label */
    public static boolean in(final String name) {
        return counters.containsKey(name);
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
