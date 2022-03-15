package org.mineacademy.fo.remain;

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
}
