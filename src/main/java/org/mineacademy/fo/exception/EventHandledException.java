package org.mineacademy.fo.exception;

import lombok.Getter;

/**
 * Represents a silent exception thrown then handling events,
 * this will only send the event player a message
 */
public final class EventHandledException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The messages to send to the command sender
	 */
	@Getter
	private final String[] messages;

	/**
	 * Should we cancel this event?
	 */
	@Getter
	private final boolean cancelled;

	public EventHandledException() {
		this(true);
	}

	/**
	 * Create a new command exception with messages for the command sender
	 *
	 * @param messages
	 */
	public EventHandledException(boolean cancelled, String... messages) {
		super("");

		this.cancelled = cancelled;
		this.messages = messages;
	}
}
