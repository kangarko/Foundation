package org.mineacademy.fo.command;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Consumer;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.platform.BukkitPlayer;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

/**
 * Implements Bukkit-specific methods for commands. The reason for this
 * is to avoid having to duplicate the same code into both command a subcommand
 * classes.
 */
public interface SharedBukkitCommandCore {

	/**
	 * Checks if the player is a console and throws an error if he is.
	 *
	 * @throws CommandException
	 */
	default void checkConsole() throws CommandException {
		if (!this.isPlayer())
			throw new CommandException(Lang.component("command-requires-player"));
	}

	/**
	 * Convenience method for completing all player names. Exclude vanished players
	 * if the sender is a player.
	 *
	 * @return
	 */
	default List<String> completeLastWordPlayerNames() {
		return this.isPlayer() ? Common.getPlayerNames(false) : Common.getPlayerNames();
	}

	/**
	 * Convenience method for completing all world names.
	 *
	 * @return
	 */
	default List<String> completeLastWordWorldNames() {
		return CommonCore.tabComplete(this.getArgs().length > 0 ? this.getArgs()[this.getArgs().length - 1] : "", Common.getWorldNames());
	}

	/**
	 * Attempts to parse the given name into a CompMaterial, will work for both modern
	 * and legacy materials: MONSTER_EGG and SHEEP_SPAWN_EGG
	 *
	 * You can use the {enum} or {item} variable to replace with the given name.
	 *
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	default CompMaterial findMaterial(final String name, final SimpleComponent falseMessage) throws CommandException {
		final CompMaterial found = CompMaterial.fromString(name);

		this.checkBoolean(found != null, falseMessage
				.replaceBracket("enum", name)
				.replaceBracket("item", name));

		return found;
	}

	/**
	 * Attempts to parse the given name into a CompMaterial, will work for both modern
	 * and legacy materials: MONSTER_EGG and SHEEP_SPAWN_EGG.
	 *
	 * You can use the {enum} or {item} variable to replace with the given name.
	 *
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	default CompMaterial findMaterial(final String name, final String falseMessage) throws CommandException {
		return this.findMaterial(name, SimpleComponent.fromMini(falseMessage));
	}

	/**
	 * Attempts to find the offline player by name or string UUID, sends an error message to sender if he did not play before
	 * or runs the specified callback on successful retrieval.
	 *
	 * The offline player lookup is done async, the callback is synchronized.
	 *
	 * @param name or string UUID
	 * @param syncCallback
	 * @throws CommandException
	 */
	default void findOfflinePlayer(final String name, final Consumer<OfflinePlayer> syncCallback) throws CommandException {
		if (name.length() == 36 && name.charAt(8) == '-' && name.charAt(13) == '-' && name.charAt(18) == '-' && name.charAt(23) == '-') {
			UUID uuid = null;

			try {
				uuid = UUID.fromString(name);

			} catch (final IllegalArgumentException ex) {
				this.returnTell(Lang.componentVars("command-invalid-uuid", "uuid", name));
			}

			this.findOfflinePlayer(uuid, syncCallback);

		} else
			this.runTaskAsync(() -> {
				final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(name);
				this.checkBoolean(targetPlayer != null && (targetPlayer.isOnline() || targetPlayer.hasPlayedBefore()), Lang.componentVars("player-not-played-before", "player", name));

				this.runTask(() -> syncCallback.accept(targetPlayer));
			});
	}

	/**
	 * Attempts to find the offline player by UUID, this will fire the callback
	 *
	 * @param uniqueId
	 * @param syncCallback
	 * @throws CommandException
	 */
	default void findOfflinePlayer(final UUID uniqueId, final Consumer<OfflinePlayer> syncCallback) throws CommandException {
		this.runTaskAsync(() -> {
			final OfflinePlayer targetPlayer = Remain.getOfflinePlayerByUUID(uniqueId);
			this.checkBoolean(targetPlayer != null && (targetPlayer.isOnline() || targetPlayer.hasPlayedBefore()), Lang.componentVars("player-invalid-uuid", "uuid", uniqueId.toString()));

			this.runTask(() -> syncCallback.accept(targetPlayer));
		});
	}

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
		this.checkBoolean(player != null && player.isOnline() && !PlayerUtil.isVanished(player), falseMessage.replaceBracket("player", name));

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
		return Bukkit.getPlayer(name);
	}

	/**
	 * Return the player by the given args index, and, when the args are shorter, return the sender if sender is player.
	 *
	 * @param argsIndex
	 *
	 * @return
	 * @throws CommandException
	 */
	default Player findPlayerOrSelf(final int argsIndex) throws CommandException {
		if (argsIndex >= this.getArgs().length) {
			this.checkBoolean(this.isPlayer(), Lang.component("command-console-missing-player-name"));

			return this.getPlayer();
		}

		final String name = this.getArgs()[argsIndex];
		final Player player = this.findPlayerInternal(name);
		this.checkBoolean(player != null && player.isOnline(), Lang.componentVars("player-not-online", "player", name));

		return player;
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
		this.checkBoolean(player != null && player.isOnline(), Lang.componentVars("player-not-online", "player", name));

		return player;
	}

	/**
	 * Attempts to convert the given name into a bukkit world,
	 * sending localized error message if such world does not exist.
	 *
	 * @param name
	 * @return
	 */
	default World findWorld(final String name) {
		if ("~".equals(name)) {
			this.checkBoolean(this.isPlayer(), Lang.component("command-cannot-autodetect-world"));

			return this.getPlayer().getWorld();
		}

		final World world = Bukkit.getWorld(name);
		this.checkBoolean(world != null, Lang.componentVars("command-invalid-world",
				"world", name,
				"available", CommonCore.join(Bukkit.getWorlds(), otherWorld -> otherWorld.getName())));

		return world;
	}

	/**
	 * Return the command sender as Bukkit's CommandSender.
	 *
	 * @return
	 */
	default CommandSender getCommandSender() {
		return ((BukkitPlayer) this.getAudience()).getCommandSender();
	}

	/**
	 * Attempts to get the sender as player, only works if the sender is actually a player,
	 * otherwise we return null.
	 *
	 * @return
	 */
	default Player getPlayer() {
		return this.isPlayer() ? ((BukkitPlayer) this.getAudience()).getPlayer() : null;
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
	 * @see SimpleCommand#checkBoolean(boolean, SimpleComponent)
	 *
	 * @param flag
	 * @param falseMessage
	 */
	void checkBoolean(boolean flag, SimpleComponent falseMessage);

	/**
	 * @see SimpleCommandCore#getArgs()
	 *
	 * @return
	 */
	String[] getArgs();

	/**
	 * @see SimpleCommandCore#getAudience()
	 *
	 * @return
	 */
	FoundationPlayer getAudience();

	/**
	 * @see SimpleCommandCore#returnTell(SimpleComponent)
	 *
	 * @param message
	 */
	void returnTell(SimpleComponent message);

	/**
	 * @see SimpleCommandCore#runTask(Runnable)
	 *
	 * @param task
	 * @return
	 */
	Task runTask(Runnable task);

	/**
	 * @see SimpleCommandCore#runTaskAsync(Runnable)
	 *
	 * @param task
	 * @return
	 */
	Task runTaskAsync(Runnable task);
}
