package org.mineacademy.fo.collection;

import java.util.Set;

/**
 * Strict set that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 */
public interface StrictSet<E> extends Set<E>, StrictCollection<E> {

}
