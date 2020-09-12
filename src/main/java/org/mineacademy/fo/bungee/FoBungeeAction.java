package org.mineacademy.fo.bungee;

import lombok.Getter;

/**
 * Proprietary implementation of BungeeAction for some of our
 * premium plugins handled by BungeeControl
 *
 * The BungeeCord protocol always begins with
 *
 * 1) The sender server name
 * 2) The {@link BungeeAction}
 *
 * and the rest is the actual data within this enum
 */
public enum FoBungeeAction implements BungeeAction {

	// ----------------------------------------------------------------------------------------------------
	// Stay on BungeeCord using BungeeControl plugin
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Execute a command on bungee
	 *
	 * Parameters: command
	 */
	BUNGEE_COMMAND(String.class),

	/**
	 * Sends the player a simple colorized message
	 *
	 * Parameters: player, message
	 */
	TELL_PLAYER(String.class, String.class),

	// ----------------------------------------------------------------------------------------------------
	// Forward to Bukkit servers
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Mute the chat.
	 *
	 * Parameters: message
	 */
	CHAT_MUTE(String.class),

	/**
	 * Wipe the chat.
	 *
	 * Parameters: message
	 */
	CHAT_CLEAR(String.class),

	/**
	 * Broadcast a message in a channel.
	 *
	 * Parameters: sender, channel name, message, has mute bypass permission, has ignore bypass permission
	 */
	CHANNEL(String.class, String.class, String.class, Boolean.class, Boolean.class),

	/**
	 * Sends an announcement message to everyone, and plays a nice sound
	 *
	 * Parameters: console message, chat message to announce
	 */
	ANNOUNCEMENT(String.class, String.class),

	/**
	 * Broadcast a json message to every player with permission as a sender.
	 *
	 * Parameters: sender, show permission, json message, has sender ignore bypass permission?
	 */
	BROADCAST_JSON_WITH_PERMISSION_AS(String.class, String.class, String.class, Boolean.class),

	/**
	 * Forwards a spy message to players with permissions
	 *
	 * Parameters: message
	 */
	SPY(String.class),

	/**
	 * Attempts to find a private message recipient, reports back to bungee
	 * whether we found him or not
	 *
	 * Parameters: sender, recipient, sender message, recipient message, has bypass permission, has toggle PM bypass permission, has PM vanished bypass permission
	 */
	PM_LOOKUP(String.class, String.class, String.class, String.class, Boolean.class, Boolean.class, Boolean.class),

	/**
	 * This message is forwarded after PM_LOOKUP to the sender server
	 * to tell him that the recipient was not found
	 *
	 * Parameters: sender, recipient
	 */
	PM_PLAYER_NOT_FOUND(String.class, String.class),

	/**
	 * Forwarded after PM_LOOKUP is done to deliver the private message
	 * to the recipient
	 *
	 * Parameters: sender, recipient, message
	 */
	PM_PLAYER_FOUND(String.class, String.class, String.class),

	;

	/**
	 * Stores all valid values, the names of them are only used
	 * in the error message when the length of data does not match
	 */
	@Getter
	private final Class<?>[] content;

	/**
	 * Constructs a new bungee action
	 *
	 * @param validValues
	 */
	FoBungeeAction(final Object... validValues) {
		final Class<?>[] classes = new Class<?>[validValues.length];

		for (int i = 0; i < classes.length; i++) {
			final Object value = validValues[i];

			classes[i] = value.getClass();
		}

		content = classes;
	}
}