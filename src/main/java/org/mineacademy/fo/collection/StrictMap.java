package org.mineacademy.fo.collection;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Strict map that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 */
public interface StrictMap<E, V>  extends Map<E, V>, StrictDataHolder {

    /**
     * Clear the map and add all given entries
     *
     * @param copyOf
     */
    void setAll(Map<E, V> copyOf);

    void removeByValue(V value);

    E getKeyFromValue(V value);

    V removeWeak(Object key);

    Object[] removeAll(Collection<E> keys);

    /**
     * @deprecated Compatibility method. Please use {@link Map#containsKey(Object)}
     */
    @Deprecated
    default boolean contains(E key) {
        return containsKey(key);
    }

    void override(E key, V value);

    void override(Map<? extends E, ? extends V> m);

    /**
     * Will return the key as normal or put it there and return it.
     */
    V getOrPut(E key, V defaultToPut);

    E firstKey();

    /**
     * @deprecated Please use {@link #forEach(BiConsumer)}
     */
    @Deprecated
    void forEachIterate(BiConsumer<E, V> consumer);
}
