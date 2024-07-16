package org.mineacademy.fo.proxy.message;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Represents a in/out message with a given action and server name
 * and a safety check for writing/reading the data
 * based on the action's content.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
abstract class Message {

	/**
	 * The listener associated with this message
	 */
	private final ProxyListener listener;

	/**
	 * The action
	 */
	private final ProxyMessage message;

	/**
	 * The current position of writing the data based on the
	 * {@link ProxyMessage#getContent()}
	 */
	private int head = 0;

	/**
	 * Ensures we are reading in the correct order as the given {@link ProxyMessage}
	 * specifies in its {@link ProxyMessage#getContent()} getter.
	 * <p>
	 * This also ensures we are reading the correct data type (both primitives and wrappers
	 * are supported).
	 *
	 * @param requiredType
	 */
	protected final void moveHead(Class<?> requiredType) {
		Valid.checkNotNull(this.message, "Action not set!");

		final Class<?>[] content = this.message.getContent();
		final Class<?> clazz = content[this.head];

		Valid.checkBoolean(requiredType.isAssignableFrom(clazz), "Expected " + requiredType.getSimpleName() + " at position " + head + " but got " + clazz.getSimpleName() + " for " + this.getMessage().name());
		Valid.checkBoolean(head < content.length, "Head out of bounds! Max data size for " + this.getMessage().name() + " is " + content.length);

		this.head++;
	}

	/**
	 *
	 * @return
	 */
	public final ProxyListener getListener() {
		return listener;
	}

	/**
	 *
	 * @return
	 */
	public final <T extends ProxyMessage> T getMessage() {
		return (T) message;
	}
}
