package org.mineacademy.fo.exception;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * Represents a silent exception thrown then handling commands,
 * this will only send the command sender a message
 */
public class CommandException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The messages to send to the command sender
	 */
	@Getter
	private final Component component;

	/**
	 * Create a new command exception with messages for the command sender
	 *
	 * @param messages
	 */
	public CommandException(String... messages) {
		super("");

		Component component = Component.text("");

		for (int i = 0; i < messages.length; i++) {
			component = component.append(Remain.convertLegacyToAdventure(Common.colorize(messages[i])));

			if (i < messages.length - 1)
				component = component.appendNewline();
		}

		this.component = component;
	}

	/**
	 * Create a new command exception with messages for the command sender
	 *
	 * @param component
	 */
	public CommandException(Component component) {
		super("");

		this.component = component;
	}
}
