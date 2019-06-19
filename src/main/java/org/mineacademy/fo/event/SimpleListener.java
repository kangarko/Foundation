package org.mineacademy.fo.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.mineacademy.fo.plugin.SimplePlugin;

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
	private final Class<T> event;

	/**
	 * The event priority
	 */
	private final EventPriority priority;

	/**
	 * Shall we ignore cancelled events down the pipeline?
	 */
	private final boolean ignoreCancelled;

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
		execute((T) event);
	}

	/**
	 * Executes when the event is run
	 *
	 * @param event
	 */
	public abstract void execute(T event);

	public final void register() {
		Bukkit.getPluginManager().registerEvent(event, this, priority, this, SimplePlugin.getInstance(), ignoreCancelled);
	}
}
