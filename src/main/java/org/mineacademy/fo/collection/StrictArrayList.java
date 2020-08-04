package org.mineacademy.fo.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

/**
 * Strict list that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 */
public final class StrictArrayList<E> extends ArrayList<E> implements StrictList<E> {

	private final String cannotRemoveMessage, cannotAddMessage;

	/**
	 * Create a new list of the given elements
	 *
	 * @param elements
	 */
	@SafeVarargs
	public StrictArrayList(E... elements) {
		this();

		setAll(Arrays.asList(elements));
	}

	/**
	 * Create a new list of the given elements
	 *
	 * @param oldList
	 */
	public StrictArrayList(Iterable<E> oldList) {
		this();

		setAll(oldList);
	}

	/**
	 * Create a new empty list
	 */
	public StrictArrayList() {
		this("Cannot remove '%s' as it is not in the list!", "Value '%s' is already in the list!");
	}

	public StrictArrayList(int initialSize) {
		this("Cannot remove '%s' as it is not in the list!", "Value '%s' is already in the list!", initialSize);
	}

	/**
	 * Create a new empty list with a custom remove/add message that are thrown as errors
	 * if the values already exist or not exist
	 *
	 * @param removeMessage
	 * @param addMessage
	 */
	public StrictArrayList(String removeMessage, String addMessage) {
		this.cannotRemoveMessage = removeMessage;
		this.cannotAddMessage = addMessage;
	}

	public StrictArrayList(String removeMessage, String addMessage, int initialSize) {
		super(initialSize);
		this.cannotRemoveMessage = removeMessage;
		this.cannotAddMessage = addMessage;
	}

	/**
	 * @deprecated As this list is now an extension of the {@link ArrayList}
	 */
	@Deprecated
	public List<E> getSource() {
		return this;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger strict checks
	// ------------------------------------------------------------------------------------------------------------


	@Override public void setAll(Iterable<E> elements) {
		clear();
		addAll0(elements);
	}

	@Override public E getAndRemove(int index) {
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

	@Override public void addAll0(Iterable<E> elements) {
		for (final E key : elements)
			add(key);
	}


	@Override
	public void addIfNotExist(E key) {
		if (!contains(key))
			add(key);
	}

	
	public boolean add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!contains(key), String.format(cannotAddMessage, key));

		return addWeak(key);
	}

	@Override public StrictArrayList<E> range(int startIndex) {
		Valid.checkBoolean(startIndex <= size(), "Start index out of range " + startIndex + " vs. list size " + size());
		final StrictArrayList<E> ranged = new StrictArrayList<>();

		for (int i = startIndex; i < size(); i++)
			ranged.add(get(i));

		return ranged;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods without throwing errors below
	// ------------------------------------------------------------------------------------------------------------

	@Override public boolean removeWeak(Object value) {
		Valid.checkNotNull(value, "Cannot remove null values");

		return remove(value);
	}

	@Override public void addWeakAll(Iterable<E> keys) {
		for (final E key : keys)
			addWeak(key);
	}


	@Override public boolean addWeak(E key) {
		return super.add(key);
	}

	@Override public E getOrDefault(int index, E def) {
		return index < size() ? get(index) : def;
	}

	@Override
	public Object serialize() {
		return SerializeUtil.serializeList(this);
	}

	@Override public String join(String separator) {
		return StringUtils.join(this, separator);
	}

}
