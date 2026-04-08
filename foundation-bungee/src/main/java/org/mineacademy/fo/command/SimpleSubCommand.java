package org.mineacademy.fo.command;

import java.util.List;
import java.util.stream.Collectors;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
	protected SimpleSubCommand(final SimpleCommandGroup parent, final String sublabel) {
		super(parent, sublabel);
	}

	/**
	 * @see SimpleSubCommandCore#SimpleSubCommand(String)
	 *
	 * @param sublabel
	 */
	protected SimpleSubCommand(final String sublabel) {
		super(sublabel);
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
		return CommonCore.tabComplete(this.getLastArg(), Remain.getServers().stream().map(ServerInfo::getName).collect(Collectors.toList()));
	}

	/**
	 * Return the player by the given args index, and, when the args are shorter, return the sender if sender is player.
	 *
	 * @param argsIndex
	 *
	 * @return
	 * @throws CommandException
	 */
	protected final ProxiedPlayer findPlayerOrSelf(final int argsIndex) throws CommandException {
		if (argsIndex >= this.args.length) {
			this.checkBoolean(this.isPlayer(), Lang.component("command-console-missing-player-name"));

			return this.getPlayer();
		}

		final String name = this.args[argsIndex];
		final ProxiedPlayer player = this.findPlayerInternal(name);
		this.checkBoolean(player != null && player.isConnected(), Lang.component("player-not-online", "player", name));

		return player;
	}
}
