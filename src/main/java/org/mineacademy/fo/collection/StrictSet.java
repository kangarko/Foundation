package org.mineacademy.fo.collection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

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

		this.addAll(Arrays.asList(elements));
	}

	/**
	 * Create a new set from the given elements
	 *
	 * @param oldList
	 */
	public StrictSet(Iterable<E> oldList) {
		this();

		this.addAll(oldList);
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
		final boolean removed = this.set.remove(value);

		Valid.checkBoolean(removed, String.format(this.getCannotRemoveMessage(), value));
	}

	/**
	 * Remove the given element from the set
	 *
	 * @param value
	 */
	public void removeWeak(E value) {
		this.set.remove(value);
	}

	/**
	 * Add all elements to the set
	 *
	 * @param collection
	 */
	public void addAll(Iterable<E> collection) {
		for (final E val : collection)
			this.add(val);
	}

	/**
	 * Add the given element to the set
	 * failing if it is null or already within the set
	 *
	 * @param key
	 */
	public void add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!this.set.contains(key), String.format(this.getCannotAddMessage(), key));

		this.set.add(key);
	}

	/**
	 * Add the given element to the set
	 *
	 * @param key
	 */
	public void override(E key) {
		this.set.add(key);
	}

	/**
	 * Returns the first value or null if the list is empty
	 *
	 * @return
	 */
	@Nullable
	public E first() {
		return this.set.isEmpty() ? null : this.set.iterator().next();
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
		return this.set.contains(key);
	}

	/**
	 * Clear the set
	 */
	public void clear() {
		this.set.clear();
	}

	/**
	 * Return true if the set is empty
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.set.isEmpty();
	}

	/**
	 * Return the sets size
	 *
	 * @return
	 */
	public int size() {
		return this.set.size();
	}

	/**
	 * Return the original Java implementation of the set
	 *
	 * @return
	 */
	public Set<E> getSource() {
		return this.set;
	}

	/**
	 * Return all set values together split by the given separator
	 *
	 * @param separator
	 * @return
	 */
	public String join(String separator) {
		return Common.join(this.set, separator);
	}

	/**
	 * Return this set as array
	 *
	 * @param e
	 * @return
	 */
	public E[] toArray(E[] e) {
		return this.set.toArray(e);
	}

	/**
	 * Return the sets iterator
	 */
	@Override
	public Iterator<E> iterator() {
		return this.set.iterator();
	}

	/**
	 * Return this set as list of serialized objects
	 */
	@Override
	public Object serialize() {
		return SerializeUtil.serialize(this.set);
	}

	@Override
	public String toString() {
		return "StrictSet{\n\t" + Common.join(this.set, "\n\t") + "\n}";
	}
}