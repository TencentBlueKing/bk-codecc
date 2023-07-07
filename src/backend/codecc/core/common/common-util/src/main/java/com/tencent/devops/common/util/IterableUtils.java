package com.tencent.devops.common.util;

import com.google.common.collect.Iterables;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class IterableUtils {

    public static <T> @Nullable T getFirst(Iterable<? extends T> iterable, @Nullable T defaultValue) {
        if (iterable == null) {
            return defaultValue;
        }

        return Iterables.getFirst(iterable, defaultValue);
    }
}
