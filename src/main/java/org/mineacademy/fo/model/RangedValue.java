package org.mineacademy.fo.model;

import org.apache.commons.lang.math.NumberUtils;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;

import lombok.Getter;

/**
 * A class holding a minimum and a maximum
 */
@Getter
public class RangedValue {

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
	public final int getMinInt() {
		return min.intValue();
	}

	/**
	 * Get the maximum as an integer
	 */
	public final int getMaxInt() {
		return max.intValue();
	}

	/**
	 * Get the minimum as an long
	 */
	public final long getMinLong() {
		return min.longValue();
	}

	/**
	 * Get the maximum as an long
	 */
	public final long getMaxLong() {
		return max.longValue();
	}

	/**
	 * Get if the number is within {@link #getMin()} and {@link #getMax()}
	 *
	 * @param value the number to compare
	 * @return if the number is within {@link #getMin()} and {@link #getMax()}
	 */
	public boolean isWithin(Number value) {
		return value.longValue() >= min.longValue() && value.longValue() <= max.longValue();
	}

	/**
	 * Return whether {@link #min} equals {@link #max}
	 * @return
	 */
	public final boolean isStatic() {
		return min == max;
	}

	/**
	 * Return a saveable representation (assuming saving in ticks) of this value
	 *
	 * @return
	 */
	public final String toLine() {
		return min + " - " + max;
	}

	/**
	 * Create a {@link RangedValue} from a line
	 * Example: 1-10
	 *          5 - 60
	 *          4
	 *
	 *          or
	 *
	 *          10 seconds - 20 minutes (will be converted to seconds)
	 *
	 */
	public static RangedValue parse(String line) {
		line = line.replace(" ", "");

		final String[] parts = line.split("\\-");
		Valid.checkBoolean(parts.length == 1 || parts.length == 2, "Malformed value " + line);

		final String first = parts[0];
		final Integer min = NumberUtils.isNumber(first) ? Integer.parseInt(first) : (int) (TimeUtil.toTicks(first) / 20);

		final String second = parts.length == 2 ? parts[1] : "";
		final Integer max = parts.length == 2 ? NumberUtils.isNumber(second) ? Integer.parseInt(second) : (int) (TimeUtil.toTicks(second) / 20) : min;
		Valid.checkBoolean(min != null && max != null, "Malformed value " + line);

		return new RangedValue(min, max);
	}

	@Override
	public final String toString() {
		return isStatic() ? min + "" : min + " - " + max;
	}
}
