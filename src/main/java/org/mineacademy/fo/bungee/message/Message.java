package org.mineacademy.fo.bungee.message;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.bungee.BungeeListener;
import org.mineacademy.fo.bungee.BungeeMessageType;

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
	private final BungeeListener listener;

	/**
	 * The action
	 */
	private final BungeeMessageType action;

	/**
	 * The current position of writing the data based on the
	 * {@link BungeeMessageType#getContent()}
	 */
	private int actionHead = 0;

	/**
	 * Ensures we are reading in the correct order as the given {@link BungeeMessageType}
	 * specifies in its {@link BungeeMessageType#getContent()} getter.
	 * <p>
	 * This also ensures we are reading the correct data type (both primitives and wrappers
	 * are supported).
	 *
	 * @param typeOf
	 */
	protected final void moveHead(Class<?> typeOf) {
		Valid.checkNotNull(this.action, "Action not set!");

		final Class<?>[] content = this.action.getContent();
		Valid.checkBoolean(this.actionHead < content.length, "Head out of bounds! Max data size for " + this.action.name() + " is " + content.length);

		this.actionHead++;
	}

	/**
	 *
	 * @return
	 */
	public final BungeeListener getListener() {
		return listener;
	}

	/**
	 *
	 * @return
	 */
	public final <T extends BungeeMessageType> T getAction() {
		return (T) action;
	}
}
