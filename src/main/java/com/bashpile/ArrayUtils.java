package com.bashpile;

public class ArrayUtils extends org.apache.commons.lang3.ArrayUtils {

    @SafeVarargs
    public static <T> T[] of(T... str) {
        return str;
    }
}
