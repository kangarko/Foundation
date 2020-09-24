package org.mineacademy.fo.remain;

import org.mineacademy.fo.Common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A wrapper for BarStyle from bukkit
 */
@RequiredArgsConstructor
public enum CompBarStyle {

	/**
	 * Makes the boss bar solid (no segments)
	 */
	SOLID("SOLID"),

	/**
	 * Splits the boss bar into 6 segments
	 */
	SEGMENTED_6("SEGMENTED_6"),

	/**
	 * Splits the boss bar into 10 segments
	 */
	SEGMENTED_10("SEGMENTED_10"),

	/**
	 * Splits the boss bar into 12 segments
	 */
	SEGMENTED_12("SEGMENTED_12"),

	/**
	 * Splits the boss bar into 20 segments
	 */
	SEGMENTED_20("SEGMENTED_20");

	@Getter
	private final String key;

	/**
	 * Attempt to load CompBarStyle from the given key
	 *
	 * @param key
	 * @return
	 */
	public static CompBarStyle fromKey(String key) {
		for (final CompBarStyle mode : values())
			if (mode.key.equalsIgnoreCase(key))
				return mode;

		throw new IllegalArgumentException("No such CompBarStyle: " + key + ". Available: " + Common.join(values()));
	}

	@Override
	public String toString() {
		return this.key;
	}
}