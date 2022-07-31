package org.mineacademy.fo.collection;

import org.mineacademy.fo.SerializeUtil.Mode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Strict collection does not allow adding duplicate elements,
 * and throw an error when you attempt to remove a non-existing element from
 * list/map.
 */
@Getter(value = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public abstract class StrictCollection {

	/**
	 * Determines how you want this list to be saved, if you use JSON or YAML file.
	 * Used in {@link #serialize()}. Defaults to YAML
	 */
	@Setter
	@Getter
	private Mode mode = Mode.YAML;

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
