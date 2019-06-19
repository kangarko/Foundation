package org.mineacademy.fo.plugin;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.event.SimpleListener;

/**
 * A simple way of registering events or running tasks that
 * are cancelled automatically when the plugin is reloaded.
 */
public final class Reloadables {

	/**
	 * A list of currently enabled event listeners
	 */
	private final StrictList<Listener> listeners = new StrictList<>();

	/**
	 * Currently running tasks
	 */
	private final StrictList<BukkitTask> tasks = new StrictList<>();

	// -------------------------------------------------------------------------------------------
	// Main
	// -------------------------------------------------------------------------------------------

	/**
	 * Remove all listeners and cancel all running tasks
	 */
	public void reload() {
		listeners.forEach((l) -> {
			if (l instanceof ReloadableListener)
				((ReloadableListener) l).reload();

			HandlerList.unregisterAll(l);
		});

		listeners.clear();

		tasks.forEach((t) -> {
			t.cancel();
		});

		tasks.clear();
	}

	// -------------------------------------------------------------------------------------------
	// Tasks
	// -------------------------------------------------------------------------------------------

	/**
	 * Run a task timer at the scheduled repeatTicks rate
	 *
	 * @param repeatTicks
	 * @param runnable
	 */
	public void runTimer(int repeatTicks, Runnable runnable) {
		runTimer(0, repeatTicks, runnable);
	}

	/**
	 * Run a task timer at the scheduled repeatTicks rate waiting delayTicks before first run
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param runnable
	 */
	public void runTimer(int delayTicks, int repeatTicks, Runnable runnable) {
		final BukkitTask task = Common.runTimer(delayTicks, repeatTicks, runnable);

		if (task != null) // Plugin is not being disabled
			tasks.add(task);
	}

	/**
	 * Run a task timer asynchronously at the scheduled repeatTicks rate
	 *
	 * @param repeatTicks
	 * @param runnable
	 */
	public void runTimerAsync(int repeatTicks, Runnable runnable) {
		runTimerAsync(0, repeatTicks, runnable);
	}

	/**
	 * Run a task timer asynchronously at the scheduled repeatTicks
	 * rate waiting delayTicks before first run
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param runnable
	 */
	public void runTimerAsync(int delayTicks, int repeatTicks, Runnable runnable) {
		final BukkitTask task = Common.runTimerAsync(delayTicks, repeatTicks, runnable);

		if (task != null) // Plugin is not being disabled
			tasks.add(task);
	}

	// -------------------------------------------------------------------------------------------
	// Events / Listeners
	// -------------------------------------------------------------------------------------------

	/**
	 * Registers Bukkit events if the condition is true
	 *
	 * @param listener
	 * @param conditions
	 */
	public void registerEventsIf(Listener listener, boolean conditions) {
		if (conditions)
			registerEvents(listener);
	}

	/**
	 * Register events to Bukkit
	 *
	 * @param listener
	 */
	public void registerEvents(Listener listener) {
		Common.registerEvents(listener);

		listeners.add(listener);
	}

	/**
	 * Register events using our listener if the condition is true
	 *
	 * @param <T>
	 * @param listener
	 * @param condition
	 */
	public <T extends Event> void registerEventsIf(SimpleListener<T> listener, boolean condition) {
		if (condition)
			registerEvents(listener);
	}

	/**
	 * Register events to Bukkit using our listener
	 *
	 * @param <T>
	 * @param listener
	 */
	public <T extends Event> void registerEvents(SimpleListener<T> listener) {
		listener.register();

		listeners.add(listener);
	}
}
