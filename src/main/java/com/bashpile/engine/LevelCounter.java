package com.bashpile.engine;

import java.io.Closeable;
import java.util.HashMap;

public class LevelCounter implements Closeable {

    private static final HashMap<String, Integer> counters = HashMap.newHashMap(20);

    private final String name;

    public LevelCounter(String name) {
        this.name = name;
        if (counters.containsKey(name)) {
            // increment
            counters.put(name, counters.get(name) + 1);
        } else {
            counters.put(name, 1);
        }

    }

    public void noop() {}

    public static int getIndent() {
        return counters.getOrDefault("block", 0);
    }

    public static int getIndentMinusOne() {
        return Math.max(getIndent() - 1, 0);
    }

    public static boolean in(String name) {
        return counters.containsKey(name);
    }

    @Override
    public void close() {
        int count = counters.get(name);
        count--;
        if (count <= 0) {
            counters.remove(name);
        } else {
            counters.put(name, count);
        }
    }
}
