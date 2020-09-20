package org.mineacademy.fo.model;

import org.mineacademy.fo.TimeUtil;

import lombok.Getter;
import lombok.NonNull;

/**
 * A simple class holding time values in human readable form such as 1 second or 5 minutes
 */
@Getter
public class SimpleTime {

	private final String raw;
	private final int timeTicks;

	protected SimpleTime(@NonNull final String time) {
		raw = time;
		timeTicks = (int) TimeUtil.toTicks(time);
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
	 *
	 * @param time
	 * @return
	 */
	public static SimpleTime from(final String time) {
		return new SimpleTime(time);
	}

	/**
	 * Get the time specified in seconds (ticks / 20)
	 *
	 * @return
	 */
	public int getTimeSeconds() {
		return timeTicks / 20;
	}

	/**
	 * Get the time specified in ticks
	 *
	 * @return
	 */
	public int getTimeTicks() {
		return timeTicks;
	}

	@Override
	public String toString() {
		return raw;
	}
}