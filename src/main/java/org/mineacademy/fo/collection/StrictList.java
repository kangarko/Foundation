package org.mineacademy.fo.collection;

import java.util.List;

public interface StrictList<E> extends List<E>, StrictCollection {

    void setAll(Iterable<E> elements);

    E getAndRemove(int index);

    void addAll0(Iterable<E> elements);

    StrictList<E> range(int startIndex);

    boolean removeWeak(Object value);

    void addWeakAll(Iterable<E> keys);

    boolean addWeak(E key);

    E getOrDefault(int index, E def);

    boolean containsIgnoreCase(E key);

    String join(String separator);
}
