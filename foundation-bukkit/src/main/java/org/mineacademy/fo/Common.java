package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Our main utility class hosting a large variety of different convenience functions.
 *
 * This is the Bukkit-specific implementation, inheriting from {@link CommonCore}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Common extends CommonCore {

	/**
	 * Send messages to the given sender, vertically centered and
	 * surrounded by chat-wide line on the top and bottom:
	 *
	 * {@literal ----------------------------------- }
	 *
	 * Hello this is a test!
	 *
	 * {@literal ----------------------------------- }
	 *
	 * You can specify {@literal <center>} among others before the message,
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 * @param sender
	 * @param messages
	 */
	public static void tellBoxed(CommandSender sender, String... messages) {
		CommonCore.tellBoxed(Platform.toPlayer(sender), messages);
	}

	/**
	 * Send messages to the given sender, vertically centered and
	 * surrounded by chat-wide line on the top and bottom:
	 *
	 * {@literal ----------------------------------- }
	 *
	 * Hello this is a test!
	 *
	 * {@literal ----------------------------------- }
	 *
	 * You can specify {@literal <center>} among others before the message,
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 * @param sender
	 * @param messages
	 */
	public static void tellBoxed(CommandSender sender, SimpleComponent... messages) {
		CommonCore.tellBoxed(Platform.toPlayer(sender), messages);
	}

	/**
	 * Send a message to the given sender after the given time in ticks.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param delayTicks
	 * @param sender
	 * @param message
	 */
	public static void tellLater(final int delayTicks, final CommandSender sender, final String message) {
		Common.tellLater(delayTicks, Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message to the given sender after the given time in ticks.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param delayTicks
	 * @param sender
	 * @param message
	 */
	public static void tellLater(final int delayTicks, final CommandSender sender, final SimpleComponent message) {
		Common.tellLater(delayTicks, Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message to the player if it was not sent previously in the given delay.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param delaySeconds
	 * @param sender
	 * @param message
	 */
	public static void tellTimed(final int delaySeconds, final CommandSender sender, final String message) {
		CommonCore.tellTimed(delaySeconds, Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message to the player if it was not sent previously in the given delay.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param delaySeconds
	 * @param sender
	 * @param message
	 */
	public static void tellTimed(final int delaySeconds, final CommandSender sender, final SimpleComponent message) {
		CommonCore.tellTimed(delaySeconds, Platform.toPlayer(sender), message);
	}

	/**
	 * Send messages to the given sender.
	 *
	 * The messages are converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tell(@NonNull CommandSender sender, String... messages) {
		CommonCore.tell(Platform.toPlayer(sender), messages);
	}

	/**
	 * Send the message to the given sender.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param sender
	 * @param message
	 */
	public static void tell(@NonNull final CommandSender sender, SimpleComponent message) {
		Platform.toPlayer(sender).sendMessage(message);
	}

	/**
	 * Return a list of all world names on the server.
	 *
	 * @return
	 */
	public static List<String> getWorldNames() {
		final List<String> worlds = new ArrayList<>();

		for (final World world : Bukkit.getWorlds())
			worlds.add(world.getName());

		return worlds;
	}

	/**
	 * Get a list of player names currently online, including vanished players.
	 *
	 * @see #getPlayerNames(boolean, Player)
	 *
	 * @return
	 */
	public static List<String> getPlayerNames() {
		return getPlayerNames(true, null);
	}

	/**
	 * Get a list of player names currently online, with an option to include or exclude vanished players.
	 *
	 * @see #getPlayerNames(boolean, Player)
	 *
	 * @param includeVanished
	 * @return
	 */
	public static List<String> getPlayerNames(final boolean includeVanished) {
		return getPlayerNames(includeVanished, null);
	}

	/**
	 * Get a list of player names currently online, with an option to include or exclude vanished players.
	 * <p>
	 * Vanished players can be included or excluded based on the `includeVanished` parameter.
	 * <p>
	 * Example usage:
	 * <pre>
	 *   List&lt;String&gt; allPlayers = getPlayerNames(true, otherPlayer);
	 *   List&lt;String&gt; visiblePlayers = getPlayerNames(false, otherPlayer);
	 * </pre>
	 *
	 * @param includeVanished whether to include vanished players in the result
	 * @param otherPlayer the player doing the query, used for checking if a player is vanished for them
	 * @return a list of online player names, optionally including vanished players
	 */
	public static List<String> getPlayerNames(final boolean includeVanished, Player otherPlayer) {
		final List<String> found = new ArrayList<>();

		for (final Player online : Remain.getOnlinePlayers()) {
			if (PlayerUtil.isVanished(online, otherPlayer) && !includeVanished)
				continue;

			found.add(online.getName());
		}

		return found;
	}

	/**
	 * Get a list of player nicknames (without color codes) currently online.
	 *
	 * @see #getPlayerNicknames(boolean, Player)
	 *
	 * @param includeVanished
	 * @return
	 */
	public static List<String> getPlayerNicknames(final boolean includeVanished) {
		return getPlayerNicknames(includeVanished, null);
	}

	/**
	 * Get a list of player nicknames (without color codes) currently online, with an option to include or exclude vanished players.
	 *
	 * Supports CMI, EssentialsX and Nicky, or if it's a console, their name.
	 *
	 * Vanished players can be included or excluded based on the `includeVanished` parameter.
	 *
	 * @param includeVanished whether to include vanished players in the result
	 * @param otherPlayer the player doing the query, used for checking if a player is vanished for them
	 * @return a list of online player nicknames without color codes, optionally including vanished players
	 */
	public static List<String> getPlayerNicknames(final boolean includeVanished, Player otherPlayer) {
		final List<String> found = new ArrayList<>();

		for (final Player online : Remain.getOnlinePlayers()) {
			if (PlayerUtil.isVanished(online, otherPlayer) && !includeVanished)
				continue;

			found.add(HookManager.getNickColorless(online));
		}

		return found;
	}

	/**
	 * Find the plugin command from the command.
	 *
	 * The command can either just be the label such as "/give" or "give"
	 * or the full command such as "/give kangarko diamonds", in which case
	 * we will find the label and just match against "/give"
	 *
	 * @param command
	 * @return
	 */
	public static Command findCommand(final String command) {
		final String[] args = command.split(" ");

		if (args.length > 0) {
			String label = args[0].toLowerCase();

			if (label.startsWith("/"))
				label = label.substring(1);

			for (final Plugin otherPlugin : Bukkit.getPluginManager().getPlugins()) {
				final JavaPlugin plugin = (JavaPlugin) otherPlugin;

				if (plugin instanceof JavaPlugin) {
					final Command pluginCommand = plugin.getCommand(label);

					if (pluginCommand != null)
						return pluginCommand;
				}
			}

			final Command serverCommand = Remain.getCommandMap().getCommand(label);

			if (serverCommand != null)
				return serverCommand;
		}

		return null;
	}

	/**
	 * Returns a random location.
	 *
	 * @param origin
	 * @param radius
	 * @param is3D true for sphere, false for cylinder search
	 * @return
	 */
	public static Location getRandomLocation(final Location origin, final double radius, final boolean is3D) {
		final double rectX = RandomUtil.getRandom().nextDouble() * radius;
		final double rectZ = RandomUtil.getRandom().nextDouble() * radius;
		final double offsetX;
		final double offsetZ;
		double offsetY = 0;
		final int transform = RandomUtil.getRandom().nextInt(4);

		if (is3D) {
			final double rectY = RandomUtil.getRandom().nextDouble() * radius;

			offsetY = getYCords(transform, rectY);
		}

		if (transform == 0) {
			offsetX = rectX;
			offsetZ = rectZ;

		} else if (transform == 1) {
			offsetX = -rectZ;
			offsetZ = rectX;

		} else if (transform == 2) {
			offsetX = -rectX;
			offsetZ = -rectZ;

		} else {
			offsetX = rectZ;
			offsetZ = -rectX;
		}

		return origin.clone().add(offsetX, offsetY, offsetZ);
	}

	/**
	 * Returns a random location, between the min and the max radius:
	 * Example: Min radius is 500 and max is 2000, then we return locations around 500-2000 blocks away from the origin
	 *
	 * @param origin
	 * @param minRadius
	 * @param maxRadius
	 * @param is3D true for sphere, false for cylinder search
	 * @return
	 */
	public static Location getRandomLocation(final Location origin, final double minRadius, final double maxRadius, final boolean is3D) {
		ValidCore.checkBoolean(maxRadius > 0 && minRadius > 0, "Max and min radius must be over 0");
		ValidCore.checkBoolean(maxRadius > minRadius, "Max radius must be greater than min radius");

		final double rectX = RandomUtil.getRandom().nextDouble() * (maxRadius - minRadius) + minRadius;
		final double rectZ = RandomUtil.getRandom().nextDouble() * (maxRadius + minRadius) - minRadius;
		final double offsetX;
		final double offsetZ;

		double offsetY = 0;
		final int transform = RandomUtil.getRandom().nextInt(4);

		if (is3D) {
			final double rectY = RandomUtil.getRandom().nextDouble() * (maxRadius + minRadius) - minRadius;

			offsetY = getYCords(transform, rectY);
		}

		if (transform == 0) {
			offsetX = rectX;
			offsetZ = rectZ;

		} else if (transform == 1) {
			offsetX = -rectZ;
			offsetZ = rectX;

		} else if (transform == 2) {
			offsetX = -rectX;
			offsetZ = -rectZ;

		} else {
			offsetX = rectZ;
			offsetZ = -rectX;
		}

		return origin.clone().add(offsetX, offsetY, offsetZ);
	}

	/*
	 * Get the Y cords for the location
	 */
	private static double getYCords(int transform, double rectY) {
		double offsetY;
		final double nextY = RandomUtil.getRandom().nextDouble();

		if (transform < 2)
			offsetY = nextY >= 0.5 ? -rectY : rectY;
		else
			offsetY = nextY >= 0.5 ? rectY : -rectY;

		return offsetY;
	}
}
