package org.mineacademy.fo.remain;

import org.mineacademy.fo.Common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A wrapper for BarColor from bukkit
 */
@RequiredArgsConstructor
public enum CompBarColor {

	PINK("PINK"),
	BLUE("BLUE"),
	RED("RED"),
	GREEN("GREEN"),
	YELLOW("YELLOW"),
	PURPLE("PURPLE"),
	WHITE("WHITE");

	@Getter
	private final String key;

	/**
	 * Attempt to load CompBarColor from the given key
	 *
	 * @param key
	 * @return
	 */
	public static CompBarColor fromKey(String key) {
		for (final CompBarColor mode : values())
			if (mode.key.equalsIgnoreCase(key))
				return mode;

		throw new IllegalArgumentException("No such CompBarColor: " + key + ". Available: " + Common.join(values()));
	}

	@Override
	public String toString() {
		return this.key;
	}
}