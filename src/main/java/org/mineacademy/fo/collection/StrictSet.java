package org.mineacademy.fo.collection;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Strict set that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 */
public interface StrictSet<E> extends Set<E>, StrictCollection {


    /**
     * Clear the list and add all given elements
     *
     * @param elements
     */
    void setAll(Iterable<E> elements);


    boolean addAll0(@NotNull Iterable<? extends E> i);
}
