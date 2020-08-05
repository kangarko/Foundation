package org.mineacademy.fo.collection;

import org.mineacademy.fo.Valid;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Strict collection does not allow adding duplicate elements,
 * and throw an error when you attempt to remove a non-existing element from
 * list/map.
 */
public interface StrictCollection<E> extends Collection<E>, StrictDataHolder {

  // ------------------------------------------------------------------------------------------------------------
  // Methods below trigger strict checks
  // ------------------------------------------------------------------------------------------------------------

  /**
   * Clear the list and add all given elements
   *
   * @param elements
   */
  boolean setAll(Iterable<? extends E> elements);

  /**
   * Add the given elements
   *
   * @param elements
   */
  default boolean addAll0(Iterable<? extends E> elements) {
    elements.forEach(this::add);
    return true;
  }

  /**
   * Remove the given elements
   */
  default boolean removeAll0(Iterable<? extends E> elements) {
    elements.forEach(this::remove);
    return true;
  }

  /**
   * Retain the given elements and remove the rest.
   *
   */
  default boolean retainAll0(Iterable<? extends E> elements) {
    return retainAll(StreamSupport.stream(elements.spliterator(), false).collect(Collectors.toSet()));
  }

  /**
   * Add the element if it does not exist
   *
   * @param key
   */
  void addIfNotExist(E key);

  // ------------------------------------------------------------------------------------------------------------
  // Methods without throwing errors below
  // ------------------------------------------------------------------------------------------------------------


  /**
   * Remove the given key without throwing error if it does not exist
   *
   * @param value
   * @return
   */
  boolean removeWeak(Object value);

  /**
   * Add all keys even if they exist
   *
   * @param keys
   */
  void addWeakAll(Iterable<E> keys);

  /**
   * Add the given key at the end of the list regardless if it already exist
   *
   * @param key
   */
  boolean addWeak(E key);

}
