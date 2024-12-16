package org.mineacademy.fo.command;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.mineacademy.fo.platform.Platform;

/**
 * Represents a Bukkit command.
 *
 * @deprecated internal use only
 */
@Deprecated
public final class BukkitCommandImpl extends Command {

	/**
	 * The delegated command.
	 */
	private final SimpleCommandCore delegate;

	/**
	 * Wrap a Foundation command into a Bukkit command.
	 *
	 * @param delegate
	 */
	public BukkitCommandImpl(final SimpleCommandCore delegate) {
		super(delegate.getLabel());

		// Delegate settings
		if (delegate.getAliases() != null)
			this.setAliases(delegate.getAliases());

		if (delegate.getUsage() != null)
			this.setUsage(delegate.getUsage().toLegacySection(null));

		if (delegate.getDescription() != null)
			this.setDescription(delegate.getDescription().toLegacySection(null));

		this.delegate = delegate;
	}

	/**
	 * Delegates execution to the Foundation command.
	 */
	@Override
	public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
		this.delegate.delegateExecute(Platform.toPlayer(sender), commandLabel, args);

		return true;
	}

	/**
	 * Delegates tab completion to the Foundation command.
	 */
	@Override
	public List<String> tabComplete(final CommandSender sender, final String label, final String[] args) throws IllegalArgumentException {
		return this.delegate.delegateTabComplete(Platform.toPlayer(sender), label, args);
	}
}
