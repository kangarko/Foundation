package org.mineacademy.fo.model;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;

/**
 * A Folia-compatible Bukkit Runnable alternative.
 */
public abstract class SimpleRunnable implements Runnable {

	private BukkitTask task;

	public final synchronized void cancel() throws IllegalStateException {
		checkScheduled();

		task.cancel();
	}

	/**
	 * Schedules this in the scheduler to run on next tick.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized BukkitTask runTask(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runLater(this));
	}

	/**
	 * Schedules this in the scheduler to run on next tick async.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized BukkitTask runTaskAsync(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runAsync(this));
	}

	/**
	 * Schedules this to run after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay  the ticks to wait before running the task
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized BukkitTask runTaskLater(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runLater((int) delay, this));
	}

	/**
	 * Schedules this to run after the specified number of server ticks async.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay  the ticks to wait before running the task
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized BukkitTask runTaskLaterAsynchronously(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runLaterAsync((int) delay, this));
	}

	/**
	 * Schedules this to run after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task initially
	 * @param period the ticks to wait before running the task again
	 *
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized BukkitTask runTaskTimer(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runTimer((int) delay, (int) period, this));
	}

	/**
	 * Schedules this to run after the specified number of server ticks async.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task initially
	 * @param period the ticks to wait before running the task again
	 *
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized BukkitTask runTaskTimerAsynchronously(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runTimerAsync((int) delay, (int) period, this));
	}

	private void checkScheduled() {
		if (task == null)
			throw new IllegalStateException("Not scheduled yet");
	}

	private void checkNotYetScheduled() {
		if (task != null)
			throw new IllegalStateException("Already scheduled");
	}

	/**
	 * @deprecated internal use only
	 *
	 * @param task
	 * @return
	 */
	@Deprecated
	public BukkitTask setupTask(final BukkitTask task) {
		this.task = task;

		return task;
	}

}