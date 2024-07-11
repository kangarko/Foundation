package org.mineacademy.fo.model;

import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.platform.Platform;

/**
 * A Folia-compatible Bukkit Runnable alternative.
 */
public abstract class SimpleRunnable implements Runnable {

	private Task task;

	public final synchronized void cancel() throws IllegalStateException {
		checkScheduled();

		task.cancel();
	}

	/**
	 * Schedules this in the scheduler to run on next tick.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @return
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized Task runTask(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Platform.runTask(this));
	}

	/**
	 * Schedules this in the scheduler to run on next tick async.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @return
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized Task runTaskAsync(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Platform.runTaskAsync(this));
	}

	/**
	 * Schedules this to run after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay  the ticks to wait before running the task
	 * @return
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized Task runTaskLater(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Platform.runTask((int) delay, this));
	}

	/**
	 * Schedules this to run after the specified number of server ticks async.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay  the ticks to wait before running the task
	 * @return
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized Task runTaskLaterAsynchronously(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Platform.runTaskAsync((int) delay, this));
	}

	/**
	 * Schedules this to run after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task initially
	 * @param period the ticks to wait before running the task again
	 *
	 * @return
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized Task runTaskTimer(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Platform.runTaskTimer((int) delay, (int) period, this));
	}

	/**
	 * Schedules this to run after the specified number of server ticks async.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task initially
	 * @param period the ticks to wait before running the task again
	 *
	 * @return
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException    if this was already scheduled
	 */
	public final synchronized Task runTaskTimerAsynchronously(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Platform.runTaskTimerAsync((int) delay, (int) period, this));
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
	public Task setupTask(final Task task) {
		this.task = task;

		return task;
	}

}