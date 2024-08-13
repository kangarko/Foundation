package org.mineacademy.fo.exception;

import net.kyori.adventure.text.Component;

/**
 * Thrown when a command has invalid argument
 */
public final class InvalidCommandArgException extends CommandException {

	private static final long serialVersionUID = 1L;

	public InvalidCommandArgException() {
	}

	public InvalidCommandArgException(Component message) {
		super(message);
	}
}