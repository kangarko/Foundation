package org.mineacademy.fo.platform;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.proxy.message.OutgoingMessage;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.event.HoverEventSource;

/**
 * Stores platform-dependend methods such as those interacting with Bukkit, BungeeCord or Velocity.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Platform {

	/**
	 * The specific platform implementation instance.
	 */
	private static FoundationPlatform instance;

	/**
	 * The current platform
	 */
	private static Type type;

	/**
	 * Return the current platform type.
	 *
	 * @return
	 */
	public static Type getType() {
		ValidCore.checkNotNull(type, "Current platform not set!");

		return type;
	}

	/**
	 * Set the current platform
	 *
	 * @param type
	 */
	static void setType(final Type type) {
		Platform.type = type;
	}

	/**
	 * Call an event using the platform-specific event caller.
	 *
	 * @param event
	 * @return
	 */
	public static boolean callEvent(final Object event) {
		return getPlatform().callEvent(event);
	}

	/**
	 * Convert the given item stack to a hover event. Requires Bukkit platforms.
	 *
	 * @param itemStack
	 * @return
	 */
	public static HoverEventSource<?> convertItemStackToHoverEvent(final Object itemStack) {
		return getPlatform().convertItemStackToHoverEvent(itemStack);
	}

	/**
	 * Runs the given command (without /) as the console, replacing {player} with sender
	 *
	 * You can prefix the command with @(announce|warn|error|info|question|success) to send a formatted
	 * message to playerReplacement directly.
	 *
	 * @param playerReplacement can be null
	 * @param command
	 */
	public static void dispatchConsoleCommand(final FoundationPlayer playerReplacement, final String command) {
		getPlatform().dispatchConsoleCommand(playerReplacement, command);
	}

	/**
	 * Get custom server name or throw an exception if not set.
	 *
	 * @return
	 */
	public static String getCustomServerName() {
		return getPlatform().getCustomServerName();
	}

	/**
	 * Get a list of all online players. On Proxy this includes all servers. Redis is supported
	 * as per implementation.
	 *
	 * @return
	 */
	public static List<FoundationPlayer> getOnlinePlayers() {
		return getPlatform().getOnlinePlayers();
	}

	/**
	 * Get the platform implementation instance or throw an exception if not set yet.
	 *
	 * @return
	 */
	static FoundationPlatform getPlatform() {

		// Do not throw FoException to prevent race condition
		if (instance == null)
			throw new NullPointerException("Foundation instance not set yet.");

		return instance;
	}

	/**
	 * Get the server name.
	 *
	 * @return
	 */
	public static String getPlatformName() {
		return getPlatform().getPlatformName();
	}

	/**
	 * Get the server version.
	 *
	 * @return
	 */
	public static String getPlatformVersion() {
		return getPlatform().getPlatformVersion();
	}

	/**
	 * Get a player by his name.
	 *
	 * @param name
	 * @return
	 */
	public static FoundationPlayer getPlayer(final String name) {
		return getPlatform().getPlayer(name);
	}

	/**
	 * Get a player by his unique id.
	 *
	 * @param uniqueId
	 * @return
	 */
	public static FoundationPlayer getPlayer(final UUID uniqueId) {
		return getPlatform().getPlayer(uniqueId);
	}

	/**
	 * Get the plugin that is using Foundation.
	 *
	 * @return
	 */
	public static FoundationPlugin getPlugin() {
		return getPlatform().getPlugin();
	}

	/**
	 * Get the plugin jar file for the given plugin.
	 *
	 * @param pluginName
	 * @return
	 */
	public static File getPluginFile(final String pluginName) {
		return getPlatform().getPluginFile(pluginName);
	}

	/**
	 * Get a list of all plugins installed on the server
	 * in a tuple where key is the plugin name, and value is its version.
	 *
	 * @return
	 */
	public static List<Tuple<String, String>> getPlugins() {
		return getPlatform().getPlugins();
	}

	/**
	 * Get a server by its name.
	 *
	 * @param name
	 * @return the server or null if not found
	 */
	public static FoundationServer getServer(final String name) {
		return getPlatform().getServer(name);
	}

	/**
	 * Return a list of servers. On Bukkit this always returns the single server instance.
	 * On proxy this returns all servers.
	 *
	 * @return
	 */
	public static List<FoundationServer> getServers() {
		return getPlatform().getServers();
	}

	/**
	 * Return true if the server has a custom server name set.
	 *
	 * @return
	 */
	public static boolean hasCustomServerName() {
		return getPlatform().hasCustomServerName();
	}

	/**
	 * Return if the platform was initialized (properly).
	 *
	 * @return
	 */
	public static boolean hasPlatform() {
		return instance != null;
	}

	/**
	 * Return true if the call is performed asynchronously.
	 *
	 * @return
	 */
	public static boolean isAsync() {
		return getPlatform().isAsync();
	}

	/**
	 * Checks if a plugin is enabled. On Bukkit, we also schedule an async task to make
	 * sure the plugin is loaded correctly when the server is done booting
	 * <p>
	 * Return true if it is loaded (this does not mean it works correctly)
	 *
	 * @param name
	 * @return
	 */
	public static boolean isPluginInstalled(final String name) {
		return getPlatform().isPluginInstalled(name);
	}

	/**
	 * Log the given message to the console.
	 *
	 * @deprecated use {@link CommonCore#log(String...)}
	 * @param message
	 */
	@Deprecated
	public static void log(final String message) {
		getPlatform().log(message);
	}

	/**
	 * Register the given command.
	 *
	 * @deprecated internal use only
	 * @param command
	 * @param unregisterOldCommand
	 * @param unregisterOldAliases
	 */
	@Deprecated
	public static void registerCommand(final SimpleCommandCore command, final boolean unregisterOldCommand, final boolean unregisterOldAliases) {
		getPlatform().registerCommand(command, unregisterOldCommand, unregisterOldAliases);
	}

	/**
	 * Automatically registers default Foundation subcommands:
	 *
	 * @see SimpleCommandGroup#registerDefaultSubcommands()
	 *
	 * @param group
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void registerDefaultPlatformSubcommands(final SimpleCommandGroup group) {
		getPlatform().registerDefaultPlatformSubcommands(group);
	}

	/**
	 * Register the given listener in the platform's event loop.
	 *
	 * @param listener
	 */
	public static void registerEvents(final Object listener) {
		getPlatform().registerEvents(listener);
	}

	/**
	 * Run the given task after the given delay in ticks.
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTask(final int delayTicks, final Runnable runnable) {
		return getPlatform().runTask(delayTicks, runnable);
	}

	/**
	 * Run the given task on the next tick.
	 *
	 * @param runnable
	 * @return
	 */
	public static Task runTask(final Runnable runnable) {
		return getPlatform().runTask(runnable);
	}

	/**
	 * Run the given task after the given delay in ticks asynchronously.
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTaskAsync(final int delayTicks, final Runnable runnable) {
		return getPlatform().runTaskAsync(delayTicks, runnable);
	}

	/**
	 * Run the given task asynchronously on the next tick.
	 *
	 * @param runnable
	 * @return
	 */
	public static Task runTaskAsync(final Runnable runnable) {
		return getPlatform().runTaskAsync(runnable);
	}

	/**
	 * Run the given task repeatedly after the given delay with the given repeat period in ticks.
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTaskTimer(final int delayTicks, final int repeatTicks, final Runnable runnable) {
		return getPlatform().runTaskTimer(delayTicks, repeatTicks, runnable);
	}

	/**
	 * Run the given task repeatedly immediatelly with the given repeat period in ticks.
	 *
	 * @param repeatTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTaskTimer(final int repeatTicks, final Runnable runnable) {
		return getPlatform().runTaskTimer(repeatTicks, runnable);
	}

	/**
	 * Run the given task repeatedly after the given delay with the given repeat period in ticks asynchronously.
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTaskTimerAsync(final int delayTicks, final int repeatTicks, final Runnable runnable) {
		return getPlatform().runTaskTimerAsync(delayTicks, repeatTicks, runnable);
	}

	/**
	 * Run the given task repeatedly immediatelly with the given repeat period in ticks asynchronously.
	 *
	 * @param repeatTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTaskTimerAsync(final int repeatTicks, final Runnable runnable) {
		return getPlatform().runTaskTimerAsync(repeatTicks, runnable);
	}

	/**
	 * Send a plugin message through the given sender by his UUID to proxy.
	 *
	 * @deprecated internal use only
	 * @see OutgoingMessage#send(UUID)
	 * @param senderUid
	 * @param channel
	 * @param message
	 */
	@Deprecated
	public static void sendPluginMessage(final UUID senderUid, final String channel, final byte[] message) {
		getPlatform().sendPluginMessage(senderUid, channel, message);
	}

	/**
	 * Set the custom server name identifier used in proxy messaging.
	 *
	 * @see OutgoingMessage#send(UUID)
	 *
	 * @param serverName
	 */
	public static void setCustomServerName(final String serverName) {
		getPlatform().setCustomServerName(serverName);
	}

	/**
	 * Set the platform implementation.
	 *
	 * @param instance
	 */
	static void setInstance(final FoundationPlatform instance) {
		Platform.instance = instance;
	}

	/**
	 * Convert the given player object to a {@link FoundationPlayer}.
	 *
	 * @param player
	 * @return
	 */
	public static FoundationPlayer toPlayer(final Object player) {
		return getPlatform().toPlayer(player);
	}

	/**
	 * Convert the given server object to a {@link FoundationServer}.
	 *
	 * @param server
	 * @return
	 */
	public static FoundationServer toServer(final Object server) {
		return getPlatform().toServer(server);
	}

	/**
	 * Unregister the given command.
	 *
	 * @deprecated internal use only
	 * @param command
	 */
	@Deprecated
	public static void unregisterCommand(final SimpleCommandCore command) {
		getPlatform().unregisterCommand(command);
	}

	/**
	 * Represents a platform type
	 */
	@RequiredArgsConstructor
	public enum Type {

		/**
		 * Represents the Bukkit platform
		 */
		BUKKIT("Bukkit", false),

		/**
		 * Represents the BungeeCord platform
		 */
		BUNGEECORD("BungeeCord", true),

		/**
		 * Represents the Velocity platform
		 */
		VELOCITY("Velocity", true);

		/**
		 * The name of this platform
		 */
		@Getter
		private final String key;

		/**
		 * Is this platform a proxy?
		 */
		@Getter
		private final boolean proxy;

		/**
		 * Return all platforms that are proxies
		 *
		 * @return
		 */
		public static Set<Type> proxies() {
			return Arrays.stream(values()).filter(Type::isProxy).collect(Collectors.toSet());
		}

		/**
		 * Return the platform type from the given key.
		 *
		 * @param key
		 * @return
		 */
		public static Platform.Type fromKey(final String key) {
			for (final Platform.Type type : Platform.Type.values())
				if (type.getKey().equalsIgnoreCase(key))
					return type;

			throw new FoException("Unknown platform type: " + key);
		}

		@Override
		public String toString() {
			return this.key;
		}
	}
}