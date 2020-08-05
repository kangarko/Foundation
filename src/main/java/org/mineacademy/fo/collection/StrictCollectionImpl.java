package org.mineacademy.fo.collection;

import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

import java.util.Collection;
import java.util.Iterator;

/**
 * Strict Collection (wrapper) which is backed by any given collection.
 * @param <E> The generic type.
 */
class StrictCollectionImpl<E> implements StrictCollection<E> {

  private final Collection<E> c; // Backing collection
  private final String cannotRemoveMessage, cannotAddMessage;

  StrictCollectionImpl(@NotNull final Collection<E> c) {
    this(c, "Cannot remove '%s' as it is not in the list!", "Value '%s' is already in the list!");
  }

  StrictCollectionImpl(@NotNull final Collection<E> c, final String removeMessage, final String addMessage) {
    this.c = c;
    this.cannotRemoveMessage = removeMessage;
    this.cannotAddMessage = addMessage;
  }

  @Override
  public boolean setAll(Iterable<? extends E> elements) {
    clear();
    return addAll0(elements);
  }

  @Override
  public boolean addAll0(Iterable<? extends E> elements) {
    for (final E key : elements)
      add(key);
    return true;
  }

  @Override
  public void addIfNotExist(E key) {
    if (!contains(key)) {
      addWeak(key);
    }
  }

  @Override
  public boolean removeWeak(Object value) {
    return c.remove(value);
  }

  @Override
  public void addWeakAll(Iterable<E> keys) {
    keys.forEach(this::addWeak);
  }

  @Override
  public boolean addWeak(E key) {
    return c.add(key);
  }

  @Override
  public Object serialize() {
    return SerializeUtil.serializeList(c);
  }

  @Override
  public int size() {
    return c.size();
  }

  @Override
  public boolean isEmpty() {
    return c.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return c.contains(o);
  }

  @NotNull
  @Override
  public Iterator<E> iterator() {
    return c.iterator();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    return c.toArray();
  }

  @NotNull
  @Override
  public <T1> T1[] toArray(@NotNull T1[] a) {
    return c.toArray(a);
  }

  @Override
  public boolean add(E key) {
    Valid.checkNotNull(key, "Cannot add null values");
    Valid.checkBoolean(!contains(key), String.format(cannotAddMessage, key));

    return addWeak(key);
  }

  @Override
  public boolean remove(Object o) {
    final boolean removed = removeWeak(o);

    Valid.checkBoolean(removed, String.format(cannotRemoveMessage, o));
    return removed;
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    return this.c.containsAll(c);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends E> c) {
    c.forEach(this::add);
    return true;
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    c.forEach(this::remove);
    return true;
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    Iterator<? extends E> i = this.c.iterator();
    boolean modified = false;
    while (i.hasNext()) {
      if (!c.contains(i.next())) {
        i.remove();
        modified = true;
      }
    }
    return modified;
  }

  @Override
  public void clear() {
    c.clear();
  }
}
