package org.mineacademy.fo.remain;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.debug.Debugger;

import lombok.Getter;

/**
 * This class is provided as an easy way to handle scheduling tasks.
 */
public abstract class CompRunnable implements Runnable {

	/**
	 * Wraps a delegate runnable and safely handles errors
	 */
	@Getter
	public static final class SafeRunnable implements Runnable {

		/*
		 * Internal counter how many tasks have been scheduled,
		 * this has nothing to do with Bukkit's task counter and
		 * does not reset upon plugin's reload!
		 */
		private static int scheduledTasks = 0;

		/**
		 * The actual runnable
		 */
		private final Runnable delegate;

		/**
		 * The classes who called this safe runnable
		 */
		private final List<String> source;

		/*
		 * The internal task id, this is not Bukkit's task id but our own!
		 */
		private final int taskId;

		/**
		 * Create a new runnable, wrapping the old one
		 */
		public SafeRunnable(Runnable delegate) {
			this.delegate = delegate;
			this.source = Debugger.traceRoute(true);
			this.taskId = ++scheduledTasks;

			// Remove the call to self and Common
			this.source.remove(0);
			this.source.remove(0);
		}

		/**
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			Debugger.debug("runnable", "Running task #" + taskId + " from " + Common.join(source));

			try {
				delegate.run();

			} catch (final Exception ex) {
				if (ex instanceof IllegalStateException && ex.getMessage() != null && (ex.getMessage().contains("Not scheduled yet") || ex.getMessage().contains("Already scheduled")))
					return;

				Common.error(ex, "Failed to execute scheduled task: " + ex);
			}
		}
	}

	private int taskId = -1;

	/**
	 * Attempts to cancel this task
	 * @throws IllegalStateException if task was not scheduled yet
	 */
	public synchronized void cancel() throws IllegalStateException {
		if (taskId != -1)
			Bukkit.getScheduler().cancelTask(getTaskId());
	}

	/**
	 * Schedules this in the Bukkit scheduler to run on next tick
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 * @see BukkitScheduler#runTask(Plugin, Runnable)
	 */
	public synchronized BukkitTask runTask(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
		checkState();

		return setupId(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this));
	}

	/**
	 * <b>Asynchronous tasks should never access any API in Bukkit.
	 *    Great care should be taken to assure the thread-safety of asynchronous tasks.</b>
	 * <br>
	 * <br>Schedules this in the Bukkit scheduler to run asynchronously.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 * @see BukkitScheduler#runTaskAsynchronously(Plugin, Runnable, long, long)
	 */
	public synchronized BukkitTask runTaskAsynchronously(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
		checkState();

		return setupId(Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, this));
	}

	/**
	 * Schedules this to run after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param task the task to be run
	 * @param delay the ticks to wait before running the task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 * @see BukkitScheduler#runTaskLater(Plugin, Runnable, long, long)
	 */
	public synchronized BukkitTask runTaskLater(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		checkState();

		return setupId(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this, delay));
	}

	/**
	 * <b>Asynchronous tasks should never access any API in Bukkit.
	 *    Great care should be taken to assure the thread-safety of asynchronous tasks.</b>
	 * <br>
	 * <br>Schedules this to run asynchronously after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 * @see BukkitScheduler#runTaskLaterAsynchronously(Plugin, Runnable, long, long)
	 */
	public synchronized BukkitTask runTaskLaterAsynchronously(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		checkState();

		return setupId(Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, this, delay));
	}

	/**
	 * Schedules this to repeatedly run until cancelled, starting after the specified number of server ticks
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task
	 * @param period the ticks to wait between runs
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 * @see BukkitScheduler#runTaskTimer(Plugin, Runnable, long, long)
	 */
	public synchronized BukkitTask runTaskTimer(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		checkState();

		return setupId(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, delay, period));
	}

	/**
	 * <b>Asynchronous tasks should never access any API in Bukkit.
	 *    Great care should be taken to assure the thread-safety of asynchronous tasks.</b>
	 * <br>
	 * <br>Schedules this to repeatedly run asynchronously until cancelled, starting after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task for the first time
	 * @param period the ticks to wait between runs
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 * @see BukkitScheduler#runTaskTimerAsynchronously(Plugin, Runnable, long, long)
	 */
	public synchronized BukkitTask runTaskTimerAsynchronously(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		checkState();

		return setupId(Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, this, delay, period));
	}

	/**
	 * Gets the task id for this runnable
	 * @return the task id that this runnable was scheduled as
	 * @throws IllegalStateException if task was not scheduled yet
	 */
	public synchronized int getTaskId() throws IllegalStateException {
		final int id = taskId;
		if (id == -1) {
			throw new IllegalStateException("Not scheduled yet");
		}
		return id;
	}

	private void checkState() {
		if (taskId != -1) {
			throw new IllegalStateException("Already scheduled as " + taskId);
		}
	}

	private BukkitTask setupId(final int taskId) {
		this.taskId = taskId;

		for (final BukkitTask task : Bukkit.getScheduler().getPendingTasks())
			if (task.getTaskId() == taskId)
				return task;

		// TODO fix for MC 1.2.5
		return null;
	}
}