package org.mineacademy.fo.collection;

import java.util.List;

public interface StrictList<E> extends List<E>, StrictCollection {

    // ------------------------------------------------------------------------------------------------------------
    // Methods below trigger strict checks
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Clear the list and add all given elements
     *
     * @param elements
     */
    void setAll(Iterable<E> elements);

    /**
     * Return value at the given index and remove it immediatelly
     *
     * @param index
     * @return
     */
    E getAndRemove(int index);

    /**
     * Add the given elements
     *
     * @param elements
     */
    void addAll0(Iterable<E> elements);

    /**
     * Add the element if it does not exist
     *
     * @param key
     */
    void addIfNotExist(E key);

    /**
     * Creates a copy of the list from the starting index
     *
     * @param startIndex
     * @return
     */
    StrictList<E> range(int startIndex);

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

    /**
     * Return the value or the default
     *
     * @param index
     * @param def
     * @return
     */
    E getOrDefault(int index, E def);

    /**
     * Return true if the list contains the key
     * <p>
     * If the key is a string we return true if it equals your key, case ignored,
     * otherwise we just call equals() method normally
     *
     * @param key
     * @return
     */
    default boolean containsIgnoreCase(E key) {
        for (final E other : this) {
            if (other instanceof String && key instanceof String)
                if (((String) other).equalsIgnoreCase((String) key))
                    return true;

            if (other.equals(key))
                return true;
        }

        return false;
    }

    /**
     * Return all list values together split by the given separator
     *
     * @param separator
     * @return
     */
    String join(String separator);
}
