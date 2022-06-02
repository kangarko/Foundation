package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Iterator;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.StrictSet;

import lombok.Getter;

/**
 * A simple class allowing you to match if something is in that list.
 * <p>
 * Example: The list contains "apple", "red", "car",
 * you call isInList("car") and you get true. Same for any other data type
 * <p>
 * If you create new IsInList("*") or with an empty list, everything will be matched
 *
 * @param <T>
 */
public final class IsInList<T> implements Iterable<T> {

	/**
	 * The internal set for matching
	 */
	@Getter
	private final StrictSet<T> list;

	/**
	 * Is everything matched?
	 */
	private final boolean matchAll;

	/**
	 * Create a new is in list
	 *
	 * @param list
	 * @param matchAll
	 */
	private IsInList(final Iterable<T> list, boolean matchAll) {
		this.list = new StrictSet<>(list);
		this.matchAll = matchAll;
	}

	/**
	 * Return true if the given value is in this list
	 *
	 * @param toEvaluateAgainst
	 * @return
	 */
	public boolean contains(final T toEvaluateAgainst) {

		// Return false when list is empty and we are not always true
		if (!this.matchAll && this.list.isEmpty())
			return false;

		return this.matchAll || this.list.contains(toEvaluateAgainst);
	}

	/**
	 * Return true if list is equal to ["*"]
	 *
	 * @return
	 */
	public boolean isEntireList() {
		return this.matchAll;
	}

	/**
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<T> iterator() {
		return this.list.iterator();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "IsInList[entire=" + this.matchAll + ", list=" + Common.join(this.list) + "]";
	}

	/**
	 * Create a new matching list from the given list
	 *
	 * @param list
	 * @return
	 */
	public static <T> IsInList<T> fromList(Iterable<T> list) {
		boolean matchAll = false;

		for (final T t : list)
			if ("*".equals(t)) {
				matchAll = true;

				break;
			}

		return new IsInList<>(list, matchAll);
	}

	/**
	 * Create a new matching list that is always true
	 *
	 * @return
	 */
	public static <T> IsInList<T> fromStar() {
		return new IsInList<>(new ArrayList<T>(), true);
	}

}
