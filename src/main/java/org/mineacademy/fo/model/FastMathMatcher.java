package org.mineacademy.fo.model;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * A fast math matcher that compares a number against a limit
 * Example: 3 > 2, 4 <= 5, 1 == 1
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FastMathMatcher implements ConfigSerializable {

	/**
	 * The mode to compare:
	 * 1 ==, 2 >, 3 >=, 4 <, 5 <=
	 */
	private final int mode;

	/**
	 * The limit to compare against
	 */
	private final int limit;

	/**
	 * Return true if the given number is within the limit
	 *
	 * @param number
	 * @return
	 */
	public boolean isInLimit(int number) {
		switch (mode) {
			case 1:
				return number == this.limit;
			case 2:
				return number > this.limit;
			case 3:
				return number >= this.limit;
			case 4:
				return number < this.limit;
			case 5:
				return number <= this.limit;
		}

		return false;
	}

	@Override
	public String toString() {
		return "FastMathMatcher{mode=" + this.mode + ", limit=" + this.limit + "}";
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray("mode", this.mode, "limit", this.limit);
	}

	/**
	 * Deserialize the matcher
	 *
	 * @param map
	 * @return
	 */
	public static FastMathMatcher deserialize(SerializedMap map) {
		final int mode = map.getInteger("mode");
		final int limit = map.getInteger("limit");

		return new FastMathMatcher(mode, limit);
	}

	/**
	 * Compile the given input into a matcher
	 *
	 * @param input
	 * @return
	 */
	public static FastMathMatcher compile(String input) {
		final String[] parts = input.split(" ");

		if (parts.length == 1) {
			int limit = 0;

			try {
				limit = Integer.parseInt(parts[0]);
			} catch (final NumberFormatException ex) {
				throw new IllegalArgumentException("Invalid number: " + parts[0]);
			}

			return new FastMathMatcher(1, limit);
		}

		Valid.checkBoolean(parts.length == 2, "Invalid syntax. Expected: <mode> <number> (i.e. '< 3'). Got: " + input);

		final String mode = parts[0];
		final int limit;

		try {
			limit = Integer.parseInt(parts[1]);

		} catch (final NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid number: " + parts[1]);
		}

		switch (mode) {
			case "==":
				return new FastMathMatcher(1, limit);
			case ">":
				return new FastMathMatcher(2, limit);
			case ">=":
				return new FastMathMatcher(3, limit);
			case "<":
				return new FastMathMatcher(4, limit);
			case "<=":
				return new FastMathMatcher(5, limit);
			default:
				throw new IllegalArgumentException("No such mode '" + mode + "'. Available modes: ==, >, >=, < and <=");
		}
	}
}
