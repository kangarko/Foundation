package org.mineacademy.fo.bungee;

/**
 * Represents an action sent over BungeeCord containing
 * a set of data. We recommend you create an enum that implements this.
 */
public interface BungeeAction {

	/**
	 * Stores all valid values in this action in the order of which they
	 * are being sent. Only primitive types and String are supported.
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
	static BungeeAction getByName(BungeeListener listener, String name) {
		final BungeeAction[] actions = listener.getActions();

		for (final BungeeAction action : actions)
			if (action.name().equals(name))
				return action;

		return null;
	}
}
