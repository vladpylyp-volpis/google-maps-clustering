package com.volpis.googleclusterization;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.List;

final class Preconditions {

    private Preconditions() {
    }

    static <T> T checkNotNull(@Nullable T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    static <T> boolean checkListsEqual(List<T> items, List<T> otherItems) {
        return new HashSet<>(items).equals(new HashSet<>(otherItems));
    }
}