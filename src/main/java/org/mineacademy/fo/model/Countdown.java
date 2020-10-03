package org.mineacademy.fo.model;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Represents a runnable timer task that counts down to 0 and stops
 */
public abstract class Countdown implements Runnable {

	/**
	 * How long to wait before starting this countdown (in ticks)?
	 * <p>
	 * Set to 1 second.
	 */
	private static final int START_DELAY = 20;

	/**
	 * How long to wait before ticking the next count (in ticks)?
	 * <p>
	 * Set to 1 second.
	 */
	private static final int TICK_PERIOD = 20;

	/**
	 * The time in seconds we are counting down from
	 */
	@Getter
	private final int countdownSeconds;

	/**
	 * How many seconds have passed since the start ?
	 */
	@Getter(AccessLevel.PROTECTED)
	private int secondsSinceStart = 0;

	/**
	 * The internal task from Bukkit associated with this countdown
	 */
	private int taskId = -1;

	/**
	 * Create new countdown from the given time
	 *
	 * @param time
	 */
	protected Countdown(final SimpleTime time) {
		this((int) time.getTimeSeconds());
	}

	/**
	 * Create new countdown
	 *
	 * @param countdownSeconds
	 */
	protected Countdown(final int countdownSeconds) {
		this.countdownSeconds = countdownSeconds;
	}

	@Override
	public final void run() {
		secondsSinceStart++;

		if (secondsSinceStart < countdownSeconds)
			try {
				onTick();

			} catch (final Throwable t) {
				try {
					onTickError(t);
				} catch (final Throwable tt) {
					Common.log("Unable to handle onTickError, got " + t + ": " + tt.getMessage());
				}

				Common.error(t,
						"Error in countdown!",
						"Seconds since start: " + secondsSinceStart,
						"Counting till: " + countdownSeconds,
						"%error");
			}
		else {
			cancel();
			onEnd();
		}
	}

	/**
	 * Called when this countdown is launched
	 */
	protected void onStart() {
	}

	/**
	 * Called on each tick (by default each second) till we count down to 0
	 */
	protected abstract void onTick();

	/**
	 * Called when the clock hits the final 0 and stops.
	 */
	protected abstract void onEnd();

	/**
	 * Called when the {@link #onTick()} method throws an error (we already log the error)
	 *
	 * @param t
	 */
	protected void onTickError(final Throwable t) {
	}

	/**
	 * Return the time left in seconds
	 *
	 * @return
	 */
	public int getTimeLeft() {
		return countdownSeconds - secondsSinceStart;
	}

	/**
	 * Starts this countdown failing if it is already running
	 */
	public final void launch() {
		Valid.checkBoolean(!isRunning(), "Task " + this + " already scheduled!");

		final BukkitTask task = Bukkit.getScheduler().runTaskTimer(SimplePlugin.getInstance(), this, START_DELAY, TICK_PERIOD);
		taskId = task.getTaskId();

		onStart();
	}

	/**
	 * Cancels this countdown, failing if it is not scheduled (use {@link #isRunning()})
	 */
	public final void cancel() {
		Bukkit.getScheduler().cancelTask(getTaskId());

		taskId = -1;
		secondsSinceStart = 0;
	}

	/**
	 * Return true if this countdown is running
	 *
	 * @return
	 */
	public final boolean isRunning() {
		return taskId != -1;
	}

	/**
	 * Return the bukkit task or fails if not running
	 *
	 * @return
	 */
	public final int getTaskId() {
		Valid.checkBoolean(isRunning(), "Task " + this + " not scheduled yet");

		return taskId;
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "{" + countdownSeconds + ", id=" + taskId + "}";
	}
}
