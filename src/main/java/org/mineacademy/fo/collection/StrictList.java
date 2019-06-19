package org.mineacademy.fo.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

/**
 * Strict list that only allows to remove elements that are contained within, or add elements that are not.
 *
 * Failing to do so results in an error, with optional error message.
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

		setAll(Arrays.asList(elements));
	}

	/**
	 * Create a new list of the given elements
	 *
	 * @param oldList
	 */
	public StrictList(Iterable<E> oldList) {
		this();

		setAll(oldList);
	}

	/**
	 * Create a new empty list
	 */
	public StrictList() {
		this("Cannot remove '%s' as it is not in the list!", "Value '%s' is already in the list!");
	}

	/**
	 * Create a new empty list with a custom remove/add message that are thrown as errors
	 * if the values already exist or not exist
	 *
	 * @param removeMessage
	 * @param addMessage
	 */
	public StrictList(String removeMessage, String addMessage) {
		super(removeMessage, addMessage);
	}

	/**
	 * Return the default Java {@link ArrayList}
	 *
	 * @return
	 */
	public List<E> getSource() {
		return list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger strict checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Clear the list and all all given elements
	 *
	 * @param elements
	 */
	public void setAll(Iterable<E> elements) {
		clear();
		addAll(elements);
	}

	/**
	 * Return value at the given index and remove it immediatelly
	 *
	 * @param index
	 * @return
	 */
	public E getAndRemove(int index) {
		final E e = list.get(index);
		remove(index);

		return e;
	}

	/**
	 * Remove the given key
	 *
	 * @param key
	 */
	public void remove(E key) {
		final boolean removed = removeWeak(key);

		Valid.checkBoolean(removed, String.format(getCannotRemoveMessage(), key));
	}

	/**
	 * Remove the key at the given index
	 *
	 * @param index
	 * @return
	 */
	public E remove(int index) {
		final E removed = list.remove(index);

		Valid.checkNotNull(removed, String.format(getCannotRemoveMessage(), "index: " + index));
		return removed;
	}

	/**
	 * Add the given elements
	 *
	 * @param elements
	 */
	public void addAll(Iterable<E> elements) {
		for (final E key : elements)
			add(key);
	}

	/**
	 * Add the element if it does not exist
	 *
	 * @param key
	 */
	public void addIfNotExist(E key) {
		if (!contains(key))
			add(key);
	}

	/**
	 * Add the element to the list
	 *
	 * @param key
	 */
	public void add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!list.contains(key), String.format(getCannotAddMessage(), key));

		addWeak(key);
	}

	/**
	 * Creates a copy of the list from the starting index
	 *
	 * @param startIndex
	 * @return
	 */
	public StrictList<E> range(int startIndex) {
		Valid.checkBoolean(startIndex <= list.size(), "Start index out of range " + startIndex + " vs. list size " + list.size());
		final StrictList<E> ranged = new StrictList<>();

		for (int i = startIndex; i < list.size(); i++)
			ranged.add(list.get(i));

		return ranged;
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

		return list.remove(value);
	}

	/**
	 * Add all keys even if they exist
	 *
	 * @param keys
	 */
	public void addWeakAll(Iterable<E> keys) {
		for (final E key : keys)
			addWeak(key);
	}

	/**
	 * Add the given key at the end of the list regardless if it already exist
	 *
	 * @param key
	 */
	public void addWeak(E key) {
		list.add(key);
	}

	/**
	 * Set a key to the certain index
	 *
	 * @param index
	 * @param key
	 */
	public void set(int index, E key) {
		list.set(index, key);
	}

	/**
	 * Return the value or the default
	 *
	 * @param index
	 * @param def
	 * @return
	 */
	public E getOrDefault(int index, E def) {
		return index < list.size() ? list.get(index) : def;
	}

	/**
	 * Return the value at given index
	 *
	 * @param index
	 * @return
	 */
	public E get(int index) {
		return list.get(index);
	}

	/**
	 * Return true if the list contains the key
	 *
	 * @param key
	 * @return
	 */
	public boolean contains(E key) {
		return list.contains(key);
	}

	/**
	 * Return true if the list contains the key
	 *
	 * If the key is a string we return true if it equals your key, case ignored,
	 * otherwise we just call equals() method normally
	 *
	 * @param key
	 * @return
	 */
	public boolean containsIgnoreCase(E key) {
		for (final E other : list) {
			if (other instanceof String && key instanceof String)
				if (((String) other).equalsIgnoreCase((String) key))
					return true;

			if (other.equals(key))
				return true;
		}

		return false;
	}

	/**
	 * Remove every single piece of that list!
	 */
	public void clear() {
		list.clear();
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
	 * Get the list size
	 *
	 * @return
	 */
	public int size() {
		return list.size();
	}

	/**
	 * See {@link List#iterator()}
	 */
	@Override
	public Iterator<E> iterator() {
		return list.iterator();
	}

	/**
	 * See {@link List#toArray()}
	 *
	 * @param e
	 * @return
	 */
	public E[] toArray(E[] e) {
		return list.toArray(e);
	}

	/**
	 * Return {@link List#toArray()}
	 *
	 * @return
	 */
	public Object[] toArray() {
		return list.toArray();
	}

	/**
	 * Serializes every value in the list so you can store it in your settings
	 */
	@Override
	public Object serialize() {
		return SerializeUtil.serializeList(list);
	}

	/**
	 * Return all list values together split by the given separator
	 *
	 * @param separator
	 * @return
	 */
	public String join(String separator) {
		return StringUtils.join(list, separator);
	}

	/**
	 * Returns string representation of this list
	 *
	 * NB: For saving in files call {@link #serialize()}
	 */
	@Override
	public String toString() {
		return list.toString();
	}
}