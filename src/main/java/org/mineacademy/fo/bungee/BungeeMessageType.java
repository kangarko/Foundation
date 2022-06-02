package org.mineacademy.fo.bungee;

/**
 * Represents an action sent over BungeeCord containing
 * a set of data. We recommend you create an enum that implements this.
 */
public interface BungeeMessageType {

	/**
	 * Stores all valid values in this action in the order of which they
	 * are being sent. Only primitive types, UUID, SerializedMap and String are supported.
	 *
	 * @return
	 */
	Class<?>[] getContent();

	/**
	 * The name of this action
	 *
	 * @return
	 */
	String name();

	/**
	 * Retrieve BungeeAction by its name
	 *
	 * @param listener
	 * @param name
	 *
	 * @return
	 */
	static BungeeMessageType getByName(BungeeListener listener, String name) {
		final BungeeMessageType[] actions = listener.getActions();

		for (final BungeeMessageType action : actions)
			if (action.name().equals(name))
				return action;

		return null;
	}
}
