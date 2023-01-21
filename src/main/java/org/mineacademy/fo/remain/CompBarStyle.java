package org.mineacademy.fo.remain;

import org.mineacademy.fo.Common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A wrapper class for BarStyle from Bukkit.
 */
@RequiredArgsConstructor
public enum CompBarStyle {

	/**
	 * Makes the boss bar solid (no segments).
	 */
	SOLID("SOLID", "SOLID"),

	/**
	 * Splits the boss bar into 6 segments.
	 */
	SEGMENTED_6("SEGMENTED_6", "SEG6"),

	/**
	 * Splits the boss bar into 10 segments.
	 */
	SEGMENTED_10("SEGMENTED_10", "SEG10"),

	/**
	 * Splits the boss bar into 12 segments.
	 */
	SEGMENTED_12("SEGMENTED_12", "SEG12"),

	/**
	 * Splits the boss bar into 20 segments.
	 */
	SEGMENTED_20("SEGMENTED_20", "SEG20");

	@Getter
	private final String key;

	@Getter
	private final String shortKey;

	/**
	 * Attempts to load a CompBarStyle from the given key.
	 *
	 * @param key
	 * @return
	 */
	public static CompBarStyle fromKey(String key) {
		for (final CompBarStyle mode : values())
			if (mode.key.equalsIgnoreCase(key) || mode.shortKey.equalsIgnoreCase(key))
				return mode;

		throw new IllegalArgumentException("No such CompBarStyle: " + key + ". Available: " + Common.join(values()));
	}

	@Override
	public String toString() {
		return this.key;
	}
}
