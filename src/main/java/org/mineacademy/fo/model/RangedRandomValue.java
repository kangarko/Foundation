package org.mineacademy.fo.model;

import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;

/**
 * Represents a numerical value in range.
 */
public final class RangedRandomValue extends RangedValue {

	/**
	 * Make a new ranged value with a fixed range
	 *
	 * @param value the fixed range
	 */
	public RangedRandomValue(final int value) {
		this(value, value);
	}

	/**
	 * Make a new ranged value between two numbers.
	 *
	 * @param min the ceiling
	 * @param max the floor
	 */
	public RangedRandomValue(final int min, final int max) {
		super(min, max);

		Valid.checkBoolean(min >= 0 && max >= 0, "Values may not be negative");
		Valid.checkBoolean(min <= max, "Minimum must be lower or equal maximum");
	}

	/**
	 * Create a {@link RangedValue} from a line
	 * Example: 1-10
	 * 5 - 60
	 * 4
	 */
	public static RangedRandomValue parse(final String line) {
		final RangedValue random = RangedValue.parse(line);
		System.out.println("Parsed values: " + random.getMinInt() + " to " + random.getMaxInt());

		return new RangedRandomValue(random.getMinInt(), random.getMaxInt());
	}

	/**
	 * Get a value in range between {@link #min} and {@link #max}
	 *
	 * @return a random value
	 */
	public int getRandom() {
		return RandomUtil.nextBetween(getMinInt(), getMaxInt());
	}

	/**
	 * Check if a value is between {@link #min} and {@link #max}
	 *
	 * @param value the value
	 * @return whether or not the value is in range
	 */
	public boolean isInRange(final int value) {
		return value >= getMinInt() && value <= getMaxInt();
	}
}
