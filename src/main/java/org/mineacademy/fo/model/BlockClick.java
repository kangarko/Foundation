package org.mineacademy.fo.model;

import org.bukkit.event.block.Action;
import org.mineacademy.fo.Valid;

/**
 * Used when player has clicked on a block.
 */
public enum BlockClick {

	/**
	 * Represents the right block click
	 */
	RIGHT_CLICK,

	/**
	 * Represents the left block click
	 */
	LEFT_CLICK;

	/**
	 * Parses Bukkit action into our block click, failing if it is not a block-related click
	 *
	 * @param action
	 * @return
	 */
	public static BlockClick fromAction(Action action) {
		final BlockClick click = BlockClick.valueOf(action.toString().replace("_BLOCK", ""));
		Valid.checkNotNull(click, "Report / Unsupported click type from " + action);

		return click;
	}
}
