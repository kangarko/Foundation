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
		super(delegate.getLabel(), delegate.getPermission(), Common.toArray(delegate.getAliases()));

		this.delegate = delegate;

		super.setPermissionMessage(delegate.getPermissionMessage().toLegacy());
	}

	/**
	 * Delegates execution to the Foundation command.
	 */
	@Override
	public void execute(CommandSender sender, String[] args) {
		delegate.delegateExecute(Platform.toPlayer(sender), args[0], this.parseArgs(args));
	}

	/**
	 * Delegates tab completion to the Foundation command.
	 */
	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		return delegate.delegateTabComplete(Platform.toPlayer(sender), args[0], this.parseArgs(args));
	}

	/**
	 * Delegates permission check to the Foundation.
	 */
	@Override
	public boolean hasPermission(CommandSender sender) {
		final String permission = this.delegate.getPermission();

		return permission == null || sender.hasPermission(permission);
	}

	/*
	 * Helper method to parse the arguments.
	 */
	private String[] parseArgs(String[] args) {
		final String[] actualArgs = args.length > 1 ? new String[args.length - 1] : new String[0];

		if (args.length > 1)
			System.arraycopy(args, 1, actualArgs, 0, args.length - 1);

		return actualArgs;
	}
}
