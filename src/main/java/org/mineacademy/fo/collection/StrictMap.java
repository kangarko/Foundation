package org.mineacademy.fo.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

/**
 * Strict map that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 * @param <K>
 * @param <V>
 */
public final class StrictMap<K, V> extends StrictCollection {

	/**
	 * The internal map holding value-key pairs
	 */
	private final Map<K, V> map = new LinkedHashMap<>();

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
	public StrictMap(Map<K, V> copyOf) {
		this();

		this.putAll(copyOf);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger strict checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Remove the first given element from map from value, failing if not exists
	 *
	 * @param value
	 */
	public void removeByValue(V value) {
		for (final Entry<K, V> e : this.map.entrySet())
			if (e.getValue().equals(value)) {
				this.map.remove(e.getKey());
				return;
			}

		throw new NullPointerException(String.format(this.getCannotRemoveMessage(), value));
	}

	/**
	 * Remove all keys failing if one or more are not contained
	 *
	 * @param keys
	 * @return
	 */
	public Object[] removeAll(Collection<K> keys) {
		final List<V> removedKeys = new ArrayList<>();

		for (final K key : keys)
			removedKeys.add(this.remove(key));

		return removedKeys.toArray();
	}

	/**
	 * Remove the given element from map from key, failing if not exists
	 *
	 * @param key
	 * @return
	 */
	public V remove(K key) {
		final V removed = this.removeWeak(key);
		Valid.checkNotNull(removed, String.format(this.getCannotRemoveMessage(), key));

		return removed;
	}

	/**
	 * Put a new pair in the map, failing if key already exists
	 *
	 * @param key
	 * @param value
	 */
	public void put(K key, V value) {
		Valid.checkBoolean(!this.map.containsKey(key), String.format(this.getCannotAddMessage(), key, this.map.get(key)));

		this.override(key, value);
	}

	/**
	 * Put the given map into this one, failing if a key already exists
	 *
	 * @param m
	 */
	public void putAll(Map<? extends K, ? extends V> m) {
		for (final Map.Entry<? extends K, ? extends V> e : m.entrySet())
			Valid.checkBoolean(!this.map.containsKey(e.getKey()), String.format(this.getCannotAddMessage(), e.getKey(), this.map.get(e.getKey())));

		this.override(m);
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
	public V removeWeak(K value) {
		return this.map.remove(value);
	}

	/**
	 * Put a new pair into the map, overriding old one
	 *
	 * @param key
	 * @param value
	 */
	public void override(K key, V value) {
		this.map.put(key, value);
	}

	/**
	 * Put new pairs into the map, overriding old one
	 *
	 * @param m
	 */
	public void override(Map<? extends K, ? extends V> m) {
		this.map.putAll(m);
	}

	/**
	 * Return the key as normal if exists or put it there and return it.
	 *
	 * @param key
	 * @param defaultToPut
	 * @return
	 */
	public V getOrPut(K key, V defaultToPut) {
		if (this.containsKey(key))
			return this.get(key);

		this.put(key, defaultToPut);
		return defaultToPut;
	}

	/**
	 * Return the first key by value or null if not found
	 *
	 * @param value
	 * @return
	 */
	public K getKeyFromValue(V value) {
		for (final Entry<K, V> e : this.map.entrySet())
			if (e.getValue().equals(value))
				return e.getKey();

		return null;
	}

	/**
	 * Return the key from the map, or null if not set
	 *
	 * @param key
	 * @return
	 */
	public V get(K key) {
		return this.map.get(key);
	}

	/**
	 * Return the key from the map or the default param if not set
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public V getOrDefault(K key, V def) {
		return this.map.getOrDefault(key, def);
	}

	/**
	 * Returns the first key value from the first pair in map or null if the map is empty
	 *
	 * @return
	 */
	@Nullable
	public K firstKey() {
		return this.map.isEmpty() ? null : this.map.keySet().iterator().next();
	}

	/**
	 * Returns the first value from the first pair in the map or null if the map is empty
	 *
	 * @return
	 */
	@Nullable
	public V firstValue() {
		return this.map.isEmpty() ? null : this.map.values().iterator().next();
	}

	/**
	 * Return true if key is not null and contained
	 *
	 * @param key
	 * @return
	 */
	public boolean containsKey(K key) {
		return key == null ? false : this.map.containsKey(key);
	}

	/**
	 * Return true if value is not null and contained
	 *
	 * @param value
	 * @return
	 */
	public boolean containsValue(V value) {
		return value == null ? false : this.map.containsValue(value);
	}

	/**
	 * Do the given action for each pair
	 *
	 * @param consumer
	 */
	public void forEachIterate(BiConsumer<K, V> consumer) {
		for (Entry<K, V> entry : this.entrySet()) {
			consumer.accept(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Get the map entries
	 *
	 * @return
	 */
	public Set<Entry<K, V>> entrySet() {
		return this.map.entrySet();
	}

	/**
	 * Get map keys
	 *
	 * @return
	 */
	public Set<K> keySet() {
		return this.map.keySet();
	}

	/**
	 * Get map values
	 *
	 * @return
	 */
	public Collection<V> values() {
		return this.map.values();
	}

	/**
	 * Clear the map
	 */
	public void clear() {
		this.map.clear();
	}

	/**
	 * Return true if map is empty
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	/**
	 * Return the original Java map
	 *
	 * @return
	 */
	public Map<K, V> getSource() {
		return this.map;
	}

	/**
	 * Return the map size
	 *
	 * @return
	 */
	public int size() {
		return this.map.size();
	}

	@Override
	public Object serialize() {
		if (!this.map.isEmpty()) {
			final Map<Object, Object> copy = new LinkedHashMap<>();

			for (final Entry<K, V> entry : this.entrySet()) {
				final V val = entry.getValue();

				if (val != null)
					copy.put(SerializeUtil.serialize(entry.getKey()), SerializeUtil.serialize(val));
			}

			return copy;
		}

		return this.getSource();
	}

	@Override
	public String toString() {
		return this.map.toString();
	}
}