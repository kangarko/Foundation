package org.mineacademy.fo.command;

import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

/**
 * Implements Bukkit-specific methods for commands. The reason for this
 * is to avoid having to duplicate the same code into both command a subcommand
 * classes.
 */
public interface SharedVelocityCommandCore {

	/**
	 * Attempts to find a non-vanished online player, failing with the message
	 * found at SimpleLocalization
	 *
	 * @param name
	 * @return
	 * @throws CommandException
	 */
	default Player findPlayer(final String name) throws CommandException {
		return this.findPlayer(name, Lang.component("player-not-online"));
	}

	/**
	 * Attempts to find a non-vanished online player, failing with a false message
	 *
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	default Player findPlayer(final String name, final SimpleComponent falseMessage) throws CommandException {
		final Player player = this.findPlayerInternal(name);
		this.checkBoolean(player != null && player.isActive() && !PlayerUtil.isVanished(player), falseMessage.replaceBracket(null, "player", name));

		return player;
	}

	/**
	 * A simple call to Bukkit.getPlayer(name) meant to be overriden
	 * if you have a custom implementation of getting players by name.
	 *
	 * Example use: ChatControl can find players by their nicknames too
	 *
	 * @param name
	 * @return
	 */
	default Player findPlayerInternal(final String name) {
		return Remain.getPlayer(name, false);
	}

	/**
	 * Return the player by the given name, and, when the name is null, return the sender if sender is player.
	 *
	 * @param name
	 * @return
	 * @throws CommandException
	 */
	default Player findPlayerOrSelf(final String name) throws CommandException {
		if (name == null) {
			this.checkBoolean(this.isPlayer(), Lang.component("command-console-missing-player-name"));

			return this.getPlayer();
		}

		final Player player = this.findPlayerInternal(name);
		this.checkBoolean(player != null && player.isActive(), Lang.componentVars("player-not-online", "player", name));

		return player;
	}

	/**
	 * Return the command sender as Bukkit's CommandSender.
	 *
	 * @return
	 */
	default CommandSource getSender() {
		return this.getAudience().getSender();
	}

	/**
	 * Attempts to get the sender as player, only works if the sender is actually a player,
	 * otherwise we return null.
	 *
	 * @return
	 */
	default Player getPlayer() {
		return this.isPlayer() ? this.getAudience().getPlayer() : null;
	}

	/**
	 * Return whether the sender is a living player.
	 *
	 * @return
	 */
	default boolean isPlayer() {
		return this.getAudience().isPlayer();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods implemented in SimpleCommandCore but required to be used in this interface.
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @see SimpleCommandCore#checkBoolean(boolean, SimpleComponent)
	 *
	 * @param flag
	 * @param falseMessage
	 */
	void checkBoolean(boolean flag, SimpleComponent falseMessage);

	/**
	 * @see SimpleCommandCore#getAudience()
	 *
	 * @return
	 */
	FoundationPlayer getAudience();
}
