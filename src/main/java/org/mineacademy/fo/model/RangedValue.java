package org.mineacademy.fo.model;

import org.apache.commons.lang.math.NumberUtils;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;

import lombok.Getter;

/**
 * A class holding a minimum and a maximum
 */
@Getter
public final class RangedValue {

	/**
	 * The minimum
	 */
	private final Number min;

	/**
	 * The maximum
	 */
	private final Number max;

	/**
	 * Create a new static value.
	 *
	 * @param value the value
	 */
	public RangedValue(Number value) {
		this(value, value);
	}

	/**
	 * Create a new ranged value.
	 *
	 * @param min the minimum value
	 * @param max the maximum value
	 */
	public RangedValue(Number min, Number max) {
		Valid.checkBoolean(min.longValue() >= 0 && max.longValue() >= 0, "Values may not be negative");
		Valid.checkBoolean(min.longValue() <= max.longValue(), "Minimum must be lower or equal maximum");

		this.min = min;
		this.max = max;
	}

	/**
	 * Get the minimum as an integer
	 */
	public int getMinInt() {
		return min.intValue();
	}

	/**
	 * Get the maximum as an integer
	 */
	public int getMaxInt() {
		return max.intValue();
	}

	/**
	 * Get the minimum as an long
	 */
	public long getMinLong() {
		return min.longValue();
	}

	/**
	 * Get the maximum as an long
	 */
	public long getMaxLong() {
		return max.longValue();
	}

	/**
	 * Get if the number is within the bounds
	 *
	 * @param value the number to compare
	 * @return
	 */
	public boolean isInRangeLong(long value) {
		return value >= min.longValue() && value <= max.longValue();
	}

	/**
	 * Get if the number is within the bounds
	 *
	 * @param value the number to compare
	 * @return
	 */
	public boolean isInRangeDouble(double value) {
		return value >= min.doubleValue() && value <= max.doubleValue();
	}

	/**
	 * Get a value in range between {@link #min} and {@link #max}
	 *
	 * @return a random value
	 */
	public int getRandomInt() {
		return RandomUtil.nextBetween(getMinInt(), getMaxInt());
	}

	/**
	 * Return whether {@link #min} equals {@link #max}
	 *
	 * @return
	 */
	public boolean isStatic() {
		return min == max;
	}

	/**
	 * Return a saveable representation (assuming saving in ticks) of this value
	 *
	 * @return
	 */
	public String toLine() {
		return min.intValue() + " - " + max.intValue();
	}

	/**
	 * Create a {@link RangedValue} from a line
	 * Example: 1-10
	 * 5 - 60
	 * 4
	 * <p>
	 * or
	 * <p>
	 * 10 seconds - 20 minutes (will be converted to seconds)
	 */
	public static RangedValue parse(String line) {
		final String[] parts = line.split("\\-");
		Valid.checkBoolean(parts.length == 1 || parts.length == 2, "Malformed value " + line);

		final String first = parts[0].trim();
		final String second = parts.length == 2 ? parts[1].trim() : "";

		// Check if valid numbers
		Valid.checkBoolean(NumberUtils.isNumber(first),
				"Invalid ranged value 1. input: '" + first + "' from line: '" + line + "'. RangedValue no longer accepts human natural format, for this, use RangedSimpleTime instead.");

		Valid.checkBoolean(NumberUtils.isNumber(second),
				"Invalid ranged value 2. input: '" + second + "' from line: '" + line + "'. RangedValue no longer accepts human natural format, for this, use RangedSimpleTime instead.");

		final Number firstNumber = first.contains(".") ? Double.parseDouble(first) : Long.parseLong(first);
		final Number secondNumber = second.contains(".") ? Double.parseDouble(second) : Long.parseLong(second);

		// Check if 1<2
		if (first.contains("."))
			Valid.checkBoolean(firstNumber.longValue() <= secondNumber.longValue(),
					"First number cannot be greater than second: " + firstNumber.longValue() + " vs " + secondNumber.longValue() + " in " + line);

		else
			Valid.checkBoolean(firstNumber.doubleValue() <= secondNumber.doubleValue(),
					"First number cannot be greater than second: " + firstNumber.doubleValue() + " vs " + secondNumber.doubleValue() + " in " + line);

		return new RangedValue(firstNumber, secondNumber);
	}

	@Override
	public String toString() {
		return isStatic() ? min.longValue() + "" : min.longValue() + " - " + max.longValue();
	}
}
