package org.mineacademy.fo.collection;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Strict collection does not allow adding duplicate elements,
 * and throw an error when you attempt to remove a non-existing element from
 * list/map.
 */
@Getter(value = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public abstract class StrictCollection {

	/**
	 * The error message when removing non-existing keys
	 */
	private final String cannotRemoveMessage;

	/**
	 * The error message when adding duplicate keys
	 */
	private final String cannotAddMessage;

	/**
	 * Convert this object into something that can be safely stored in a settings file
	 *
	 * @return
	 */
	public abstract Object serialize();
}
