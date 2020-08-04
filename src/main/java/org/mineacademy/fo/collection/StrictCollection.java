package org.mineacademy.fo.collection;

/**
 * Strict collection does not allow adding duplicate elements,
 * and throw an error when you attempt to remove a non-existing element from
 * list/map.
 */
public interface StrictCollection {

	/**
	 * Convert this object into something that can be safely stored in a settings file
	 *
	 * @return
	 */
	 Object serialize();
}
