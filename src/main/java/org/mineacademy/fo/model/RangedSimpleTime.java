package org.mineacademy.fo.model;

import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;

import lombok.Getter;

/**
 * Represents two {@link SimpleTime} instances you can
 * extract a random time from.
 */
@Getter
public final class RangedSimpleTime {

	/**
	 * The minimum time
	 */
	private final SimpleTime min;

	/**
	 * The maximum time
	 */
	private final SimpleTime max;

	/**
	 * Create a new ranged time from the given time, effectively without any range
	 *
	 * @param time
	 */
	public RangedSimpleTime(final SimpleTime time) {
		this(time, time);
	}

	/**
	 * Create a new ranged time from the min and the max values
	 *
	 * @param min
	 * @param max
	 */
	public RangedSimpleTime(final SimpleTime min, final SimpleTime max) {
		Valid.checkBoolean(min.getTimeTicks() >= 0 && max.getTimeTicks() >= 0, "Values may not be negative");
		Valid.checkBoolean(min.getTimeTicks() <= max.getTimeTicks(), "Minimum must be lower or equal maximum");

		this.min = min;
		this.max = max;
	}

	/**
	 * Return a random time in ticks between min-max value
	 *
	 * @return
	 */
	public int getRandomTicks() {
		return RandomUtil.nextBetween(this.min.getTimeTicks(), this.max.getTimeTicks());
	}

	/**
	 * Return a random time in seconds between min-max value
	 *
	 * @return
	 */
	public int getRandomSeconds() {
		return RandomUtil.nextBetween((int) this.min.getTimeSeconds(), (int) this.max.getTimeSeconds());
	}

	/**
	 * Return if the given ticks are within min-max value
	 *
	 * @param ticks
	 * @return
	 */
	public boolean isInRangeTicks(final int ticks) {
		return ticks >= this.min.getTimeTicks() && ticks <= this.min.getTimeTicks();
	}

	/**
	 * Return if the given seconds are within min-max value
	 *
	 * @param seconds
	 * @return
	 */
	public boolean isInRangeSeconds(final int seconds) {
		return seconds >= this.min.getTimeSeconds() && seconds <= this.min.getTimeSeconds();
	}

	/**
	 * Return a formatted line such as '1 ticks - 2 minutes'
	 *
	 * @return
	 */
	public String toLine() {
		return (this.min.getRaw() + (this.min.equals(this.max) ? "" : " - " + this.max.getRaw())).replace("  ", " ");
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RangedSimpleTime{min=" + this.min + ", max=" + this.max + "}";
	}

	/**
	 * Parse a ranged time from the given line, such as "1 second - 2 minutes" etc.
	 *
	 * @param line
	 * @return
	 */
	public static RangedSimpleTime parse(final String line) {
		final String[] parts = line.split("\\-");
		Valid.checkBoolean(parts.length == 1 || parts.length == 2, "Malformed RangedSimpleTime " + line);

		final String min = parts[0].trim();
		final String max = (parts.length == 2 ? parts[1] : min).trim();

		return new RangedSimpleTime(SimpleTime.from(min), SimpleTime.from(max));
	}
}
