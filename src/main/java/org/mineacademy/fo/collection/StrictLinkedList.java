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
public final class StrictLinkedList<E> extends LinkedList<E> implements StrictList<E> {

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
	
	public boolean setAll(Iterable<? extends E> elements) {
		clear();
		return addAll0(elements);
	}

	public E getAndRemove(int index) {
		final E e = get(index);
		remove(index);

		return e;
	}

	public boolean remove(Object key) {
		final boolean removed = removeWeak(key);

		Valid.checkBoolean(removed, String.format(cannotAddMessage, key));
		return removed;
	}

	public E remove(int index) {
		final E removed = super.remove(index);

		Valid.checkNotNull(removed, String.format(cannotRemoveMessage, "index: " + index));
		return removed;
	}

	public boolean addAll0(Iterable<? extends E> elements) {
		for (final E key : elements)
			add(key);
		return true;
	}

	@Override
	public boolean removeAll0(Iterable<? extends E> elements) {
		boolean modified = false;
		for (Object o : elements) {
			boolean b = remove(o);
			if (!modified)
				modified = b;
		}
		return modified;
	}

	public void addIfNotExist(E key) {
		if (!contains(key))
			add(key);
	}

	public boolean add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!contains(key), String.format(cannotAddMessage, key));

		return addWeak(key);
	}

	public StrictList<E> range(int startIndex) {
		Valid.checkBoolean(startIndex <= size(), "Start index out of range " + startIndex + " vs. list size " + size());
		final StrictLinkedList<E> ranged = new StrictLinkedList<>();

		for (int i = startIndex; i < size(); i++)
			ranged.add(get(i));

		return ranged;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods without throwing errors below
	// ------------------------------------------------------------------------------------------------------------

	public boolean removeWeak(Object value) {
		Valid.checkNotNull(value, "Cannot remove null values");

		return remove(value);
	}

	public void addWeakAll(Iterable<E> keys) {
		for (final E key : keys)
			addWeak(key);
	}

	public boolean addWeak(E key) {
		return super.add(key);
	}

	public E getOrDefault(int index, E def) {
		return index < size() ? get(index) : def;
	}

	public boolean contains(Object key) {
		return contains(key);
	}

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

	@Override
	public Object serialize() {
		return SerializeUtil.serializeList(this);
	}

	public String join(String separator) {
		return StringUtils.join(this, separator);
	}

}
