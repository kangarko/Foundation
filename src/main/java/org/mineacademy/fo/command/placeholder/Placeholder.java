package org.mineacademy.fo.command.placeholder;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A simple placeholder used in commands to replace {placeholder} with an actual value
 *
 * This is used to remove boilerplace code when sending messages
 */
@RequiredArgsConstructor
public abstract class Placeholder {

	/**
	 * The name of the variable, example "player" would replace {player}
	 */
	@Getter
	@NonNull
	private final String identifier;

	/**
	 * Replace the message with this placeholder
	 *
	 * @param message, the entire message or a part of it
	 * @return
	 */
	public abstract String replace(String message);

	@Override
	public final boolean equals(Object obj) {
		return obj instanceof Placeholder ? ((Placeholder) obj).identifier.equals(this.identifier) : false;
	}
}