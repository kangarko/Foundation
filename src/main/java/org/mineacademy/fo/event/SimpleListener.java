package org.mineacademy.fo.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.EventExecutor;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A simply way of allowing plugin to change the event listening priority
 *
 * @param <T> the event we are listening for
 */
@RequiredArgsConstructor
public abstract class SimpleListener<T extends Event> implements Listener, EventExecutor {

	/**
	 * The event we are listening to
	 */
	private final Class<T> eventClass;

	/**
	 * The event priority
	 */
	private final EventPriority priority;

	/**
	 * Shall we ignore cancelled events down the pipeline?
	 */
	private final boolean ignoreCancelled;

	/**
	 * The run event temporary placeholder
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private T event;

	/**
	 * Creates a new listener using the normal priority
	 * and ignoring cancelled
	 *
	 * @param event
	 */
	public SimpleListener(Class<T> event) {
		this(event, EventPriority.NORMAL);
	}

	/**
	 * Creates a new listener ignoring cancelled
	 *
	 * @param event
	 * @param priority
	 */
	public SimpleListener(Class<T> event, EventPriority priority) {
		this(event, priority, true);
	}

	@Override
	public final void execute(Listener listener, Event event) throws EventException {

		if (!event.getClass().equals(this.eventClass))
			return;

		final String logName = listener.getClass().getSimpleName() + " listening to " + event.getEventName() + " at " + priority + " priority";

		LagCatcher.start(logName);

		try {
			this.event = this.eventClass.cast(event);

			execute((T) event);

		} catch (final EventHandledException ex) {
			final String[] messages = ex.getMessages();
			final boolean cancelled = ex.isCancelled();

			final Player player = findPlayer();

			if (messages != null && player != null)
				for (String message : messages) {
					message = Variables.replace(message, player);

					if (Messenger.ENABLED)
						Messenger.error(player, message);
					else
						Common.tell(player, "&c" + message);
				}

			if (cancelled && event instanceof Cancellable)
				((Cancellable) event).setCancelled(true);

		} catch (final Throwable t) {
			Common.error(t, "Unhandled exception listening to " + this.eventClass.getSimpleName());

		} finally {
			LagCatcher.end(logName);

			// Do not null the event since this breaks findPlayer for any scheduled tasks
			//this.event = null;
		}
	}

	/**
	 * Executes when the event is run
	 *
	 * @param event
	 */
	protected abstract void execute(T event);

	/**
	 * Return a player from this event, null if none,
	 * used for messaging
	 *
	 * @return
	 */
	protected Player findPlayer() {
		Valid.checkNotNull(this.event, "Called findPlayer for null event!");

		if (this.event instanceof PlayerEvent)
			return ((PlayerEvent) this.event).getPlayer();

		throw new FoException("Called findPlayer but not method not implemented for event " + this.event);
	}

	/**
	 * If the object is null, stop your code from further execution, cancel the event and
	 * send the player a null message (see {@link #findPlayer(Event)})
	 *
	 * @param toCheck
	 * @param falseMessages
	 */
	protected final void checkNotNull(Object toCheck, String... nullMessages) {
		checkBoolean(toCheck != null, nullMessages);
	}

	/**
	 * If the condition is false, stop your code from further execution, cancel the event and
	 * send the player a false message (see {@link #findPlayer(Event)})
	 *
	 * @param condition
	 * @param falseMessages
	 */
	protected final void checkBoolean(boolean condition, String... falseMessages) {
		if (!condition)
			throw new EventHandledException(true, falseMessages);
	}

	/**
	 * Stop code from executing and send the player a message (see {@link #findPlayer(Event)})
	 * when he lacks the given permission
	 *
	 * @param permission
	 */
	protected final void checkPerm(String permission) {
		checkPerm(permission, SimpleLocalization.NO_PERMISSION);
	}

	/**
	 * Return if the {@link #findPlayer(Event)} player has the given permission;
	 *
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(String permission) {
		return PlayerUtil.hasPerm(findPlayer(), permission);
	}

	/**
	 * Stop code from executing and send the player a message (see {@link #findPlayer(Event)})
	 * when he lacks the given permission
	 *
	 * @param permission
	 * @param falseMessage
	 */
	protected final void checkPerm(String permission, String falseMessage) {
		final Player player = findPlayer();
		Valid.checkNotNull(player, "Player cannot be null for " + this.event + "!");

		if (!PlayerUtil.hasPerm(player, permission))
			throw new EventHandledException(true, falseMessage.replace("{permission}", permission));
	}

	/**
	 * Cancel the event and send the player a message (see {@link #findPlayer(Event)})
	 *
	 * @param messages
	 */
	protected final void cancel(String... messages) {
		throw new EventHandledException(true, messages);
	}

	/**
	 * Cancel this event
	 */
	protected final void cancel() {
		throw new EventHandledException(true);
	}

	/**
	 * Return code execution and send messages
	 *
	 * @param messages
	 */
	protected final void returnTell(String... messages) {
		throw new EventHandledException(false, messages);
	}

	/**
	 * A shortcut for registering this event in Bukkit
	 */
	public final void register() {
		Bukkit.getPluginManager().registerEvent(eventClass, this, priority, this, SimplePlugin.getInstance(), ignoreCancelled);
	}
}
