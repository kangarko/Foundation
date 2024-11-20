package org.mineacademy.fo.command;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.platform.Platform;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.TabExecutor;

/**
 * Represents a Bukkit command.
 *
 * @deprecated internal use only
 */
@Deprecated
public final class BungeeCommandImpl extends net.md_5.bungee.api.plugin.Command implements TabExecutor {

	/**
	 * The delegated command.
	 */
	private final SimpleCommandCore delegate;

	/**
	 * Wrap a Foundation command into a Bukkit command.
	 *
	 * @param delegate
	 */
	public BungeeCommandImpl(SimpleCommandCore delegate) {
		super(delegate.getLabel(), null /* we check for perm in the delegate so it's null here */, Common.toArray(delegate.getAliases()));

		this.delegate = delegate;
	}

	/**
	 * Delegates execution to the Foundation command.
	 */
	@Override
	public void execute(CommandSender sender, String[] args) {
		delegate.delegateExecute(Platform.toPlayer(sender), delegate.getLabel(), args);
	}

	/**
	 * Delegates tab completion to the Foundation command.
	 */
	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		return delegate.delegateTabComplete(Platform.toPlayer(sender), delegate.getLabel(), args);
	}
}
