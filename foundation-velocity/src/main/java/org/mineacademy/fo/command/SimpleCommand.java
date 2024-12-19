package org.mineacademy.fo.command;

import java.util.List;
import java.util.stream.Collectors;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

import com.velocitypowered.api.proxy.Player;

/**
 * A Bukkit implementation of {@link SimpleCommandCore} with shared methods from {@link SharedVelocityCommandCore}.
 */
public abstract class SimpleCommand extends SimpleCommandCore implements SharedVelocityCommandCore {

	/**
	 * @see SimpleCommandCore#SimpleCommand(List)
	 *
	 * @param labelAndAliases
	 */
	protected SimpleCommand(final List<String> labelAndAliases) {
		super(labelAndAliases);
	}

	/**
	 * @see SimpleCommandCore#SimpleCommand(String)
	 *
	 * @param label
	 */
	protected SimpleCommand(final String label) {
		super(label);
	}

	/**
	 * @see SimpleCommandCore#SimpleCommand(String, List)
	 *
	 * @param label
	 * @param aliases
	 */
	protected SimpleCommand(final String label, final List<String> aliases) {
		super(label, aliases);
	}

	/**
	 * Convenience method for completing all player names. Exclude vanished players
	 * if the sender is a player.
	 *
	 * @return
	 */
	@Override
	protected final List<String> completeLastWordPlayerNames() {
		return CommonCore.tabComplete(this.getLastArg(), this.isPlayer() ? Common.getPlayerNames(false) : Common.getPlayerNames(true));
	}

	/**
	 * Convenience method for completing all server names.
	 *
	 * @return
	 */
	protected final List<String> completeLastWordServerNames() {
		return CommonCore.tabComplete(this.getLastArg(), Remain.getServers().stream().map(server -> server.getServerInfo().getName()).collect(Collectors.toList()));
	}

	/**
	 * Return the player by the given args index, and, when the args are shorter, return the sender if sender is player.
	 *
	 * @param argsIndex
	 *
	 * @return
	 * @throws CommandException
	 */
	protected final Player findPlayerOrSelf(final int argsIndex) throws CommandException {
		if (argsIndex >= this.args.length) {
			this.checkBoolean(this.isPlayer(), Lang.component("command-console-missing-player-name"));

			return this.getPlayer();
		}

		final String name = this.args[argsIndex];
		final Player player = this.findPlayerInternal(name);
		this.checkBoolean(player != null && player.isActive(), Lang.component("player-not-online", "player", name));

		return player;
	}
}
