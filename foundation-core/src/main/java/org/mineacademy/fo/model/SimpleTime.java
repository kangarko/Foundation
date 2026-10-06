package org.mineacademy.fo.model;

import org.mineacademy.fo.TimeUtil;

import lombok.Getter;
import lombok.NonNull;

/**
 * A simple class holding time values in human readable form such as 1 second or 5 minutes.
 */
@Getter
public final class SimpleTime implements ConfigStringSerializable {

	/**
	 * The raw time string such as 1 second or 5 minutes
	 */
	private final String raw;

	/**
	 * The time in ticks.
	 */
	private final long timeTicks;

	/**
	 * Is this class enabled?
	 */
	private final boolean enabled;

	/*
	 * Create a new time instance.
	 */
    private SimpleTime(@NonNull final String time) {
        final String normalized = time.trim();

        if ("0".equals(normalized) || "none".equalsIgnoreCase(normalized) || "never".equalsIgnoreCase(normalized)) {
            this.raw       = "0";
            this.timeTicks = 0;
            this.enabled   = false;

        } else {
            this.raw       = time;
            this.timeTicks = TimeUtil.toTicks(time);

            if (this.timeTicks > Long.MAX_VALUE / 50)
                throw new IllegalArgumentException("Time is too large to express in milliseconds: '" + time + "'");

            this.enabled = this.timeTicks > 0;
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
	 *
	 * @return
	 */
    public int getTimeTicks() {
        if (this.timeTicks > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Time is too large to express in integer ticks: '" + this.raw + "'");

        return (int) this.timeTicks;
    }

	/**
	 * Get the time specified in ms (ticks * 50)
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
		return System.currentTimeMillis() - limitMs > this.getTimeMilliseconds();
	}

	/**
	 * Return true if the given limit has not yet passed from the current timestamp
	 *
	 * @param limitMs
	 * @return
	 */
	public boolean isUnderLimitMs(final long limitMs) {
		return System.currentTimeMillis() - limitMs < this.getTimeMilliseconds();
	}

	/**
	 * Calculate and format the remaining wait time in a human-readable format.
	 *
	 * <p>This method computes how much time is left until the next execution is allowed
	 * based on the last execution timestamp and the time limit, and formats the result.</p>
	 *
	 * @param lastLastExecutionMs The timestamp (in milliseconds) of the last execution.
	 *
	 * @return A formatted string representing the remaining time, in seconds, until the next execution.
	 *
	 * <p><b>Example Usage:</b></p>
	 * <pre>{@code
	 * long lastExecution = System.currentTimeMillis() - 5000L;
	 * String waitTime = getFormattedRemainingWaitTime(lastExecution);
	 * print "Remaining wait time: " + waitTime
	 * }</pre>
	 *
	 * <p>For example, if the time limit is 10 seconds and the last execution was 5 seconds ago,
	 * it will return a string representing 5 seconds.</p>
	 */

	public String getFormattedWaitTime(final long lastLastExecutionMs) {
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
    public int hashCode() {
        return Long.hashCode(this.timeTicks);
    }

	/**
	 * @see #serialize()
	 */
	@Override
	public String toString() {
		return this.serialize();
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public String serialize() {
		return this.getRaw();
	}

	/**
	 * Generate new time from the given seconds.
	 *
	 * @param seconds
	 * @return
	 */
	public static SimpleTime fromSeconds(final int seconds) {
		return fromString(seconds + " seconds");
	}

	/**
	 * Generate new time. Valid examples: 15 ticks 1 second 25 minutes 3 hours etc.
	 * or input "none" to create an instance with the time of 0.
	 *
	 * @param time
	 * @return
	 */
	public static SimpleTime fromString(final String time) {
		return new SimpleTime(time);
	}
}
