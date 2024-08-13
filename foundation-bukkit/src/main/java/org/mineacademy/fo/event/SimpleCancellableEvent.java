package org.mineacademy.fo.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An extension of {@link SimpleEvent} which also makes your custom events
 * cancellable.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SimpleCancellableEvent extends SimpleEvent {

	/**
	 * Is the event cancelled?
	 */
	private boolean cancelled;

	/**
	 * Create a new event indicating whether it is run from
	 * the primary Minecraft server thread or not
	 *
	 * @param async
	 */
	protected SimpleCancellableEvent(boolean async) {
		super(async);
	}
}
