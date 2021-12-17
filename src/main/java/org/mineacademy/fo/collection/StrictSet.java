package org.mineacademy.fo.collection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;

/**
 * Strict set that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 * @param <E>
 */
public final class StrictSet<E> extends StrictCollection implements Iterable<E> {

	/**
	 * The internal set
	 */
	private final Set<E> set = new LinkedHashSet<>();

	/**
	 * Create a new set from the given elements
	 *
	 * @param elements
	 */
	@SafeVarargs
	public StrictSet(E... elements) {
		this();

		addAll(Arrays.asList(elements));
	}

	/**
	 * Create a new set from the given elements
	 *
	 * @param oldList
	 */
	public StrictSet(Iterable<E> oldList) {
		this();

		addAll(oldList);
	}

	/**
	 * Create a new strict set
	 */
	public StrictSet() {
		super("Cannot remove '%s' as it is not in the set!", "Value '%s' is already in the set!");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger strict checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Remove the given element from the set
	 * failing if it is null or not within the set
	 *
	 * @param value
	 */
	public void remove(E value) {
		Valid.checkNotNull(value, "Cannot remove null values");
		final boolean removed = set.remove(value);

		Valid.checkBoolean(removed, String.format(getCannotRemoveMessage(), value));
	}

	/**
	 * Remove the given element from the set
	 *
	 * @param value
	 */
	public void removeWeak(E value) {
		set.remove(value);
	}

	/**
	 * Add all elements to the set
	 *
	 * @param collection
	 */
	public void addAll(Iterable<E> collection) {
		for (final E val : collection)
			add(val);
	}

	/**
	 * Add the given element to the set
	 * failing if it is null or already within the set
	 *
	 * @param key
	 */
	public void add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!set.contains(key), String.format(getCannotAddMessage(), key));

		set.add(key);
	}

	/**
	 * Add the given element to the set
	 *
	 * @param key
	 */
	public void override(E key) {
		set.add(key);
	}

	/**
	 * Return the element at the given index
	 *
	 * @param index
	 * @return
	 */
	public E getAt(int index) {
		int i = 0;

		final Iterator<E> it = set.iterator();

		while (it.hasNext()) {
			final E e = it.next();

			if (i++ == index)
				return e;
		}

		throw new FoException("Index (" + index + ") + out of size (" + set.size() + ")");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods without throwing errors below
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the set contains the given element
	 *
	 * @param key
	 * @return
	 */
	public boolean contains(E key) {
		return set.contains(key);
	}

	/**
	 * Clear the set
	 */
	public void clear() {
		set.clear();
	}

	/**
	 * Return true if the set is empty
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return set.isEmpty();
	}

	/**
	 * Return the sets size
	 *
	 * @return
	 */
	public int size() {
		return set.size();
	}

	/**
	 * Return the original Java implementation of the set
	 *
	 * @return
	 */
	public Set<E> getSource() {
		return set;
	}

	/**
	 * Return all set values together split by the given separator
	 *
	 * @param separator
	 * @return
	 */
	public String join(String separator) {
		return StringUtils.join(set, separator);
	}

	/**
	 * Return this set as array
	 *
	 * @param e
	 * @return
	 */
	public E[] toArray(E[] e) {
		return set.toArray(e);
	}

	/**
	 * Return the sets iterator
	 */
	@Override
	public Iterator<E> iterator() {
		return set.iterator();
	}

	/**
	 * Return this set as list of serialized objects
	 */
	@Override
	public Object serialize() {
		return SerializeUtil.serialize(set);
	}

	@Override
	public String toString() {
		return "StrictSet{\n\t" + Common.join(set, "\n\t") + "\n}";
	}
}