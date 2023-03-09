package com.bashpile;

public class ArrayUtils extends org.apache.commons.lang3.ArrayUtils {

    // can't use generics due to type erasure -- unsafe
    public static String[] of(String... str) {
        return str;
    }
}
