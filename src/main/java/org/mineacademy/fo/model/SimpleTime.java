package org.mineacademy.fo.model;

import org.mineacademy.fo.TimeUtil;

import lombok.Getter;
import lombok.NonNull;

/**
 * A simple class holding time values in human readable form such as 1 second or 5 minutes
 */
@Getter
public final class SimpleTime {

	private final String raw;
	private final long timeTicks;
	private final boolean enabled;

	protected SimpleTime(@NonNull final String time) {
		if ("0".equals(time) || "none".equalsIgnoreCase(time)) {
			this.raw = "0";
			this.timeTicks = 0;
			this.enabled = false;

		} else {
			this.raw = time;
			this.timeTicks = TimeUtil.toTicks(time);
			this.enabled = true;
		}
	}

	/**
	 * Get the time specified in seconds (ticks / 20)
	 *
	 * @return
	 */
	public long getTimeSeconds() {
		return this.timeTicks / 20L;
	}

	/**
	 * Get the time specified in ticks
	 * *WARNING* if the time ticks is over {@value Integer#MAX_VALUE} it will overflow!
	 *
	 * @return
	 */
	public int getTimeTicks() {
		return (int) this.timeTicks;
	}

	/**
	 * Get the time specified in ms (ticks * 20)
	 *
	 * @return
	 */
	public long getTimeMilliseconds() {
		return this.timeTicks * 50;
	}

	/**
	 * Return the human readable representation of this time, such as 69 seconds (no pun intended)
	 *
	 * @return
	 */
	public String getRaw() {
		return this.timeTicks == 0 ? "0" : this.raw;
	}

	/**
	 * Return true if the given limit has already passed from the current timestamp
	 *
	 * @param limitMs
	 * @return
	 */
	public boolean isOverLimitMs(final long limitMs) {
		return (System.currentTimeMillis() - limitMs) > this.getTimeMilliseconds();
	}

	/**
	 * Return true if the given limit has not yet passed from the current timestamp
	 *
	 * @param limitMs
	 * @return
	 */
	public boolean isUnderLimitMs(final long limitMs) {
		return (System.currentTimeMillis() - limitMs) < this.getTimeMilliseconds();
	}

	/**
	 * Format the time left until the given limit in human readable form
	 *
	 * @param lastLastExecutionMs
	 * @return
	 */
	public String formatWaitTime(final long lastLastExecutionMs) {
		final long limit = this.getTimeMilliseconds();
		final long delay = System.currentTimeMillis() - lastLastExecutionMs;

		return TimeUtil.formatTimeGeneric(1 + (limit - delay) / 1000L);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof SimpleTime && ((SimpleTime) obj).timeTicks == this.timeTicks;
	}

	@Override
	public String toString() {
		return this.getRaw();
	}

	/**
	 * Generate new time from the given seconds
	 *
	 * @param seconds
	 * @return
	 */
	public static SimpleTime fromSeconds(final int seconds) {
		return from(seconds + " seconds");
	}

	/**
	 * Generate new time. Valid examples: 15 ticks 1 second 25 minutes 3 hours etc.
	 * or input "none" to create an instance with the time of 0
	 *
	 * @param time
	 * @return
	 */
	public static SimpleTime from(final String time) {
		return new SimpleTime(time);
	}
}