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
public abstract class AbstractStrictDataHolder implements StrictDataHolder {

	/**
	 * {@inheritDoc}
	 */
	private final String cannotRemoveMessage;

	/**
	 * {@inheritDoc}
	 */
	private final String cannotAddMessage;

}
