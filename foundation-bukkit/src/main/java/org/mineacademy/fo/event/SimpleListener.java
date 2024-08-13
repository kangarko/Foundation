package org.mineacademy.fo.event;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

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
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
	 * The optional player implementation for some helper methods
	 */
	@Nullable
	private Player player;

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

		final boolean eventIgnored = event.getEventName().equals("SimpleChatEvent");
		final String logName = (listener != null ? listener.getClass().getSimpleName() + " listening to " : "") + event.getEventName() + " at " + this.priority + " priority";

		if (!eventIgnored)
			LagCatcher.start(logName);

		if (event instanceof PlayerEvent)
			this.player = ((PlayerEvent) event).getPlayer();

		else {
			try {
				final Method getPlayer = ReflectionUtil.getMethod(event.getClass(), "getPlayer");

				if (getPlayer != null)
					this.player = ReflectionUtil.invoke(getPlayer, event);
			} catch (final Throwable ignored) {
			}
		}

		try {
			this.execute(this.eventClass.cast(event));

		} catch (final EventHandledException ex) {
			final boolean cancelled = ex.isCancelled();

			if (ex.getComponent() != null && this.player != null)
				if (Messenger.ENABLED)
					Messenger.error(this.player, ex.getComponent());
				else
					Common.tell(this.player, Component.text("").color(NamedTextColor.RED).append(ex.getComponent()));

			if (cancelled && event instanceof Cancellable)
				((Cancellable) event).setCancelled(true);

		} catch (final Throwable t) {
			Common.error(t, "Unhandled exception listening to " + this.eventClass.getSimpleName());

		} finally {
			if (!eventIgnored)
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
	 * Call this in your event to set the player.
	 *
	 * @param player
	 */
	protected final void setPlayer(Player player) {
		this.player = player;
	}

	/*
	 * Return a player from this event, null if none,
	 * used for messaging
	 */
	private Player findPlayer() {
		Valid.checkNotNull(this.player, "Call setPlayer() early in your event to set the player");

		return this.player;
	}

	/**
	 * If the object is null, stop your code from further execution, cancel the event and
	 * send the player a null message (see {@link #findPlayer(Event)})
	 *
	 * @param toCheck
	 * @param falseMessages
	 */
	protected final void checkNotNull(Object toCheck, Component nullMessages) {
		this.checkBoolean(toCheck != null, nullMessages);
	}

	/**
	 * If the condition is false, stop your code from further execution, cancel the event and
	 * send the player a false message (see {@link #findPlayer(Event)})
	 *
	 * @param condition
	 * @param falseMessages
	 */
	/*protected final void checkBoolean(boolean condition, String... falseMessages) {
		if (!condition)
			throw new EventHandledException(true, falseMessages);
	}*/

	/**
	 * If the condition is false, stop your code from further execution, cancel the event and
	 * send the player a false message (see {@link #findPlayer(Event)})
	 *
	 * @param condition
	 * @param falseMessages
	 */
	protected final void checkBoolean(boolean condition, Component falseMessages) {
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
		this.checkPerm(permission, SimpleLocalization.NO_PERMISSION);
	}

	/**
	 * Return if the {@link #findPlayer(Event)} player has the given permission;
	 *
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(String permission) {
		return this.findPlayer().hasPermission(permission);
	}

	/**
	 * Stop code from executing and send the player a message (see {@link #findPlayer(Event)})
	 * when he lacks the given permission
	 *
	 * @param permission
	 * @param falseMessage
	 */
	/*protected final void checkPerm(String permission, String falseMessage) {
		this.checkBoolean(this.findPlayer().hasPermission(permission), falseMessage.replace("{permission}", permission));
	}*/

	/**
	 * Stop code from executing and send the player a message (see {@link #findPlayer(Event)})
	 * when he lacks the given permission
	 *
	 * @param permission
	 * @param falseMessage
	 */
	protected final void checkPerm(String permission, Component falseMessage) {
		this.checkBoolean(this.findPlayer().hasPermission(permission), falseMessage.replaceText(b -> b.matchLiteral("{permission}").replacement(permission)));
	}

	/**
	 * Cancel the event and send the player a message (see {@link #findPlayer(Event)})
	 *
	 * @param messages
	 */
	protected final void cancel(Component messages) {
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
	protected final void returnTell(Component messages) {
		throw new EventHandledException(false, messages);
	}

	/**
	 * A shortcut for registering this event in Bukkit
	 */
	public final void register() {
		Bukkit.getPluginManager().registerEvent(this.eventClass, this, this.priority, this, SimplePlugin.getInstance(), this.ignoreCancelled);
	}
}
