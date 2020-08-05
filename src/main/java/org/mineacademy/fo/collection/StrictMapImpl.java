package org.mineacademy.fo.collection;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.Valid;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * StrictMap implementation (wrapper) backed by a given map.
 * @param <K> The generic type of the key.
 * @param <V> The generic type of value.
 */
public class StrictMapImpl<K, V> extends AbstractStrictDataHolder implements StrictMap<K, V> {

  @Getter(AccessLevel.PRIVATE)
  private final String cannotAddValueMessage;
  private final Map<K, V> m; // Backing Map
  private final Map<V, K> inverse;

  StrictMapImpl(Map<K, V> m) {
    this(m, "Cannot remove '%s' as it is not in the map!", "Key '%s' is already in the map --> '%s'", "Value '%s' is already in the map --> '%S'" );
  }

  StrictMapImpl(Map<K, V> m, String cannotRemove, String cannotAdd, String cannotAddValue) {
    super(cannotAdd, cannotRemove);
    this.cannotAddValueMessage = cannotAddValue;
    this.m = m;
    this.inverse = new StrictLinkedHashMap<>(m.size());
    for (Map.Entry<K, V> entry : m.entrySet()) {
      inverse.put(entry.getValue(), entry.getKey());
    }
  }

  @Override
  public void setAll(Map<K, V> copyOf) {
    clear();

    for (final Map.Entry<K, V> e : copyOf.entrySet())
      put(e.getKey(), e.getValue());
  }

  @Override
  public void removeByValue(V value) {
    K k = inverse.get(value);
    Valid.checkNotNull(k, String.format(getCannotRemoveMessage(), value));
    inverse.remove(value);
    m.remove(k);
  }

  @Override
  public K getKeyFromValue(V value) {
    return inverse.get(value);
  }

  @Override
  public V removeWeak(Object key) {
    V value = m.remove(key);
    if (value != null) {
      inverse.remove(value);
    }
    return value;
  }

  @Override
  public Object[] removeAll(Collection<K> keys) {
    final List<K> removedKeys = new LinkedList<>();
    for (K k : keys) {
      V v = remove(k);
      if (v != null) {
        inverse.remove(v);
        removedKeys.add(k);
      }
    }
    return removedKeys.toArray();
  }

  @Override
  public void override(K key, V value) {
    m.put(key, value);
    inverse.put(value, key);
  }

  @Override
  public void override(Map<? extends K, ? extends V> m) {
    this.m.putAll(m);
    for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
      this.inverse.put(entry.getValue(), entry.getKey());
    }
  }

  @Override
  public V getOrPut(K key, V defaultToPut) {
    if (containsKey(key)) {
      return get(key);
    }
    put(key, defaultToPut);
    return defaultToPut;
  }

  @Override
  public K firstKey() {
    if (isEmpty()) {
      return null;
    }
    return keySet().iterator().next();
  }

  @Override
  public void forEachIterate(BiConsumer<K, V> consumer) {
    this.forEach(consumer);
  }

  @Override
  public int size() {
    return m.size();
  }

  @Override
  public boolean isEmpty() {
    return m.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return m.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return inverse.containsKey(value);
  }

  @Override
  public V get(Object key) {
    return this.m.get(key);
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    Valid.checkBoolean(!containsKey(key), String.format(getCannotAddMessage(), key, get(key)));
    Valid.checkBoolean(!containsValue(value), String.format(getCannotAddValueMessage(), value, getKeyFromValue(value)));
    override(key, value);
    return null;
  }

  @Override
  public V remove(Object key) {
    Valid.checkBoolean(containsKey(key), String.format(getCannotRemoveMessage(), key));
    V v = m.remove(key);
    inverse.remove(v);
    return v;
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    this.m.clear();
    this.inverse.clear();
  }

  @NotNull
  @Override
  public Set<K> keySet() {
    return new StrictLinkedHashMap.StrictKeySet<>(m.keySet(), getCannotRemoveMessage());
  }

  @NotNull
  @Override
  public Collection<V> values() {
    return m.values();
  }

  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return new StrictLinkedHashMap.StrictEntrySet<>(m.entrySet(), getCannotRemoveMessage(), getCannotAddValueMessage());
  }

  @Override
  public Object serialize() {
    return null;
  }
}
