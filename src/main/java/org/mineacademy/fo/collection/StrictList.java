package org.mineacademy.fo.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

/**
 * Strict list that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 * @param <E>
 */
public final class StrictList<E> extends StrictCollection implements Iterable<E> {

	/**
	 * The internal list
	 */
	private final List<E> list = new ArrayList<>();

	/**
	 * Create a new list of the given elements
	 *
	 * @param elements
	 */
	@SafeVarargs
	public StrictList(E... elements) {
		this();

		this.addAll(Arrays.asList(elements));
	}

	/**
	 * Create a new list of the given elements
	 *
	 * @param oldList
	 */
	public StrictList(Iterable<E> oldList) {
		this();

		this.addAll(oldList);
	}

	/**
	 * Create a new empty list
	 */
	public StrictList() {
		super("Cannot remove '%s' as it is not in the list!", "Value '%s' is already in the list!");
	}

	/**
	 * Return the default Java {@link ArrayList}
	 *
	 * @return
	 */
	public List<E> getSource() {
		return this.list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger strict checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return value at the given index and remove it immediatelly
	 *
	 * @param index
	 * @return
	 */
	public E getAndRemove(int index) {
		final E e = this.list.get(index);
		this.remove(index);

		return e;
	}

	/**
	 * Remove the given key
	 *
	 * @param key
	 */
	public void remove(E key) {
		final boolean removed = this.removeWeak(key);

		Valid.checkBoolean(removed, String.format(this.getCannotRemoveMessage(), key));
	}

	/**
	 * Remove the key at the given index
	 *
	 * @param index
	 * @return
	 */
	public E remove(int index) {
		final E removed = this.list.remove(index);

		Valid.checkNotNull(removed, String.format(this.getCannotRemoveMessage(), "index: " + index));
		return removed;
	}

	/**
	 * Add the given elements
	 *
	 * @param elements
	 */
	public void addAll(Iterable<E> elements) {
		for (final E key : elements)
			this.add(key);
	}

	/**
	 * Add the element if it does not exist
	 *
	 * @param key
	 */
	public void addIfNotExist(E key) {
		if (!this.contains(key))
			this.add(key);
	}

	/**
	 * Add the element to the list
	 *
	 * @param key
	 */
	public void add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!this.list.contains(key), String.format(this.getCannotAddMessage(), key));

		this.addWeak(key);
	}

	/**
	 * Creates a copy of the list from the starting index
	 *
	 * @param startIndex
	 * @return
	 */
	public StrictList<E> range(int startIndex) {
		Valid.checkBoolean(startIndex <= this.list.size(), "Start index out of range " + startIndex + " vs. list size " + this.list.size());
		final StrictList<E> ranged = new StrictList<>();

		for (int i = startIndex; i < this.list.size(); i++)
			ranged.add(this.list.get(i));

		return ranged;
	}

	/**
	 * Returns the first value or null if the list is empty
	 *
	 * @return
	 */
	@Nullable
	public E first() {
		return this.list.isEmpty() ? null : this.list.get(0);
	}

	/**
	 * Returns the last value or null if the list is empty
	 *
	 * @return
	 */
	@Nullable
	public E last() {
		return this.list.isEmpty() ? null : this.list.get(this.list.size() - 1);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods without throwing errors below
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Remove the given key without throwing error if it does note exist
	 *
	 * @param value
	 * @return
	 */
	public boolean removeWeak(E value) {
		Valid.checkNotNull(value, "Cannot remove null values");

		return this.list.remove(value);
	}

	/**
	 * Add all keys even if they exist
	 *
	 * @param keys
	 */
	public void addWeakAll(Iterable<E> keys) {
		for (final E key : keys)
			this.addWeak(key);
	}

	/**
	 * Add the given key at the end of the list regardless if it already exist
	 *
	 * @param key
	 */
	public void addWeak(E key) {
		this.list.add(key);
	}

	/**
	 * Set a key to the certain index
	 *
	 * @param index
	 * @param key
	 */
	public void set(int index, E key) {
		this.list.set(index, key);
	}

	/**
	 * Return the value or the default
	 *
	 * @param index
	 * @param def
	 * @return
	 */
	public E getOrDefault(int index, E def) {
		return index < this.list.size() ? this.list.get(index) : def;
	}

	/**
	 * Return the value at given index
	 *
	 * @param index
	 * @return
	 */
	public E get(int index) {
		return this.list.get(index);
	}

	/**
	 * Return true if the list contains the key
	 *
	 * If the key is string we return true if it contains ignore case
	 *
	 * @param key
	 * @return
	 */
	public boolean contains(E key) {
		for (final E other : this.list) {
			if (other instanceof String && key instanceof String)
				if (((String) other).equalsIgnoreCase((String) key))
					return true;

			if (other.equals(key))
				return true;
		}

		return false;
	}

	/**
	 * Returns a view of the portion of this list between the specified fromIndex,
	 * inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal,
	 * the returned list is empty.) The returned list is backed by this list,
	 * so non-structural changes in the returned list are reflected in this list,
	 * and vice-versa.The returned list supports all of the optional list operations
	 * supported by this list.
	 *
	 * @param fromIndex
	 * @param toIndex
	 * @return
	 */
	public List<E> subList(int fromIndex, int toIndex) {
		return this.list.subList(fromIndex, toIndex);
	}

	/**
	 * Remove every single piece of that list!
	 */
	public void clear() {
		this.list.clear();
	}

	/**
	 * Return true if the list is empty
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	/**
	 * Get the list size
	 *
	 * @return
	 */
	public int size() {
		return this.list.size();
	}

	/**
	 * Return all list values together split by the given separator
	 *
	 * @param separator
	 * @return
	 */
	public String join(String separator) {
		return Common.join(this.list, separator);
	}

	/**
	 * See {@link List#toArray()}
	 *
	 * @param e
	 * @return
	 */
	public E[] toArray(E[] e) {
		return this.list.toArray(e);
	}

	/**
	 * Return {@link List#toArray()}
	 *
	 * @return
	 */
	public Object[] toArray() {
		return this.list.toArray();
	}

	/**
	 * See {@link List#iterator()}
	 */
	@Override
	public Iterator<E> iterator() {
		return this.list.iterator();
	}

	/**
	 * Serializes every value in the list so you can store it in your settings
	 */
	@Override
	public Object serialize() {
		return SerializeUtil.serialize(this.list);
	}

	/**
	 * Returns string representation of this list
	 * <p>
	 * NB: For saving in files call {@link #serialize()}
	 */
	@Override
	public String toString() {
		return this.list.toString();
	}
}