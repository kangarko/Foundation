package org.mineacademy.fo.command;

import java.util.List;

import org.mineacademy.fo.platform.Platform;

/**
 * Represents a Bukkit command.
 *
 * @deprecated internal use only
 */
@Deprecated
public final class VelocityCommandImpl implements com.velocitypowered.api.command.SimpleCommand {

	/**
	 * The delegated command.
	 */
	private final SimpleCommandCore delegate;

	/**
	 * Wrap a Foundation command into a Bukkit command.
	 *
	 * @param delegate
	 */
	@Deprecated
	public VelocityCommandImpl(final SimpleCommandCore delegate) {
		this.delegate = delegate;
	}

	/**
	 * Delegates execution to the Foundation command.
	 */
	@Deprecated
	@Override
	public void execute(final Invocation invocation) {
		this.delegate.delegateExecute(Platform.toPlayer(invocation.source()), invocation.alias(), invocation.arguments());
	}

	/**
	 * Delegates tab completion to the Foundation command.
	 */
	@Deprecated
	@Override
	public List<String> suggest(final Invocation invocation) {
		return this.delegate.delegateTabComplete(Platform.toPlayer(invocation.source()), invocation.alias(), invocation.arguments());
	}
}
