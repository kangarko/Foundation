package org.mineacademy.fo.collection;

import org.apache.commons.lang.StringUtils;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Strict list that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 */
public final class StrictLinkedList<E> extends LinkedList<E> implements StrictCollection {

	private final String cannotRemoveMessage, cannotAddMessage;

	/**
	 * Create a new list of the given elements
	 *
	 * @param elements
	 */
	@SafeVarargs
	public StrictLinkedList(E... elements) {
		this();

		setAll(Arrays.asList(elements));
	}

	/**
	 * Create a new list of the given elements
	 *
	 * @param oldList
	 */
	public StrictLinkedList(Iterable<E> oldList) {
		this();

		setAll(oldList);
	}

	/**
	 * Create a new empty list
	 */
	public StrictLinkedList() {
		this("Cannot remove '%s' as it is not in the list!", "Value '%s' is already in the list!");
	}

	/**
	 * Create a new empty list with a custom remove/add message that are thrown as errors
	 * if the values already exist or not exist
	 *
	 * @param removeMessage
	 * @param addMessage
	 */
	public StrictLinkedList(String removeMessage, String addMessage) {
		this.cannotRemoveMessage = removeMessage;
		this.cannotAddMessage = addMessage;
	}

	/**
	 * @deprecated As this list is now an extension of the {@link LinkedList}
	 */
	@Deprecated
	public List<E> getSource() {
		return this;
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
		final E e = get(index);
		remove(index);

		return e;
	}

	/**
	 * Remove the given key
	 *
	 * @param key
	 */
	public boolean remove(Object key) {
		final boolean removed = removeWeak(key);

		Valid.checkBoolean(removed, String.format(cannotAddMessage, key));
		return removed;
	}

	/**
	 * Remove the key at the given index
	 *
	 * @param index
	 * @return
	 */
	public E remove(int index) {
		final E removed = remove(index);

		Valid.checkNotNull(removed, String.format(cannotRemoveMessage, "index: " + index));
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
	public boolean add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!contains(key), String.format(cannotAddMessage, key));

		return addWeak(key);
	}

	/**
	 * Creates a copy of the list from the starting index
	 *
	 * @param startIndex
	 * @return
	 */
	public StrictLinkedList<E> range(int startIndex) {
		Valid.checkBoolean(startIndex <= size(), "Start index out of range " + startIndex + " vs. list size " + size());
		final StrictLinkedList<E> ranged = new StrictLinkedList<>();

		for (int i = startIndex; i < size(); i++)
			ranged.add(get(i));

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
	public boolean removeWeak(Object value) {
		Valid.checkNotNull(value, "Cannot remove null values");

		return remove(value);
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
	public boolean addWeak(E key) {
		return super.add(key);
	}

	/**
	 * Return the value or the default
	 *
	 * @param index
	 * @param def
	 * @return
	 */
	public E getOrDefault(int index, E def) {
		return index < size() ? get(index) : def;
	}

	/**
	 * Return true if the list contains the key
	 *
	 * @param key
	 * @return
	 */
	public boolean contains(Object key) {
		return contains(key);
	}

	/**
	 * Return true if the list contains the key
	 * <p>
	 * If the key is a string we return true if it equals your key, case ignored,
	 * otherwise we just call equals() method normally
	 *
	 * @param key
	 * @return
	 */
	public boolean containsIgnoreCase(E key) {
		for (final E other : this) {
			if (other instanceof String && key instanceof String)
				if (((String) other).equalsIgnoreCase((String) key))
					return true;

			if (other.equals(key))
				return true;
		}

		return false;
	}

	/**
	 * Serializes every value in the list so you can store it in your settings
	 */
	@Override
	public Object serialize() {
		return SerializeUtil.serializeList(this);
	}

	/**
	 * Return all list values together split by the given separator
	 *
	 * @param separator
	 * @return
	 */
	public String join(String separator) {
		return StringUtils.join(this, separator);
	}

}
