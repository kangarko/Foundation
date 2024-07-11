package org.mineacademy.fo.proxy;

/**
 * Represents a plugin message type we send over proxy containing
 * a set of data. We recommend you create an enum that implements this.
 */
public interface ProxyMessage {

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
	 * Retrieve a {@link ProxyMessage} by its name
	 *
	 * @param listener
	 * @param name
	 *
	 * @return
	 */
	static ProxyMessage getByName(ProxyListener listener, String name) {
		for (final ProxyMessage message : listener.getActions())
			if (message.name().equals(name))
				return message;

		return null;
	}
}
