package org.mineacademy.fo.exception;

import org.mineacademy.fo.remain.RemainCore;

import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * Represents a silent exception thrown then handling commands,
 * this will only send the command sender a message
 */
public class CommandException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The messages to send to the command sender, null if not set
	 */
	@Getter
	private final Component component;

	/**
	 * Create a new command exception
	 */
	public CommandException() {
		this(null);
	}

	/**
	 * Create a new command exception with message for the command sender
	 *
	 * @param component
	 */
	public CommandException(Component component) {
		super("");

		this.component = component;
	}

	@Override
	public String getMessage() {
		return this.component != null ? RemainCore.convertAdventureToLegacy(this.component) : "";
	}
}
