package org.mineacademy.fo.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

/**
 * Strict map that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 */
public final class StrictMap<E, T> extends StrictCollection {

	/**
	 * The internal map holding value-key pairs
	 */
	private final Map<E, T> map = new LinkedHashMap<>();

	/**
	 * Create a new strict map
	 */
	public StrictMap() {
		super("Cannot remove '%s' as it is not in the map!", "Key '%s' is already in the map --> '%s'");
	}

	/**
	 * Create a new strict map with custom already exist/not exists error messages
	 *
	 * @param removeMessage
	 * @param addMessage
	 */
	public StrictMap(String removeMessage, String addMessage) {
		super(removeMessage, addMessage);
	}

	/**
	 * Create a new strict map from the given old map
	 *
	 * @param copyOf
	 */
	public StrictMap(Map<E, T> copyOf) {
		this();

		putAll(copyOf);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger strict checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Remove the first given element from map from value, failing if not exists
	 *
	 * @param value
	 */
	public void removeByValue(T value) {
		for (final Entry<E, T> e : map.entrySet())
			if (e.getValue().equals(value)) {
				map.remove(e.getKey());
				return;
			}

		throw new NullPointerException(String.format(getCannotRemoveMessage(), value));
	}

	/**
	 * Remove all keys failing if one or more are not contained
	 *
	 * @param keys
	 * @return
	 */
	public Object[] removeAll(Collection<E> keys) {
		final List<T> removedKeys = new ArrayList<>();

		for (final E key : keys)
			removedKeys.add(remove(key));

		return removedKeys.toArray();
	}

	/**
	 * Remove the given element from map from key, failing if not exists
	 *
	 * @param key
	 * @return
	 */
	public T remove(E key) {
		final T removed = removeWeak(key);
		Valid.checkNotNull(removed, String.format(getCannotRemoveMessage(), key));

		return removed;
	}

	/**
	 * Put a new pair in the map, failing if key already exists
	 *
	 * @param key
	 * @param value
	 */
	public void put(E key, T value) {
		Valid.checkBoolean(!map.containsKey(key), String.format(getCannotAddMessage(), key, map.get(key)));

		override(key, value);
	}

	/**
	 * Put the given map into this one, failing if a key already exists
	 *
	 * @param m
	 */
	public void putAll(Map<? extends E, ? extends T> m) {
		for (final Map.Entry<? extends E, ? extends T> e : m.entrySet())
			Valid.checkBoolean(!map.containsKey(e.getKey()), String.format(getCannotAddMessage(), e.getKey(), map.get(e.getKey())));

		override(m);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods without throwing errors below
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Remove the given value, or do nothing if not contained
	 *
	 * @param value
	 * @return
	 */
	public T removeWeak(E value) {
		return map.remove(value);
	}

	/**
	 * Put a new pair into the map, overriding old one
	 *
	 * @param key
	 * @param value
	 */
	public void override(E key, T value) {
		map.put(key, value);
	}

	/**
	 * Put new pairs into the map, overriding old one
	 *
	 * @param m
	 */
	public void override(Map<? extends E, ? extends T> m) {
		map.putAll(m);
	}

	/**
	 * Return the key as normal if exists or put it there and return it.
	 *
	 * @param key
	 * @param defaultToPut
	 * @return
	 */
	public T getOrPut(E key, T defaultToPut) {
		if (contains(key))
			return get(key);

		put(key, defaultToPut);
		return defaultToPut;
	}

	/**
	 * Return the first key by value or null if not found
	 *
	 * @param value
	 * @return
	 */
	public E getKeyFromValue(T value) {
		for (final Entry<E, T> e : map.entrySet())
			if (e.getValue().equals(value))
				return e.getKey();

		return null;
	}

	/**
	 * Get the first key or null if none
	 *
	 * @return
	 */
	public E getFirstKey() {
		return map.isEmpty() ? null : map.keySet().iterator().next();
	}

	/**
	 * Return the key from the map, or null if not set
	 */
	public T get(E key) {
		return map.get(key);
	}

	/**
	 * Return the key from the map or the default param if not set
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public T getOrDefault(E key, T def) {
		return map.getOrDefault(key, def);
	}

	/**
	 * Return true if key is not null and contained
	 *
	 * @param key
	 * @return
	 */
	public boolean contains(E key) {
		return key == null ? false : map.containsKey(key);
	}

	/**
	 * Return true if value is not null and contained
	 *
	 * @param value
	 * @return
	 */
	public boolean containsValue(T value) {
		return value == null ? false : map.containsValue(value);
	}

	/**
	 * Do the given action for each pair
	 *
	 * @param consumer
	 */
	public void forEachIterate(BiConsumer<E, T> consumer) {
		for (final Iterator<Map.Entry<E, T>> it = entrySet().iterator(); it.hasNext();) {
			final Map.Entry<E, T> entry = it.next();

			consumer.accept(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Get the map entries
	 *
	 * @return
	 */
	public Set<Entry<E, T>> entrySet() {
		return map.entrySet();
	}

	/**
	 * Get map keys
	 *
	 * @return
	 */
	public Set<E> keySet() {
		return map.keySet();
	}

	/**
	 * Get map values
	 *
	 * @return
	 */
	public Collection<T> values() {
		return map.values();
	}

	/**
	 * Clear the map
	 */
	public void clear() {
		map.clear();
	}

	/**
	 * Return true if map is empty
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Return the original Java map
	 *
	 * @return
	 */
	public Map<E, T> getSource() {
		return map;
	}

	/**
	 * Return the map size
	 *
	 * @return
	 */
	public int size() {
		return map.size();
	}

	@Override
	public Object serialize() {
		if (!map.isEmpty()) {
			final Map<Object, Object> copy = new LinkedHashMap<>();

			for (final Entry<E, T> entry : entrySet()) {
				final T val = entry.getValue();

				if (val != null)
					copy.put(SerializeUtil.serialize(entry.getKey()), SerializeUtil.serialize(val));
			}

			return copy;
		}

		return getSource();
	}

	@Override
	public String toString() {
		return map.toString();
	}
}