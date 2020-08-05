package org.mineacademy.fo.collection;

/**
 * Strict data holder does not allow adding duplicate elements,
 * and throw an error when you attempt to remove a non-existing element from
 * this holder.
 */
public interface StrictDataHolder {

	/**
	 * Convert this object into something that can be safely stored in a settings file
	 *
	 * @return
	 */
	 Object serialize();
}
