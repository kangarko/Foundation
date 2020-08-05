package org.mineacademy.fo.collection;

import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Strict map implementation which extends LinkedHashMap.
 */
public final class StrictLinkedHashMap<E, V> extends LinkedHashMap<E, V> implements
        StrictMap<E, V> {

  private final String cannotRemoveMessage, cannotAddMessage, cannotAddValue;
  private final Map<V, E> inverse = new LinkedHashMap<>();

  public StrictLinkedHashMap() {
    this("Cannot remove '%s' as it is not in the map!",
            "Key '%s' is already in the map --> '%s'",
            "Value '%s' is already in the map --> '%S'");
  }

  public StrictLinkedHashMap(int initialSize) {
    this("Cannot remove '%s' as it is not in the map!",
            "Key '%s' is already in the map --> '%s'",
            "Value '%s' is already in the map --> '%S'", initialSize);
  }

  public StrictLinkedHashMap(int initialSize, float loadFactor) {
    this("Cannot remove '%s' as it is not in the map!",
            "Key '%s' is already in the map --> '%s'",
            "Value '%s' is already in the map --> '%S'", initialSize, loadFactor);
  }

  public StrictLinkedHashMap(String removeMessage, String addMessage, String addValueMessage, int initialSize, float loadFactor) {
    super(initialSize, loadFactor);
    this.cannotRemoveMessage = removeMessage;
    this.cannotAddMessage = addMessage;
    this.cannotAddValue = addValueMessage;
  }

  public StrictLinkedHashMap(String removeMessage, String addMessage, String addValueMessage, int initialSize) {
    super(initialSize);
    this.cannotRemoveMessage = removeMessage;
    this.cannotAddMessage = addMessage;
    this.cannotAddValue = addValueMessage;
  }


  public StrictLinkedHashMap(String removeMessage, String addMessage, String addValueMessage) {
    this.cannotRemoveMessage = removeMessage;
    this.cannotAddMessage = addMessage;
    this.cannotAddValue = addValueMessage;
  }

  public StrictLinkedHashMap(Map<E, V> copyOf) {
    this();
    setAll(copyOf);
  }

  @Override
  public void setAll(Map<E, V> copyOf) {
    clear();

    for (final Map.Entry<E, V> e : copyOf.entrySet())
      put(e.getKey(), e.getValue());
  }

  @Override
  public V remove(Object key) {
    final V removed = removeWeak(key);
    Valid.checkNotNull(removed, String.format(cannotRemoveMessage, key));

    return removed;
  }

  @Override
  public void removeByValue(V value) {
    E key = inverse.get(value);
    if (key != null) {
      remove(value);
    } else {
      throw new NullPointerException(String.format(cannotRemoveMessage, value));
    }
  }

  @Override
  public E getKeyFromValue(V value) {
    return inverse.get(value);
  }

  @Override
  public V removeWeak(Object key) {
    inverse.values().remove(key);
    return super.remove(key);
  }

  @Override
  public Object[] removeAll(Collection<E> keys) {
    final List<V> removedKeys = new ArrayList<>();

    for (final E key : keys)
      removedKeys.add(remove(key));
    inverse.keySet().removeAll(removedKeys);
    return removedKeys.toArray();
  }

  @Override
  public V put(E key, V value) {
    Valid.checkBoolean(!containsKey(key), String.format(cannotAddMessage, key, get(key)));
    Valid.checkBoolean(!containsValue(key), String.format(cannotAddMessage, value));
    inverse.put(value, key);
    return super.put(key, value);
  }

  @Override
  public void override(E key, V value) {
    super.put(key, value);
  }

  @Override
  public void override(Map<? extends E, ? extends V> m) {
    super.putAll(m);
  }

  public boolean contains(E key) {
    return containsKey(key);
  }

  @Override
  public Set<E> keySet() {
    return new StrictKeySet<>(super.keySet(), cannotRemoveMessage);
  }

  @Override
  public Set<Entry<E, V>> entrySet() {
    return new StrictEntrySet<>(super.entrySet(), cannotRemoveMessage, cannotAddValue);
  }

  public void putAll(Map<? extends E, ? extends V> m) {
    for (final Map.Entry<? extends E, ? extends V> e : m.entrySet())
      Valid.checkBoolean(!containsKey(e.getKey()), String.format(cannotAddMessage, e.getKey(), get(e.getKey())));
    super.putAll(m);
    for (final Map.Entry<? extends E, ? extends V> e : m.entrySet())
      inverse.put(e.getValue(), e.getKey());
  }

  @Override
  public V getOrPut(E key, V defaultToPut) {
    if (containsKey(key))
      return get(key);

    put(key, defaultToPut);
    return defaultToPut;
  }

  /**
   * @deprecated As this map now extends {@link LinkedHashMap}
   */
  @Deprecated
  public Map<E, V> getSource() {
    return this;
  }

  @Override
  public E firstKey() {
    return isEmpty() ? null : super.keySet().iterator().next();
  }

  @Override
  @Deprecated
  public void forEachIterate(BiConsumer<E, V> consumer) {
    this.forEach(consumer);
  }


  @Override
  public Object serialize() {
    if (!isEmpty()) {
      final Map<Object, Object> copy = new HashMap<>();

      for (final Entry<E, V> e : entrySet()) {
        final V val = e.getValue();

        if (val != null)
          copy.put(SerializeUtil.serialize(e.getKey()), SerializeUtil.serialize(val));
      }

      return copy;
    }

    return this;
  }

  static class StrictEntrySet<E, V> extends AbstractSet<Map.Entry<E, V>> implements StrictSet<Map.Entry<E, V>> {

    private final Set<Map.Entry<E, V>> entries;
    private final String cannotRemoveMessage;
    private final String cannotAddMessage;

    StrictEntrySet(Set<Map.Entry<E, V>> entries, String cannotRemoveMessage, String cannotAddValueMessage) {
      this.entries = entries;
      this.cannotRemoveMessage = cannotRemoveMessage;
      this.cannotAddMessage = cannotAddValueMessage;
    }

    @Override
    @NotNull
    public Iterator<Entry<E, V>> iterator() {
      return entries.iterator();
    }

    @Override
    public boolean setAll(Iterable<? extends Entry<E, V>> elements) {
      throw new UnsupportedOperationException("Operation unsupported by this collection.");
    }

    @Override
    public void addIfNotExist(Entry<E, V> key) {
      throw new UnsupportedOperationException("Operation unsupported by this collection.");
    }

    @Override
    public boolean removeWeak(Object value) {
      return entries.remove(value);
    }

    @Override
    public void addWeakAll(Iterable<Entry<E, V>> keys) {
      throw new UnsupportedOperationException("Operation unsupported by this collection.");
    }

    @Override
    public boolean addWeak(Entry<E, V> key) {
      throw new UnsupportedOperationException("Operation unsupported by this collection.");
    }

    @Override
    public boolean remove(Object o) {
      Valid.checkBoolean(contains(o), String.format(cannotRemoveMessage, o));
      return entries.remove(o);
    }

    @Override
    public int size() {
      return entries.size();
    }

    @Override
    public Object serialize() {
      if (size() == 0) {
        return this;
      }
      return entries.stream().map(SerializeUtil::serialize).collect(Collectors.toCollection(HashSet::new));
    }

    class StrictEntry<K, V> implements Map.Entry<K, V> {

      private final Map.Entry<K, V> original;
      private final Collection<Map.Entry<K, V>> entries;

      public StrictEntry(Map.Entry<K, V> entry, Set<Map.Entry<K, V>> entries) {
        this.original = entry;
        this.entries = entries;
      }

      @Override
      public K getKey() {
        return original.getKey();
      }

      @Override
      public V getValue() {
        return original.getValue();
      }

      @Override
      public V setValue(V value) {
        Valid.checkBoolean(entries.stream().noneMatch(entry -> value.equals(entry.getValue())), String.format(cannotAddMessage, value, original.getKey()));
        return original.setValue(value);
      }
    }
  }


  static class StrictKeySet<K> extends AbstractSet<K> implements StrictSet<K> {

    private final Set<K> set;
    private final String cannotRemoveMessage;

    StrictKeySet(Set<K> set, String cannotRemoveMessage) {
      this.set = set;
      this.cannotRemoveMessage = cannotRemoveMessage;
    }

    @Override
    public boolean removeWeak(Object value) {
      return super.remove(value);
    }

    @Override
    @NotNull
    public Iterator<K> iterator() {
      return set.iterator();
    }

    @Override
    public boolean remove(final Object value) {
      if (!contains(value)) {
        throw new NullPointerException(String.format(cannotRemoveMessage, value));
      }
      return super.remove(value);
    }

    @Override
    public int size() {
      return set.size();
    }

    @Override
    public Object serialize() {
      if (size() == 0) {
        return this;
      }
      return set.stream().map(SerializeUtil::serialize).collect(Collectors.toCollection(HashSet::new));
    }


    @Override
    public boolean setAll(Iterable<? extends K> elements) {
      throw new UnsupportedOperationException("Add operation is unsupported on this set!");
    }

    @Override
    public boolean addAll0(@NotNull Iterable<? extends K> i) {
      throw new UnsupportedOperationException("Add operation is unsupported on this set!");
    }

    @Override
    public boolean removeAll0(Iterable<? extends K> elements) {
      elements.forEach(this::remove);
      return true;

    }

    @Override
    public void addIfNotExist(K key) {
      throw new UnsupportedOperationException("Add operation is unsupported on this set!");
    }

    @Override
    public void addWeakAll(Iterable<K> keys) {
      throw new UnsupportedOperationException("Add operation is unsupported on this set!");
    }

    @Override
    public boolean addWeak(K key) {
      throw new UnsupportedOperationException("Add operation is unsupported on this set!");
    }
  }
}
