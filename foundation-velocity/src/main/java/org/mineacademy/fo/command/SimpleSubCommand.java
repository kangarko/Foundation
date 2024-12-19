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
 * A Bukkit implementation of {@link SimpleSubCommandCore} with shared methods from {@link SharedVelocityCommandCore}.
 */
public abstract class SimpleSubCommand extends SimpleSubCommandCore implements SharedVelocityCommandCore {

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
