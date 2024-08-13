package org.mineacademy.fo.model;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the different first screens appearing in toast notifications,
 * you can use this in {@link Remain#sendToast(org.bukkit.entity.Player, String, CompMaterial, CompToastStyle)}
 */
@RequiredArgsConstructor
public enum CompToastStyle {

	TASK("task"),
	GOAL("goal"),
	CHALLENGE("challenge");

	@Getter
	private final String key;

	/**
	 * Attempt to load CompToastStyle from the given key
	 *
	 * @param key
	 * @return
	 */
	public static CompToastStyle fromKey(String key) {
		for (final CompToastStyle style : values())
			if (style.key.equalsIgnoreCase(key))
				return style;

		throw new IllegalArgumentException("No such CompToastStyle '" + key + "'. Available: " + CommonCore.join(values()));
	}

	@Override
	public String toString() {
		return this.key;
	}
}
