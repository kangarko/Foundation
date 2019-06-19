package org.mineacademy.fo.model;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictSet;

/**
 * A simple class for matching items in the list
 *
 * @param <T>
 */
public final class Allower<T> {

	/**
	 * How should we match items in the list?
	 */
	private final AllowMode mode;

	/**
	 * The item list
	 */
	private final StrictSet<T> items;

	/**
	 * Create a new allower without items, only works when mode is NOT specific
	 *
	 * @param mode
	 */
	public Allower(AllowMode mode) {
		this(null, mode);
	}

	/**
	 * Create a new allowed with items
	 *
	 * @param items
	 */
	public Allower(StrictSet<T> items) {
		this(items, AllowMode.SPECIFIC);
	}

	/**
	 * Create a new allowed for the given mode for items
	 *
	 * @param items
	 * @param mode
	 */
	private Allower(StrictSet<T> items, AllowMode mode) {
		this.items = items;
		this.mode = mode;

		if (items == null)
			Valid.checkBoolean(mode != AllowMode.SPECIFIC, "Mode cannot be specific when the list is null");
	}

	/**
	 * Return true according to the {@link #mode} and the given item
	 *
	 * @param item
	 * @return
	 */
	public boolean isAllowed(T item) {
		if (mode == AllowMode.NONE)
			return false;

		if (mode == AllowMode.ALL)
			return true;

		return items.contains(item);
	}

	/**
	 * Represents how the items should be matched
	 */
	public enum AllowMode {

		/**
		 * Always return true regardless what item is matched
		 */
		ALL,

		/**
		 * Never ever return true regardless what item is matched
		 */
		NONE,

		/**
		 * The standard mode, return true if the item is in the specified item list
		 */
		SPECIFIC
	}
}
