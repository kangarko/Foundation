package org.mineacademy.fo.collection;

import org.mineacademy.fo.Valid;

import java.util.Iterator;

/**
 * A list that automatically calls your code when you add/remove values etc.
 *
 * @param <T>
 */
public final class AutoUpdateList<T> extends StrictCollection implements Iterable<T> {

	/**
	 * The list itself
	 */
	private final StrictList<T> list = new StrictList<>();

	/**
	 * The code that gets triggered
	 */
	private Runnable updater;

	/**
	 * Create a new self-updating list
	 *
	 * @param updater
	 */
	public AutoUpdateList(Runnable updater) {
		super("Cannot remove '%s' as it is not in the list!", "Key '%s' is already in the list --> '%s'");

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
	 * Return the internal not updating list
	 *
	 * @return
	 */
	public StrictList<T> getSource() {
		return list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger updater code when called
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Add the key and update the list
	 *
	 * @param key
	 */
	public void addAndUpdate(T key) {
		list.add(key);

		updater.run();
	}

	/**
	 * Add all keys and call the update code
	 *
	 * @param keys
	 */
	public void addAllAndUpdate(Iterable<T> keys) {
		list.addAll(keys);

		updater.run();
	}

	/**
	 * Remove the value and update
	 *
	 * @param value
	 */
	public void removeAndUpdate(T value) {
		list.remove(value);

		updater.run();
	}

	/**
	 * Remove the value at the given index and update
	 *
	 * @param index
	 * @return
	 */
	public T removeAndUpdate(int index) {
		final T t = list.remove(index);
		updater.run();

		return t;
	}

	public void setAndUpdate(int index, T key) {
		list.set(index, key);

		updater.run();
	}

	/**
	 * Clears the list and updates
	 */
	public void clearAndUpdate() {
		list.clear();

		updater.run();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods that wont update the code and delegate methods
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return value at the given index
	 *
	 * @param index
	 * @return
	 */
	public T get(int index) {
		return list.get(index);
	}

	/**
	 * Return true if the value exists in the list
	 *
	 * @param key
	 * @return
	 */
	public boolean contains(T key) {
		return list.contains(key);
	}

	/**
	 * Return true if the list is empty
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/**
	 * Return the size of this list
	 *
	 * @return
	 */
	public int size() {
		return list.size();
	}

	/**
	 * Serialize this list into something you can store in your settings
	 *
	 * @return
	 */
	@Override
	public Object serialize() {
		return list.serialize();
	}

	/**
	 * Return the classic list iterator
	 *
	 * @return
	 */
	@Override
	public Iterator<T> iterator() {
		return list.iterator();
	}

	/**
	 * Get the string representation of this list
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return list.toString();
	}
}
