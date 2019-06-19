package org.mineacademy.fo.command.placeholder;

import lombok.Getter;

/**
 * A position placeholder simply replaces {0} with args[0],
 * {1} with args[1] and so on.
 */
public abstract class PositionPlaceholder extends Placeholder {

	/**
	 * What argument number is replacing?
	 */
	@Getter
	private final int position;

	/**
	 * Create a new position placeholder
	 *
	 * @param identifier
	 * @param position
	 */
	protected PositionPlaceholder(String identifier, int position) {
		super(identifier);

		this.position = position;
	}
}