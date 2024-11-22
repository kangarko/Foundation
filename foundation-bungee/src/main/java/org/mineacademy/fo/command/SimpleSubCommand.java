package org.mineacademy.fo.command;

/**
 * A Bukkit implementation of {@link SimpleSubCommandCore} with shared methods from {@link SharedBungeeCommandCore}.
 */
public abstract class SimpleSubCommand extends SimpleSubCommandCore implements SharedBungeeCommandCore {

	/**
	 * @see SimpleSubCommandCore#SimpleSubCommand(SimpleCommandGroup, String)
	 *
	 * @param parent
	 * @param sublabel
	 */
	protected SimpleSubCommand(SimpleCommandGroup parent, String sublabel) {
		super(parent, sublabel);
	}

	/**
	 * @see SimpleSubCommandCore#SimpleSubCommand(String)
	 *
	 * @param sublabel
	 */
	protected SimpleSubCommand(String sublabel) {
		super(sublabel);
	}
}
