package org.mineacademy.fo.exception;

import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;

/**
 * Represents a silent exception with a localizable message.
 */
public class CommandException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The messages to send to the command sender, null if not set
	 */
	private final SimpleComponent[] components;

	/**
	 * Create a new command exception
	 */
	public CommandException() {
		this((SimpleComponent[]) null);
	}

	/**
	 * Create a new command exception with message for the command sender
	 *
	 * @param components
	 */
	public CommandException(SimpleComponent... components) {
		super("");

		this.components = components;
	}

	/**
	 * Return the components to send to the command sender.
	 *
	 * @return
	 */
	public final SimpleComponent[] getComponents() {
		return this.components == null ? new SimpleComponent[0] : this.components;
	}

	/**
	 * Send the error message to the given audience.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 */
	public final void sendErrorMessage(FoundationPlayer audience) {
		if (this.components != null)
			if (this.components.length == 1)
				Messenger.error(audience, this.components[0]);
			else
				for (final SimpleComponent component : this.components)
					audience.sendMessage(component.color(CompChatColor.RED));
	}

	/**
	 * Get the message as a string.
	 *
	 * @return
	 */
	@Override
	public final String getMessage() {
		return this.components != null ? SimpleComponent.fromChildren(this.components).toLegacy() : "";
	}
}
