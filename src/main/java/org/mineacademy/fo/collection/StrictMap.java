package org.mineacademy.fo.collection;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

public interface StrictMap<E, V> extends Map<E, V>, StrictCollection {

    void setAll(Map<E, V> copyOf);

    void removeByValue(V value);

    E getKeyFromValue(V value);

    V removeWeak(Object key);

    Object[] removeAll(Collection<E> keys);

    @Deprecated
    default boolean contains(E key) {
        return containsKey(key);
    }

    void override(E key, V value);

    void override(Map<? extends E, ? extends V> m);

    V getOrPut(E key, V defaultToPut);

    @Deprecated Map<E, V> getSource();

    E firstKey();

    void forEachIterate(BiConsumer<E, V> consumer);
}
