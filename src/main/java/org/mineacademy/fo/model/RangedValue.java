package org.mineacademy.fo.model;

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
		Valid.checkBoolean(min.longValue() <= max.longValue(), "Minimum must be lower or equal maximum");

		this.min = min;
		this.max = max;
	}

	/**
	 * Get the minimum as double
	 * @return
	 */
	public double getMinDouble() {
		return this.min.doubleValue();
	}

	/**
	 * Get the maximum as double
	 * @return
	 */
	public double getMaxDouble() {
		return this.max.doubleValue();
	}

	/**
	 * Get the minimum as an long
	 * @return
	 */
	public long getMinLong() {
		return this.min.longValue();
	}

	/**
	 * Get the maximum as an long
	 * @return
	 */
	public long getMaxLong() {
		return this.max.longValue();
	}

	/**
	 * Get if the number is within the bounds
	 *
	 * @param value the number to compare
	 * @return
	 */
	public boolean isInRangeLong(long value) {
		return value >= this.min.longValue() && value <= this.max.longValue();
	}

	/**
	 * Get if the number is within the bounds
	 *
	 * @param value the number to compare
	 * @return
	 */
	public boolean isInRangeDouble(double value) {
		return value >= this.min.doubleValue() && value <= this.max.doubleValue();
	}

	/**
	 * Get a value in range between the two values we store in this class
	 *
	 * @return a random value
	 */
	public int getRandomInt() {
		return RandomUtil.nextBetween((int) this.getMinLong(), (int) this.getMaxLong());
	}

	/**
	 * Return whether the two values we store in this class are equal
	 *
	 * @return
	 */
	public boolean isStatic() {
		return this.min.longValue() == this.max.longValue();
	}

	/**
	 * Return a saveable representation (assuming saving in ticks) of this value
	 *
	 * @return
	 */
	public String toLine() {
		return this.min.longValue() + " - " + this.max.longValue();
	}

	/**
	 * Create a {@link RangedValue} from a line
	 * Example: 1-10
	 * 5 - 60
	 * -5 - 5
	 * -5 - -2
	 * 4
	 * @param line
	 * @return
	 */
	public static RangedValue parse(String line) {

		line = line.replace(" ", "").trim();

		boolean firstNegative = false;

		if (line.startsWith("-")) {
			firstNegative = true;

			line = line.substring(1);
		}

		String[] parts;
		final String[] split = line.split("\\-");

		final boolean secondNegative = split.length == 3;

		if (split.length == 1)
			parts = new String[] { (firstNegative ? "-" : "") + line };
		else
			parts = new String[] { (firstNegative ? "-" : "") + split[0], (secondNegative ? "-" + split[2] : split[1]) };

		Valid.checkBoolean(parts.length == 1 || parts.length == 2, "Malformed value " + line);

		final String first = parts[0].trim();
		final String second = parts.length == 2 ? parts[1].trim() : first;

		// Check if valid numbers
		Valid.checkBoolean(Valid.isNumber(first),
				"Invalid ranged value 1. input: '" + first + "' from line: '" + line + "'. RangedValue no longer accepts human natural format, for this, use RangedSimpleTime instead.");

		Valid.checkBoolean(Valid.isNumber(second),
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
		return this.isStatic() ? this.min.longValue() + "" : this.min.longValue() + " - " + this.max.longValue();
	}
}
