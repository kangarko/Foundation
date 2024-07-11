package org.mineacademy.fo.platform;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.proxy.message.OutgoingMessage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
	 * Call an event using the platform-specific event caller.
	 *
	 * @param event
	 * @return
	 */
	public static boolean callEvent(Object event) {
		return getPlatform().callEvent(event);
	}

	/**
	 * Convert the given item stack to a hover event. Requires Bukkit platforms.
	 *
	 * @param itemStack
	 * @return
	 */
	public static HoverEventSource<?> convertItemStackToHoverEvent(Object itemStack) {
		return getPlatform().convertItemStackToHoverEvent(itemStack);
	}

	/**
	 * Runs the given command (without /) as the console, replacing {player} with sender
	 *
	 * You can prefix the command with @(announce|warn|error|info|question|success) to send a formatted
	 * message to playerReplacement directly.
	 *
	 * @param playerReplacement
	 * @param command
	 */
	public static void dispatchConsoleCommand(FoundationPlayer playerReplacement, String command) {
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
	 * Get a list of all online players.
	 *
	 * @return
	 */
	public static List<FoundationPlayer> getOnlinePlayers() {
		return getPlatform().getOnlinePlayers();
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
	public static File getPluginFile(String pluginName) {
		return getPlatform().getPluginFile(pluginName);
	}

	/**
	 * Get a list of all plugins installed on the server
	 * in a tuple where key is the plugin name, and value is its version.
	 *
	 * @return
	 */
	public static List<Tuple<String, String>> getServerPlugins() {
		return getPlatform().getServerPlugins();
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
	 * Return true if the server supports HEX colors.
	 *
	 * @return
	 */
	public static boolean hasHexColorSupport() {
		return getPlatform().hasHexColorSupport();
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
	public static boolean isPluginInstalled(String name) {
		return getPlatform().isPluginInstalled(name);
	}

	/**
	 * Log the given message to the console.
	 *
	 * @deprecated use {@link CommonCore#log(String...)}
	 * @param message
	 */
	@Deprecated
	public static void log(String message) {
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
	public static void registerCommand(SimpleCommandCore command, boolean unregisterOldCommand, boolean unregisterOldAliases) {
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
	public static void registerDefaultSubcommands(SimpleCommandGroup group) {
		getPlatform().registerDefaultSubcommands(group);
	}

	/**
	 * Register the given listener in the platform's event loop.
	 *
	 * @param listener
	 */
	public static void registerEvents(Object listener) {
		getPlatform().registerEvents(listener);
	}

	/**
	 * Run the given task after the given delay in ticks.
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTask(int delayTicks, Runnable runnable) {
		return getPlatform().runTask(delayTicks, runnable);
	}

	/**
	 * Run the given task on the next tick.
	 *
	 * @param runnable
	 * @return
	 */
	public static Task runTask(Runnable runnable) {
		return getPlatform().runTask(runnable);
	}

	/**
	 * Run the given task after the given delay in ticks asynchronously.
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTaskAsync(int delayTicks, Runnable runnable) {
		return getPlatform().runTaskAsync(delayTicks, runnable);
	}

	/**
	 * Run the given task asynchronously on the next tick.
	 *
	 * @param runnable
	 * @return
	 */
	public static Task runTaskAsync(Runnable runnable) {
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
	public static Task runTaskTimer(int delayTicks, int repeatTicks, Runnable runnable) {
		return getPlatform().runTaskTimer(delayTicks, repeatTicks, runnable);
	}

	/**
	 * Run the given task repeatedly immediatelly with the given repeat period in ticks.
	 *
	 * @param repeatTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTaskTimer(int repeatTicks, Runnable runnable) {
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
	public static Task runTaskTimerAsync(int delayTicks, int repeatTicks, Runnable runnable) {
		return getPlatform().runTaskTimerAsync(delayTicks, repeatTicks, runnable);
	}

	/**
	 * Run the given task repeatedly immediatelly with the given repeat period in ticks asynchronously.
	 *
	 * @param repeatTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTaskTimerAsync(int repeatTicks, Runnable runnable) {
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
	public static void sendPluginMessage(UUID senderUid, String channel, byte[] message) {
		getPlatform().sendPluginMessage(senderUid, channel, message);
	}

	/**
	 * Set the custom server name identifier used in proxy messaging.
	 *
	 * @see OutgoingMessage#send(UUID)
	 *
	 * @param serverName
	 */
	public static void setCustomServerName(String serverName) {
		getPlatform().setCustomServerName(serverName);
	}

	/**
	 * Set the platform implementation.
	 *
	 * @param instance
	 */
	static void setInstance(FoundationPlatform instance) {
		Platform.instance = instance;
	}

	/**
	 * Convert the given player object to a FoundationPlayer.
	 *
	 * @param player
	 * @return
	 */
	public static FoundationPlayer toPlayer(Object player) {
		return getPlatform().toPlayer(player);
	}

	/**
	 * Unregister the given command.
	 *
	 * @deprecated internal use only
	 * @param command
	 */
	@Deprecated
	public static void unregisterCommand(SimpleCommandCore command) {
		getPlatform().unregisterCommand(command);
	}
}
