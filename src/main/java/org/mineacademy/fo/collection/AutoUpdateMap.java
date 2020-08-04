package org.mineacademy.fo.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import org.mineacademy.fo.Valid;

/**
 * A map that automatically calls your code when you add/remove values etc.
 *
 * @param <V>
 * @param <K>
 */
public final class AutoUpdateMap<V, K> extends AbstractStrictCollection {

	/**
	 * The internal map
	 */
	private final StrictMap<V, K> map = new StrictMap<>();

	/**
	 * The code that gets triggered
	 */
	private Runnable updater;

	/**
	 * Create a new self-updating map
	 *
	 * @param updater
	 */
	public AutoUpdateMap(Runnable updater) {
		super("Cannot remove '%s' as it is not in the map!", "Key '%s' is already in the map --> '%s'");

		this.updater = updater;
	}

	/**
	 * Set new update code
	 *
	 * @param updater
	 */
	public void setUpdater(Runnable updater) {
		Valid.checkNotNull(updater, "Updater cannot be null");

		this.updater = updater;
	}

	/**
	 * Execute the update code;
	 */
	public void update() {
		updater.run();
	}

	/**
	 * Get the original not updating map
	 *
	 * @return
	 */
	public StrictMap<V, K> getSource() {
		return map;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger updater code when called
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Puts the key-value pair into the map and updates
	 * <p>
	 * Calls an exception if the key already exists
	 *
	 * @param key
	 * @param value
	 */
	public void putAndUpdate(V key, K value) {
		map.put(key, value);

		updater.run();
	}

	/**
	 * Puts the key-value pair into the map and updates
	 * <p>
	 * If it already contains the key, it is overriden
	 *
	 * @param key
	 * @param value
	 */
	public void overrideAndUpdate(V key, K value) {
		map.removeWeak(key);
		map.put(key, value);

		updater.run();
	}

	/**
	 * Removes the key-value pair by key and updates
	 *
	 * @param key
	 * @return
	 */
	public K removeAndUpdate(V key) {
		final K k = map.remove(key);
		updater.run();

		return k;
	}

	/**
	 * Removes the key-value pair by value and updates
	 *
	 * @param value
	 */
	public void removeByValueAndUpdate(K value) {
		map.removeByValue(value);

		updater.run();
	}

	/**
	 * Removes all the given key pairs and updates
	 *
	 * @param keys
	 * @return
	 */
	public Object[] removeAllAndUpdate(Collection<V> keys) {
		final Object[] obj = map.removeAll(keys);

		updater.run();
		return obj;
	}

	/**
	 * Clears the map and updates
	 */
	public void clearAndUpdate() {
		map.clear();

		updater.run();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods that wont update the code and delegate methods
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the value from the given key
	 *
	 * @param key
	 * @return
	 */
	public K get(V key) {
		return map.get(key);
	}

	/**
	 * Return the value from the given key or the default if it does not exist
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public K getOrDefault(V key, K def) {
		return map.getOrDefault(key, def);
	}

	/**
	 * Returns the key from the first matching value
	 *
	 * @param value
	 * @return
	 */
	public V getKeyFromValue(K value) {
		return map.getKeyFromValue(value);
	}

	/**
	 * Returns true if the map contains the given key
	 *
	 * @param key
	 * @return
	 */
	public boolean contains(V key) {
		return map.contains(key);
	}

	/**
	 * Returns true if the map contains the given value
	 *
	 * @param value
	 * @return
	 */
	public boolean containsValue(K value) {
		return map.containsValue(value);
	}

	/**
	 * Return the classic entry set for map, not updating & read-only
	 *
	 * @return
	 */
	public Set<Entry<V, K>> entrySet() {
		return Collections.unmodifiableSet(map.entrySet());
	}

	/**
	 * Return the classic key set for map, not updating & read-only
	 *
	 * @return
	 */
	public Set<V> keySet() {
		return Collections.unmodifiableSet(map.keySet());
	}

	/**
	 * Return the classic value collection for map, not updating & read-only
	 *
	 * @return
	 */
	public Collection<K> values() {
		return Collections.unmodifiableCollection(map.values());
	}

	/**
	 * Return true if the map is empty
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Get the size of the map
	 *
	 * @return
	 */
	public int size() {
		return map.size();
	}

	/**
	 * Serializes the map so you can store it as a settings file
	 *
	 * @return
	 */
	@Override
	public Object serialize() {
		return map.serialize();
	}

	/**
	 * Returns the map as a string (if you want to save this into file call {@link #serialize()})
	 */
	@Override
	public String toString() {
		return map.toString();
	}
}
