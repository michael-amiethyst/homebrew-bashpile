package com.bashpile;

import javax.annotation.Nonnull;
import java.util.List;

public class ListUtils {

    public static <T> @Nonnull T getLast(@Nonnull final List<T> list) {
        return list.get(list.size() - 1);
    }
}
