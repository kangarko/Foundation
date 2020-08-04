package org.mineacademy.fo.collection;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface StrictSet<E> extends Set<E> {

    void setAll(Iterable<E> collection);

    boolean addAll0(@NotNull Iterable<? extends E> i);
}
