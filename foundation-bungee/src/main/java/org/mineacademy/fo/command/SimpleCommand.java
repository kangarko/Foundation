package org.mineacademy.fo.command;

import java.util.List;

/**
 * A Bukkit implementation of {@link SimpleCommandCore} with shared methods from {@link SharedBungeeCommandCore}.
 */
public abstract class SimpleCommand extends SimpleCommandCore implements SharedBungeeCommandCore {

	/**
	 * @see SimpleCommandCore#SimpleCommand(List)
	 *
	 * @param labelAndAliases
	 */
	protected SimpleCommand(List<String> labelAndAliases) {
		super(labelAndAliases);
	}

	/**
	 * @see SimpleCommandCore#SimpleCommand(String)
	 *
	 * @param label
	 */
	protected SimpleCommand(String label) {
		super(label);
	}

	/**
	 * @see SimpleCommandCore#SimpleCommand(String, List)
	 *
	 * @param label
	 * @param aliases
	 */
	protected SimpleCommand(String label, List<String> aliases) {
		super(label, aliases);
	}
}
