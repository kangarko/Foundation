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
	public VelocityCommandImpl(SimpleCommandCore delegate) {
		this.delegate = delegate;
	}

	/**
	 * Delegates execution to the Foundation command.
	 */
	@Override
	public void execute(Invocation invocation) {
		delegate.delegateExecute(Platform.toPlayer(invocation.source()), invocation.alias(), invocation.arguments());
	}

	/**
	 * Delegates tab completion to the Foundation command.
	 */
	@Override
	public List<String> suggest(Invocation invocation) {
		return delegate.delegateTabComplete(Platform.toPlayer(invocation.source()), invocation.alias(), invocation.arguments());
	}
}
