
package org.mineacademy.fo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.conversations.Conversable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.RegexTimeoutException;
import org.mineacademy.fo.model.DiscordSender;
import org.mineacademy.fo.model.LocalCommandSender;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Our main utility class hosting a large variety of different convenience
 * functions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Common {

	// ------------------------------------------------------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Pattern used to match colors with & or {@link ChatColor#COLOR_CHAR}
	 */
	private static final Pattern COLOR_REGEX = Pattern.compile("(?i)(&|" + ChatColor.COLOR_CHAR + ")([0-9A-F])");

	/**
	 * We use this to send messages with colors to yor console
	 */
	private static final CommandSender CONSOLE_SENDER = Bukkit.getServer() != null ? Bukkit.getServer().getConsoleSender() : LocalCommandSender.INSTANCE;

	/**
	 * Used to send messages to player without repetition, e.g. if they attempt to break a block
	 * in a restricted region, we will not spam their chat with "You cannot break this block here" 120x times,
	 * instead, we only send this message once per X seconds. This cache holds the last times when we
	 * sent that message so we know how long to wait before the next one.
	 */
	private static final Map<String, Long> TIMED_TELL_CACHE = new HashMap<>();

	/**
	 * See {@link #TIMED_TELL_CACHE}, but this is for sending messages to your console
	 */
	private static final Map<String, Long> TIMED_LOG_CACHE = new HashMap<>();

	// ------------------------------------------------------------------------------------------------------------
	// Tell prefix
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Should we add a prefix to the messages we send to players using tell() methods?
	 */
	public static boolean ADD_TELL_PREFIX = false;

	/**
	 * Should we add a prefix to the messages we send to the console?
	 *
	 * This is enabled automatically when a {@link SimplePlugin} starts
	 */
	public static boolean ADD_LOG_PREFIX = false;

	/**
	 * The tell prefix applied on tell() messages
	 */
	private static String tellPrefix = "";

	static {
		tellPrefix = "[" + SimplePlugin.getNamed() + "] ";
	}

	/**
	 * Get the tell prefix applied for messages to players and console
	 *
	 * @return
	 */
	public static String getTellPrefix() {
		return tellPrefix;
	}

	/**
	 * Set the tell prefix applied for messages to players and console
	 *
	 * We add an empty space after it for you automatically.
	 *
	 * Colors with & letter are translated automatically.
	 *
	 * @param prefix
	 */
	public static void setTellPrefix(String prefix) {
		tellPrefix = colorize(prefix) + " ";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Broadcasting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Broadcast the message replacing {player} variable with the given command sender
	 *
	 * @param message
	 * @param sender
	 */
	public static void broadcastWithPlayer(String message, CommandSender sender) {
		broadcastWithPlayer(message, resolveSenderName(sender));
	}

	/**
	 * Broadcast the message replacing {player} variable with the given player replacement
	 *
	 * @param message
	 * @param playerReplacement
	 */
	public static void broadcastWithPlayer(String message, String playerReplacement) {
		broadcast(message.replace("{player}", playerReplacement));
	}

	/**
	 * Broadcast the message to everyone and logs it
	 *
	 * @param message
	 */
	public static void broadcast(String message) {
		broadcast(message, true);
	}

	/**
	 * Broadcast the message and also may log it into the console
	 *
	 * @param message
	 * @param log
	 */
	public static void broadcast(String message, boolean log) {
		if (message != null && !message.equals("none")) {
			for (final Player online : Remain.getOnlinePlayers())
				tellJson(online, message);

			if (log)
				log(message);
		}
	}

	/**
	 * Broadcast the message to everyone with permission
	 *
	 * @param permission
	 * @param message
	 * @param log
	 */
	public static void broadcastWithPerm(String permission, String message, boolean log) {
		if (message != null && !message.equals("none")) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, permission))
					tellJson(online, message);

			if (log)
				log(message);
		}
	}

	/**
	 * Broadcast the text component message to everyone with permission
	 *
	 * @param permission
	 * @param message
	 */
	public static void broadcastWithPerm(String permission, @NonNull TextComponent message) {
		final String legacy = message.toLegacyText();

		if (!legacy.equals("none")) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, permission))
					Remain.sendComponent(online, message);

			log(legacy);
		}
	}

	/**
	 * Sends messages to all recipients
	 *
	 * @param recipients
	 * @param messages
	 */
	public static void broadcastTo(Iterable<? extends CommandSender> recipients, String... messages) {
		for (final CommandSender sender : recipients)
			tell(sender, messages);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Messaging
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Sends a message to the player and saves the time when it was sent.
	 * The delay in seconds is the delay between which we won't send player the
	 * same message, in case you call this method again.
	 *
	 * See {@link #TIMED_TELL_CACHE} for more explanation.
	 *
	 * @param delaySeconds
	 * @param sender
	 * @param message
	 */
	public static void tellTimed(int delaySeconds, CommandSender sender, String message) {

		// No previous message stored, just tell the player now
		if (!TIMED_TELL_CACHE.containsKey(message)) {
			tell(sender, message);

			TIMED_TELL_CACHE.put(message, TimeUtilFo.currentTimeSeconds());
			return;
		}

		if (TimeUtilFo.currentTimeSeconds() - TIMED_TELL_CACHE.get(message) > delaySeconds) {
			tell(sender, message);

			TIMED_TELL_CACHE.put(message, TimeUtilFo.currentTimeSeconds());
		}
	}

	/**
	 * Sends the conversable a message later
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	public static void tellLaterConversing(int delayTicks, Conversable conversable, String message) {
		Common.runLater(delayTicks, () -> tellConversing(conversable, message));
	}

	/**
	 * Sends the conversable player a colorized message
	 *
	 * @param conversable
	 * @param message
	 */
	public static void tellConversing(Conversable conversable, String message) {
		conversable.sendRawMessage(Common.colorize((ADD_TELL_PREFIX ? tellPrefix : "") + message));
	}

	/**
	 * Sends a message to the sender with a given delay, colors & are supported
	 *
	 * @param sender
	 * @param delayTicks
	 * @param messages
	 */
	public static void tellLater(final int delayTicks, final CommandSender sender, final String... messages) {
		runLater(delayTicks, () -> tell(sender, messages));
	}

	/**
	 * Sends the sender a bunch of messages, colors & are supported
	 * without {@link #getTellPrefix()} prefix
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tellNoPrefix(CommandSender sender, String... messages) {
		final boolean was = ADD_TELL_PREFIX;

		ADD_TELL_PREFIX = false;
		tell(sender, messages);
		ADD_TELL_PREFIX = was;
	}

	/**
	 * Send the sender a bunch of messages, colors & are supported
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tell(CommandSender sender, Collection<String> messages) {
		tell(sender, toArray(messages));
	}

	/**
	 * Sends sender a bunch of messages, ignoring the ones that equal "none" or null,
	 * replacing & colors and {player} with his variable
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tell(CommandSender sender, String... messages) {
		for (final String message : messages)
			if (message != null && !"none".equals(message))
				tellJson(sender, message);
	}

	/**
	 * Tells the sender a basic message with & colors replaced and {player} with his variable replaced.
	 *
	 * If the message starts with [JSON] than we remove the [JSON] prefix and handle the message
	 * as a valid JSON component.
	 *
	 * Finally, a prefix to non-json messages is added, see {@link #getTellPrefix()}
	 *
	 * @param sender
	 * @param message
	 */
	public static void tellJson(@NonNull CommandSender sender, String message) {
		if (message.isEmpty() || "none".equals(message))
			return;

		// Has prefix already? This is replaced when colorizing
		final boolean hasPrefix = message.contains("{prefix}");

		// Add colors and replace player
		message = colorize(message.replace("{player}", resolveSenderName(sender)));

		// Send [JSON] prefixed messages as json component
		if (message.startsWith("[JSON]")) {
			String stripped = message.substring(6);

			if (stripped.startsWith(" "))
				stripped = stripped.substring(1);

			if (!stripped.isEmpty())
				Remain.sendJson(sender, stripped);

		} else
			for (final String part : splitNewline(message))
				sender.sendMessage((ADD_TELL_PREFIX && !hasPrefix ? tellPrefix : "") + part);
	}

	/**
	 * Return the sender's name if it's a player or discord sender, or simply {@link SimplePlugin#getConsoleName()} if it is a console
	 *
	 * @param sender
	 * @return
	 */
	public static String resolveSenderName(CommandSender sender) {
		return sender instanceof Player || sender instanceof DiscordSender ? sender.getName() : SimpleLocalization.CONSOLE_NAME;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Colorizing messages
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces & colors for every string in the list
	 * A new list is created only containing non-null list values
	 *
	 * @param list
	 * @return
	 */
	public static List<String> colorize(List<String> list) {
		final List<String> copy = new ArrayList<>();
		copy.addAll(list);

		for (int i = 0; i < copy.size(); i++) {
			final String message = copy.get(i);

			if (message != null)
				copy.set(i, colorize(message));
		}

		return copy;
	}

	/**
	 * Replace the & letter with the {@link org.bukkit.ChatColor.COLOR_CHAR} in the message.
	 *
	 * @param messages the messages to replace color codes with '&'
	 * @return the colored message
	 */
	public static String colorize(String... messages) {
		return colorize(StringUtils.join(messages, "\n"));
	}

	/**
	 * Replace the & letter with the {@link org.bukkit.ChatColor.COLOR_CHAR} in the message.
	 *
	 * Also replaces {prefix} with {@link #getTellPrefix()} and {server} with {@link SimplePlugin#getServerPrefix()}
	 *
	 * @param message the message to replace color codes with '&'
	 * @return the colored message
	 */
	public static String colorize(String message) {
		return message == null || message.isEmpty() ? ""
				: ChatColor.translateAlternateColorCodes('&', message
						.replace("{prefix}", tellPrefix)
						.replace("{server}", SimpleLocalization.SERVER_PREFIX)
						.replace("{plugin.name}", SimplePlugin.getNamed().toLowerCase()));
	}

	/**
	 * Replaces the {@link ChatColor#COLOR_CHAR} colors with & letters
	 *
	 * @param messages
	 * @return
	 */
	public static String[] revertColorizing(String[] messages) {
		for (int i = 0; i < messages.length; i++)
			messages[i] = revertColorizing(messages[i]);

		return messages;
	}

	/**
	 * Replaces the {@link ChatColor#COLOR_CHAR} colors with & letters
	 *
	 * @param message
	 * @return
	 */
	public static String revertColorizing(String message) {
		return message.replaceAll("(?i)" + ChatColor.COLOR_CHAR + "([0-9a-fk-or])", "&$1");
	}

	/**
	 * Remove all {@link ChatColor#COLOR_CHAR} as well as & letter colors from the message
	 *
	 * @param message
	 * @return
	 */
	public static String stripColors(String message) {
		return message == null ? "" : message.replaceAll("(" + ChatColor.COLOR_CHAR + "|&)([0-9a-fk-or])", "");
	}

	/**
	 * Returns if the message contains either {@link ChatColor#COLOR_CHAR} or & letter colors
	 *
	 * @param message
	 * @return
	 */
	public static boolean hasColors(String message) {
		return COLOR_REGEX.matcher(message).find();
	}

	/**
	 * Returns the last color, either & or {@link ChatColor#COLOR_CHAR} from the given message
	 *
	 * @param message, or empty if none
	 * @return
	 */
	public static String lastColor(String message) {
		final String andLetter = lastColorLetter(message);
		final String colorChat = lastColorChar(message);

		return !andLetter.isEmpty() ? andLetter : !colorChat.isEmpty() ? colorChat : "";
	}

	/**
	 * Return last color & + the color letter from the message, or empty if not exist
	 *
	 * @param message
	 * @return
	 */
	public static String lastColorLetter(String message) {
		return lastColor(message, '&');
	}

	/**
	 * Return last {@link ChatColor#COLOR_CHAR} + the color letter from the message, or empty if not exist
	 *
	 * @param message
	 * @return
	 */
	public static String lastColorChar(String message) {
		return lastColor(message, ChatColor.COLOR_CHAR);
	}

	private static String lastColor(String msg, char colorChar) {
		final int c = msg.lastIndexOf(colorChar);

		// Contains our character
		if (c != -1) {

			// Contains a character after color character
			if (msg.length() > c + 1)

				// The after character is a valid color
				if (msg.substring(c + 1, c + 2).matches("([0-9a-fk-or])"))
					return msg.substring(c, c + 2).trim();

			// Search after colors before that invalid character
			return lastColor(msg.substring(0, c), colorChar);
		}

		return "";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Aesthetics
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns a long ------ console line
	 *
	 * @return
	 */
	public static String consoleLine() {
		return "!-----------------------------------------------------!";
	}

	/**
	 * Returns a long ______ console line
	 *
	 * @return
	 */
	public static String consoleLineSmooth() {
		return "______________________________________________________________";
	}

	/**
	 * Returns a long -------- chat line
	 *
	 * @return
	 */
	public static String chatLine() {
		return "*--------------------------------------------------*";
	}

	/**
	 * Returns a long ----------- chat line with strike color
	 *
	 * @return
	 */
	public static String chatLineSmooth() {
		return ChatColor.STRIKETHROUGH + "-----------------------------------------------------";
	}

	/**
	 * Returns a very long -------- config line
	 *
	 * @return
	 */
	public static String configLine() {
		return "-------------------------------------------------------------------------------------------";
	}

	/**
	 * Returns a |------------| scoreboard line with given dashes amount
	 *
	 * @param length
	 * @return
	 */
	public static String scoreboardLine(int length) {
		String fill = "";

		for (int i = 0; i < length; i++)
			fill += "-";

		return "&m|" + fill + "|";
	}

	/**
	 * Limits length to 60 chars.
	 *
	 * If JSON, unpacks it and display [json] prefix.
	 */
	public static String formatStringHover(String msg) {
		String finalText = msg;

		if (msg.startsWith("[JSON]")) {
			final String stripped = msg.replaceFirst("\\[JSON\\]", "").trim();

			if (!stripped.isEmpty())
				finalText = "&8[&6json&8] &r" + StringUtils.join(splitNewline(Remain.toLegacyText(stripped, false)));
		}

		return finalText.length() <= 60 ? finalText : finalText.substring(0, 60) + "...";
	}

	/**
	 * If the count is 0 or over 1, adds an "s" to the given string
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String plural(long count, String ofWhat) {
		return count + " " + ofWhat + (count == 0 || count > 1 && !ofWhat.endsWith("s") ? "s" : "");
	}

	/**
	 * If the count is 0 or over 1, adds an "es" to the given string
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String pluralEs(long count, String ofWhat) {
		return count + " " + ofWhat + (count == 0 || count > 1 && !ofWhat.endsWith("es") ? "es" : "");
	}

	/**
	 * If the count is 0 or over 1, adds an "ies" to the given string
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String pluralIes(long count, String ofWhat) {
		return count + " " + (count == 0 || count > 1 && !ofWhat.endsWith("ies") ? ofWhat.substring(0, ofWhat.length() - 1) + "ies" : ofWhat);
	}

	/**
	 * Prepends the given string with either "a" or "an" (does a dummy syllable check)
	 *
	 * @param ofWhat
	 * @return
	 * @deprecated only a dummy syllable check, e.g. returns a hour
	 */
	@Deprecated
	public static String article(String ofWhat) {
		Valid.checkBoolean(ofWhat.length() > 0, "String cannot be empty");
		final List<String> syllables = Arrays.asList("a", "e", "i", "o", "u", "y");

		return (syllables.contains(ofWhat.trim().substring(0, 1)) ? "an" : "a") + " " + ofWhat;
	}

	/**
	 * Generates a bar indicating progress. Example:
	 *
	 * ##-----
	 * ###----
	 * ####---
	 *
	 * @param min the min progress
	 * @param minChar
	 * @param max the max prograss
	 * @param maxChar
	 * @param delimiterColor
	 * @return
	 */
	public static String fancyBar(int min, char minChar, int max, char maxChar, ChatColor delimiterColor) {
		String formatted = "";

		for (int i = 0; i < min; i++)
			formatted += minChar;

		formatted += delimiterColor;

		for (int i = 0; i < max - min; i++)
			formatted += maxChar;

		return formatted;
	}

	/**
	 * Formats the vector location to one digit decimal points
	 *
	 * @param vec
	 * @return
	 */
	public static String shortLocation(Vector vec) {
		return " [" + MathUtil.formatOneDigit(vec.getX()) + ", " + MathUtil.formatOneDigit(vec.getY()) + ", " + MathUtil.formatOneDigit(vec.getZ()) + "]";
	}

	/**
	 * Formats the given location to block points without decimals
	 *
	 * @param loc
	 * @return
	 */
	public static String shortLocation(Location loc) {
		Valid.checkNotNull(loc, "Cannot shorten a null location!");

		if (loc.equals(new Location(null, 0, 0, 0)))
			return "Null location";

		Valid.checkNotNull(loc.getWorld(), "Cannot shorten a location with null world!");

		return loc.getWorld().getName() + " [" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Plugins management
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the plugin exist and is loaded correctly,
	 * printing a "Hooked into" console message when it does
	 *
	 * @param plugin
	 * @return
	 */
	public static boolean doesPluginExist(String plugin) {
		final boolean hooked = doesPluginExistSilently(plugin);

		if (hooked)
			log("&3Hooked into&8: &f" + plugin);

		return hooked;
	}

	/**
	 * Checks if a plugin is enabled. We also schedule an async task to make
	 * sure the plugin is loaded correctly when the server is done booting
	 *
	 * Return true if it is loaded (this does not mean it works correctly)
	 *
	 * @param pluginName
	 * @return
	 */
	public static boolean doesPluginExistSilently(final String pluginName) {
		Plugin lookup = null;

		for (final Plugin otherPlugin : Bukkit.getPluginManager().getPlugins())
			if (otherPlugin.getName().equals(pluginName)) {
				lookup = otherPlugin;
				break;
			}

		final Plugin found = lookup;

		if (found == null)
			return false;

		if (!found.isEnabled())
			runLaterAsync(0, () -> Valid.checkBoolean(found.isEnabled(), SimplePlugin.getNamed() + " could not hook into " + pluginName + " as the plugin is disabled! (DO NOT REPORT THIS TO "
					+ SimplePlugin.getNamed() + ", look for errors above and contact support " + pluginName + ")"));

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Running commands
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Runs the given command (without /) as the console, replacing {player} with sender
	 *
	 * @param sender
	 * @param command
	 */
	public static void dispatchCommand(@NonNull final CommandSender sender, @NonNull final String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		Common.runLater(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Common.colorize(command.replace("{player}", Common.resolveSenderName(sender)))));
	}

	/**
	 * Runs the given command (without /) as if the sender would type it, replacing {player} with his name
	 *
	 * @param sender
	 * @param command
	 */
	public static void dispatchCommandAsPlayer(@NonNull final Player sender, @NonNull final String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		Common.runLater(() -> sender.performCommand(Common.colorize(command.replace("{player}", Common.resolveSenderName(sender)))));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Logging and error handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Logs the message, and saves the time it was logged. If you call this method
	 * to log exactly the same message within the delay in seconds, it will not be logged.
	 *
	 * Saves console spam.
	 *
	 * @param delaySec
	 * @param msg
	 */
	public static void logTimed(int delaySec, String msg) {
		if (!TIMED_LOG_CACHE.containsKey(msg)) {
			log(msg);
			TIMED_LOG_CACHE.put(msg, TimeUtilFo.currentTimeSeconds());
			return;
		}

		if (TimeUtilFo.currentTimeSeconds() - TIMED_LOG_CACHE.get(msg) > delaySec) {
			log(msg);
			TIMED_LOG_CACHE.put(msg, TimeUtilFo.currentTimeSeconds());
		}
	}

	/**
	 * Works similarily to {@link String#format(String, Object...)} however
	 * all arguments are explored, so player names are properly given, location is shortened etc.
	 *
	 * @param format
	 * @param args
	 */
	public static void logF(String format, @NonNull Object... args) {
		for (int i = 0; i < args.length; i++) {
			final Object arg = args[i];

			if (arg != null)
				if (arg instanceof Entity)
					args[i] = ((Entity) arg).getName();
				else if (arg instanceof CommandSender)
					args[i] = ((CommandSender) arg).getName();
				else if (arg instanceof World)
					args[i] = ((World) arg).getName();
				else if (arg instanceof Location)
					args[i] = shortLocation((Location) arg);
		}

		log(false, String.format(format, args));
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param messages
	 */
	public static void log(List<String> messages) {
		log(toArray(messages));
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param messages
	 */
	public static void log(String... messages) {
		log(true, messages);
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * Does not add {@link #getTellPrefix()}
	 *
	 * @param messages
	 */
	public static void logNoPrefix(String... messages) {
		log(false, messages);
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param addTellPrefix should we add {@link #getTellPrefix()} ?
	 * @param messages
	 */
	public static void log(boolean addTellPrefix, String... messages) {
		for (String message : messages) {
			if (message.isEmpty() || message.equals("none"))
				continue;

			message = colorize(message);

			if (message.startsWith("[JSON]")) {
				final String stripped = message.replaceFirst("\\[JSON\\]", "").trim();

				if (!stripped.isEmpty())
					log(Remain.toLegacyText(stripped, false));

			} else
				for (final String part : splitNewline(message))
					CONSOLE_SENDER.sendMessage((addTellPrefix && ADD_LOG_PREFIX ? tellPrefix : "") + part.replace("\n", colorize("\n&r")));
		}
	}

	/**
	 * Logs a bunch of messages to the console in a {@link #consoleLine()} frame.
	 *
	 * @param messages
	 */
	public static void logFramed(String... messages) {
		logFramed(false, messages);
	}

	/**
	 * Logs a bunch of messages to the console in a {@link #consoleLine()} frame.
	 *
	 * Used when an error occurs, can also disable the plugin
	 *
	 * @param disablePlugin
	 * @param messages
	 */
	public static void logFramed(boolean disablePlugin, String... messages) {
		if (messages != null && !Valid.isNullOrEmpty(messages)) {
			log("&7" + consoleLine());
			for (final String msg : messages)
				log(" &c" + msg);

			if (disablePlugin)
				log(" &cPlugin is now disabled.");

			log("&7" + consoleLine());
		}

		if (disablePlugin)
			Bukkit.getPluginManager().disablePlugin(SimplePlugin.getInstance());
	}

	/**
	 * Saves the error, prints the stack trace and logs it in frame.
	 * Possible to use %error variable
	 *
	 * @param t
	 * @param messages
	 */
	public static void error(Throwable t, String... messages) {
		error(false, t, messages);
	}

	/**
	 * Saves the error, prints the stack trace and logs it in frame.
	 * Possible to use %error variable
	 *
	 * @param disablePlugin shall we disable this plugin ?
	 * @param t
	 * @param messages
	 */
	public static void error(boolean disablePlugin, Throwable t, String... messages) {
		if (!(t instanceof FoException))
			Debugger.saveError(t, messages);

		Debugger.printStackTrace(t);
		logFramed(disablePlugin, replaceErrorVariable(t, messages));
	}

	/**
	 * Logs the messages in frame (if not null),
	 * saves the error to errors.log and then throws it
	 *
	 * Possible to use %error variable
	 *
	 * @param throwable
	 * @param messages
	 */
	public static void throwError(Throwable throwable, String... messages) {
		if (throwable.getCause() != null)
			throwable = throwable.getCause();

		if (messages != null)
			logFramed(false, replaceErrorVariable(throwable, messages));

		if (!(throwable instanceof FoException))
			Debugger.saveError(throwable, messages);

		Remain.sneaky(throwable);
	}

	/**
	 * Replace the %error variable with a smart error info, see above
	 *
	 * @param throwable
	 * @param msgs
	 * @return
	 */
	private static String[] replaceErrorVariable(Throwable throwable, String... msgs) {
		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		final String throwableName = throwable == null ? "Unknown error." : throwable.getClass().getSimpleName();
		final String throwableMessage = throwable == null || throwable.getMessage() == null || throwable.getMessage().isEmpty() ? "" : ": " + throwable.getMessage();

		for (int i = 0; i < msgs.length; i++)
			msgs[i] = msgs[i].replace("%error", throwableName + throwableMessage);

		return msgs;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Regular expressions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if the given regex matches the given message
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static boolean regExMatch(String regex, String message) {
		return regExMatch(compilePattern(regex), message);
	}

	/**
	 * Returns true if the given pattern matches the given message
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static boolean regExMatch(Pattern regex, String message) {
		return regExMatch(compileMatcher(regex, message));
	}

	/**
	 * Returns true if the given matcher matches. We also evaluate
	 * how long the evaluation took and stop it in case it takes too long,
	 * see {@link SimplePlugin#getRegexTimeout()}
	 *
	 * @param matcher
	 * @return
	 */
	public static boolean regExMatch(Matcher matcher) {
		try {
			return matcher != null ? matcher.find() : false;

		} catch (final RegexTimeoutException ex) {
			FileUtil.writeFormatted(FoConstants.File.ERRORS, null, "Matching timed out (bad regex?) (plugin ver. " + SimplePlugin.getVersion() + ")! \nString checked: " + ex.getCheckedMessage() + "\nRegex: " + (matcher != null ? matcher.pattern().pattern() : "null") + "");

			logFramed(false,
					"&cRegex check took too long! (allowed: " + SimplePlugin.getInstance().getRegexTimeout() + "ms)",
					"&cRegex:&f " + (matcher != null ? matcher.pattern().pattern() : matcher),
					"&cMessage:&f " + ex.getCheckedMessage());

			return false;
		}
	}

	/**
	 * Compiles a matches for the given pattern and message. Colors are stripped.
	 *
	 * We also evaluate how long the evaluation took and stop it in case it takes too long,
	 * see {@link SimplePlugin#getRegexTimeout()}
	 *
	 * @param pattern
	 * @param message
	 * @return
	 */
	public static Matcher compileMatcher(@NonNull Pattern pattern, String message) {
		try {
			final String strippedMessage = SimplePlugin.getInstance().regexStripColors() ? stripColors(message) : message;
			final int timeout = SimplePlugin.getInstance().getRegexTimeout();

			return pattern.matcher(new TimedCharSequence(strippedMessage, timeout));

		} catch (final RegexTimeoutException ex) {
			FileUtil.writeFormatted(FoConstants.File.ERRORS, null, "Regex check timed out (bad regex?) (plugin ver. " + SimplePlugin.getVersion() + ")! \nString checked: " + ex.getCheckedMessage() + "\nRegex: " + pattern.pattern() + "");

			throwError(ex,
					"&cChecking a message took too long! (limit: " + SimplePlugin.getInstance().getRegexTimeout() + ")",
					"&cReg-ex:&f " + pattern.pattern(),
					"&cString:&f " + ex.getCheckedMessage());
			return null;
		}
	}

	/**
	 * Compiles a matcher for the given regex and message
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static Matcher compileMatcher(String regex, String message) {
		return compileMatcher(compilePattern(regex), message);
	}

	/**
	 * Compiles a pattern from the given regex, stripping colors and making
	 * it case insensitive
	 *
	 * @param regex
	 * @return
	 */
	public static Pattern compilePattern(String regex) {
		regex = SimplePlugin.getInstance().regexStripColors() ? stripColors(regex) : regex;

		Pattern pattern = null;

		try {
			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		} catch (final PatternSyntaxException ex) {
			throwError(ex, "Malformed regex: \'" + regex + "\'", "Use online services (like &fregex101.com&f) for fixing errors");

			return null;
		}

		return pattern;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Joining strings and lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Joins an array of lists together into one big list
	 *
	 * @param <T>
	 * @param arrays
	 * @return
	 */
	public static <T> List<T> joinArrays(Collection<T>... arrays) {
		final List<T> all = new ArrayList<>();

		for (final Collection<T> array : arrays)
			all.addAll(array);

		return all;
	}

	/**
	 * Join a strict list array into one big list
	 *
	 * @param <T>
	 * @param lists
	 * @return
	 */
	public static <T> StrictList<T> join(StrictList<T>... lists) {
		final StrictList<T> joined = new StrictList<>();

		for (final StrictList<T> list : lists)
			joined.addAll(list);

		return joined;
	}

	/**
	 * Joins an array together using spaces from the given start index
	 *
	 * @param startIndex
	 * @param array
	 * @return
	 */
	public static String joinRange(int startIndex, String[] array) {
		return joinRange(startIndex, array.length, array);
	}

	/**
	 * Join an array together using spaces using the given range
	 *
	 * @param startIndex
	 * @param stopIndex
	 * @param array
	 * @return
	 */
	public static String joinRange(int startIndex, int stopIndex, String[] array) {
		return joinRange(startIndex, stopIndex, array, " ");
	}

	/**
	 * Join an array together using the given deliminer
	 *
	 * @param start
	 * @param stop
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static String joinRange(int start, int stop, String[] array, String delimiter) {
		String joined = "";

		for (int i = start; i < MathUtil.range(stop, 0, array.length); i++)
			joined += (joined.isEmpty() ? "" : delimiter) + array[i];

		return joined;
	}

	/**
	 * Joins an array of a given type using the given delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static <T> String join(T[] array, String delimiter, Stringer<T> stringer) {
		return join(Arrays.asList(array), delimiter, stringer);
	}

	/**
	 * A convenience method for converting array of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String joinToString(T[] array) {
		return joinToString(Arrays.asList(array));
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String joinToString(Iterable<T> array) {
		return joinToString(array, ", ");
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static <T> String joinToString(Iterable<T> array, String delimiter) {
		return join(array, delimiter, (object) -> object == null ? "" : object.toString());
	}

	/**
	 * A convenience method for converting array of command senders into array of their names
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T extends CommandSender> String joinPlayers(Iterable<T> array) {
		return join(array, ", ", (Stringer<T>) object -> object.getName());
	}

	/**
	 * Joins a list of a given type using the given delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static <T> String join(Iterable<T> array, String delimiter, Stringer<T> stringer) {
		final Iterator<T> it = array.iterator();
		String message = "";

		while (it.hasNext()) {
			final T next = it.next();

			message += stringer.toString(next) + (it.hasNext() ? delimiter : "");
		}

		return message;
	}

	/**
	 * A simple interface from converting objects into strings
	 *
	 * @param <T>
	 */
	public interface Stringer<T> {

		/**
		 * Convert the given object into a string
		 *
		 * @param object
		 * @return
		 */
		String toString(T object);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting and retyping
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for getting a list of world names
	 *
	 * @return
	 */
	public static List<String> getWorldNames() {
		return convert(Bukkit.getWorlds(), (world) -> world.getName());
	}

	/**
	 * Convenience method for getting a list of player names
	 *
	 * @return
	 */
	public static List<String> getPlayerNames() {
		return getPlayerNames(true);
	}

	/**
	 * Convenience method for getting a list of player names
	 *
	 * @param includeVanished
	 * @return
	 */
	public static List<String> getPlayerNames(boolean includeVanished) {
		final List<String> found = new ArrayList<>();

		for (final Player online : Remain.getOnlinePlayers()) {
			if (PlayerUtil.isVanished(online) && !includeVanished)
				continue;

			found.add(online.getName());
		}

		return found;
	}

	/**
	 * Converts a list having one type object into another
	 *
	 * @param list the old list
	 * @param converter the converter
	 * @return the new list
	 */
	public static <OLD, NEW> List<NEW> convert(Iterable<OLD> list, TypeConverter<OLD, NEW> converter) {
		final List<NEW> copy = new ArrayList<>();

		for (final OLD old : list)
			copy.add(converter.convert(old));

		return copy;
	}

	/**
	 * Converts a list having one type object into another
	 *
	 * @param list the old list
	 * @param converter the converter
	 * @return the new list
	 */
	public static <OLD, NEW> StrictList<NEW> convertStrict(Iterable<OLD> list, TypeConverter<OLD, NEW> converter) {
		final StrictList<NEW> copy = new StrictList<>();

		for (final OLD old : list)
			copy.add(converter.convert(old));

		return copy;
	}

	/**
	 * Attempts to convert the given map into another map
	 *
	 * @param <OLD_KEY>
	 * @param <OLD_VALUE>
	 * @param <NEW_KEY>
	 * @param <NEW_VALUE>
	 * @param oldMap
	 * @param converter
	 * @return
	 */
	public static <OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> Map<NEW_KEY, NEW_VALUE> convert(Map<OLD_KEY, OLD_VALUE> oldMap, MapToMapConverter<OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> converter) {
		final Map<NEW_KEY, NEW_VALUE> newMap = new HashMap<>();
		oldMap.entrySet().forEach((e) -> newMap.put(converter.convertKey(e.getKey()), converter.convertValue(e.getValue())));

		return newMap;
	}

	/**
	 * Attempts to convert the given map into another map
	 *
	 * @param <OLD_KEY>
	 * @param <OLD_VALUE>
	 * @param <NEW_KEY>
	 * @param <NEW_VALUE>
	 * @param oldMap
	 * @param converter
	 * @return
	 */
	public static <OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> StrictMap<NEW_KEY, NEW_VALUE> convertStrict(Map<OLD_KEY, OLD_VALUE> oldMap, MapToMapConverter<OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> converter) {
		final StrictMap<NEW_KEY, NEW_VALUE> newMap = new StrictMap<>();
		oldMap.entrySet().forEach((e) -> newMap.put(converter.convertKey(e.getKey()), converter.convertValue(e.getValue())));

		return newMap;
	}

	/**
	 * Attempts to convert the gfiven map into a list
	 *
	 * @param <LIST_KEY>
	 * @param <OLD_KEY>
	 * @param <OLD_VALUE>
	 * @param map
	 * @param converter
	 * @return
	 */
	public static <LIST_KEY, OLD_KEY, OLD_VALUE> StrictList<LIST_KEY> convertToList(Map<OLD_KEY, OLD_VALUE> map, MapToListConverter<LIST_KEY, OLD_KEY, OLD_VALUE> converter) {
		final StrictList<LIST_KEY> list = new StrictList<>();

		for (final Entry<OLD_KEY, OLD_VALUE> e : map.entrySet())
			list.add(converter.convert(e.getKey(), e.getValue()));

		return list;
	}

	/**
	 * Attempts to convert an array into a different type
	 *
	 * @param <OLD_TYPE>
	 * @param <NEW_TYPE>
	 * @param oldArray
	 * @param converter
	 * @return
	 */
	public static <OLD_TYPE, NEW_TYPE> List<NEW_TYPE> convert(OLD_TYPE[] oldArray, TypeConverter<OLD_TYPE, NEW_TYPE> converter) {
		final List<NEW_TYPE> newList = new ArrayList<>();

		for (final OLD_TYPE old : oldArray)
			newList.add(converter.convert(old));

		return newList;
	}

	/**
	 * A simple interface to convert between types
	 *
	 * @param <Old> the initial type to convert from
	 * @param <New> the final type to convert to
	 */
	public interface TypeConverter<Old, New> {

		/**
		 * Convert a type given from A to B
		 *
		 * @param value the old value type
		 * @return the new value type
		 */
		New convert(Old value);
	}

	/**
	 * Convenience class for converting map to a list
	 *
	 * @param <O>
	 * @param <K>
	 * @param <V>
	 */
	public interface MapToListConverter<O, K, V> {

		/**
		 * Converts the given map key-value pair into a new type stored in a list
		 *
		 * @param key
		 * @param value
		 * @return
		 */
		O convert(K key, V value);
	}

	/**
	 * Convenience class for converting between maps
	 *
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 */
	public interface MapToMapConverter<A, B, C, D> {

		/**
		 * Converts the old key type to a new type
		 *
		 * @param key
		 * @return
		 */
		C convertKey(A key);

		/**
		 * Converts the old value into a new value type
		 *
		 * @param value
		 * @return
		 */
		D convertValue(B value);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc message handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Attempts to split the message using the \n character. This is used in some plugins
	 * since some OS's have a different method for splitting so we just go letter by letter
	 * there and match \ and n and then split it.
	 *
	 * @param message
	 * @return
	 * @deprecated usage specific, also some operating systems seems to handle this poorly
	 */
	@Deprecated
	public static String[] splitNewline(String message) {
		if (!SimplePlugin.getInstance().enforeNewLine())
			return message.split("\n");

		final String delimiter = "KANGARKOJESUUPER";

		final char[] chars = message.toCharArray();
		String parts = "";

		for (int i = 0; i < chars.length; i++) {
			final char c = chars[i];

			if ('\\' == c)
				if (i + 1 < chars.length)
					if ('n' == chars[i + 1]) {
						i++;

						parts += delimiter;
						continue;
					}
			parts += c;
		}

		return parts.split(delimiter);
	}

	/**
	 * Replaces string by a substitute for each element in the array
	 *
	 * @param what
	 * @param byWhat
	 * @param messages
	 * @return
	 */
	public static String[] replace(String what, String byWhat, String... messages) {
		for (int i = 0; i < messages.length; i++)
			messages[i] = messages[i].replace(what, byWhat);

		return messages;
	}

	/**
	 * Replaces string by a substitute for each element in the list
	 *
	 * @param what
	 * @param byWhat
	 * @param messages
	 * @return
	 */
	public static List<String> replace(String what, String byWhat, List<String> messages) {
		for (int i = 0; i < messages.size(); i++)
			messages.set(i, messages.get(i).replace(what, byWhat));

		return messages;
	}

	/**
	 * REplaces all nulls with an empty string
	 *
	 * @param list
	 * @return
	 */
	public static String[] replaceNuls(String[] list) {
		for (int i = 0; i < list.length; i++)
			if (list[i] == null)
				list[i] = "";

		return list;
	}

	/**
	 * Creates a new list only containing non-null and not empty string elements
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> List<T> removeNulsAndEmpties(T[] array) {
		return array != null ? removeNulsAndEmpties(Arrays.asList(array)) : new ArrayList<>();
	}

	/**
	 * Creates a new list only containing non-null and not empty string elements
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static <T> List<T> removeNulsAndEmpties(List<T> list) {
		final List<T> copy = new ArrayList<>();

		for (int i = 0; i < list.size(); i++) {
			final T key = list.get(i);

			if (key != null)
				if (key instanceof String) {
					if (!((String) key).isEmpty())
						copy.add(key);
				} else
					copy.add(key);
		}

		return copy;
	}

	/**
	 * Get an array from the given object. If the object
	 * is a list, return its array, otherwise return an array only containing the
	 * object as the first element
	 *
	 *
	 * @param obj
	 * @return
	 */
	public static String[] getListOrString(Object obj) {
		if (obj instanceof List) {
			final List<String> cast = (List<String>) obj;

			return toArray(cast);
		}

		return new String[] { obj.toString() };
	}

	/**
	 * Return an empty String if the String is null or equals to none.
	 *
	 * @param input
	 * @return
	 */
	public static String getOrEmpty(String input) {
		return input == null || "none".equalsIgnoreCase(input) ? "" : input;
	}

	/**
	 * Returns the value or its default counterpart in case it is null
	 *
	 * @param value the primary value
	 * @param def the default value
	 * @return the value, or default it the value is null
	 */
	public static <T> T getOrDefault(T value, T def) {
		Objects.requireNonNull(def, "The default value must not be null!");

		return value != null ? value : def;
	}

	/**
	 * Return the default value if the given string is null, "" or equals to "none"
	 *
	 * @param input
	 * @param def
	 * @return
	 */
	public static String getOrSupply(String input, String def) {
		return input == null || "none".equalsIgnoreCase(input) || input.isEmpty() ? def : input;
	}

	/**
	 * Converts a list of string into a string array
	 *
	 * @param array
	 * @return
	 */
	public static String[] toArray(Collection<String> array) {
		return array.toArray(new String[array.size()]);
	}

	/**
	 * Converts a string array into a list of strings
	 *
	 * @param array
	 * @return
	 */
	public static ArrayList<String> toList(String... array) {
		return new ArrayList<>(Arrays.asList(array));
	}

	/**
	 * Converts {@link Iterable} to {@link List}
	 *
	 * @param it the iterable
	 * @return the new list
	 */
	public static <T> List<T> toList(Iterable<T> it) {
		final List<T> list = new ArrayList<>();
		it.forEach((el) -> list.add(el));

		return list;
	}

	/**
	 * Reverses elements in the array
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> T[] reverse(T[] array) {
		if (array == null)
			return null;

		int i = 0;
		int j = array.length - 1;

		while (j > i) {
			final T tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}

		return array;
	}

	/**
	 * Lowercases all items in the array
	 *
	 * @param list
	 * @return
	 */
	public static String[] toLowerCase(String... list) {
		for (int i = 0; i < list.length; i++)
			list[i] = list[i].toLowerCase();

		return list;
	}

	/**
	 * Return a new hashmap having the given first key and value pair
	 *
	 * @param <A>
	 * @param <B>
	 * @param firstKey
	 * @param firstValue
	 * @return
	 */
	public static <A, B> Map<A, B> newHashMap(A firstKey, B firstValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);

		return map;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Bukkit scheduling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Runs the task if the condition is met
	 *
	 * @param condition
	 * @param task
	 */
	public static void runLaterIf(boolean condition, Runnable task) {
		if (condition)
			runLater(1, task);
		else
			task.run();
	}

	/**
	 * Runs the task if the plugin is enabled correctly
	 *
	 * @param task the task
	 * @return the task or null
	 */
	public static BukkitTask runLater(Runnable task) {
		return runLater(1, task);
	}

	/**
	 * Runs the task even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param task
	 * @return the task or null
	 */
	public static BukkitTask runLater(int delayTicks, Runnable task) {
		final BukkitScheduler scheduler = Bukkit.getScheduler();
		final JavaPlugin instance = SimplePlugin.getInstance();

		return runIfDisabled(task) ? null
				: delayTicks == 0 ? task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTask(instance) : scheduler.runTask(instance, task)
						: task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskLater(instance, delayTicks) : scheduler.runTaskLater(instance, task, delayTicks);
	}

	/**
	 * Runs the task async even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param task
	 * @return the task or null
	 */
	public static BukkitTask runLaterAsync(int delayTicks, Runnable task) {
		final BukkitScheduler scheduler = Bukkit.getScheduler();
		final JavaPlugin instance = SimplePlugin.getInstance();

		return runIfDisabled(task) ? null
				: delayTicks == 0 ? task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskAsynchronously(instance)
						: scheduler.runTaskAsynchronously(instance, task)
						: task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskLaterAsynchronously(instance, delayTicks)
								: scheduler.runTaskLaterAsynchronously(instance, task, delayTicks);
	}

	/**
	 * Runs the task timer even if the plugin is disabled.
	 *
	 * @param repeatTicks the delay between each execution
	 * @param task the task
	 * @return the bukkit task or null
	 */
	public static BukkitTask runTimer(int repeatTicks, Runnable task) {
		return runTimer(0, repeatTicks, task);
	}

	/**
	 *	Runs the task timer even if the plugin is disabled.
	 *
	 * @param delayTicks the delay before first run
	 * @param repeatTicks the delay between each run
	 * @param task the task
	 * @return the bukkit task or null if error
	 */
	public static BukkitTask runTimer(int delayTicks, int repeatTicks, Runnable task) {
		return runIfDisabled(task) ? null
				: task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskTimer(SimplePlugin.getInstance(), delayTicks, repeatTicks)
						: Bukkit.getScheduler().runTaskTimer(SimplePlugin.getInstance(), task, delayTicks, repeatTicks);
	}

	/**
	 * Runs the task timer async even if the plugin is disabled.
	 *
	 * @param repeatTicks
	 * @param task
	 * @return
	 */
	public static BukkitTask runTimerAsync(int repeatTicks, Runnable task) {
		return runTimerAsync(0, repeatTicks, task);
	}

	/**
	 * Runs the task timer async even if the plugin is disabled.
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param task
	 * @return
	 */
	public static BukkitTask runTimerAsync(int delayTicks, int repeatTicks, Runnable task) {
		return runIfDisabled(task) ? null
				: task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskTimerAsynchronously(SimplePlugin.getInstance(), delayTicks, repeatTicks)
						: Bukkit.getScheduler().runTaskTimerAsynchronously(SimplePlugin.getInstance(), task, delayTicks, repeatTicks);
	}

	// Check our plugin instance if it's enabled
	// In case it is disabled, just runs the task and returns true
	// Otherwise we return false and the task will be run correctly in Bukkit scheduler
	// This is fail-safe to critical save-on-exit operations in case our plugin is improperly reloaded (PlugMan) or malfunctions
	private static boolean runIfDisabled(Runnable run) {
		if (!SimplePlugin.getInstance().isEnabled()) {
			run.run();

			return true;
		}

		return false;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Bukkit
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Call an event in Bukkit and return whether it was NOT cancelled
	 *
	 * @param event the event
	 * @return true if the event was not cancelled
	 */
	public static boolean callEvent(Event event) {
		Bukkit.getPluginManager().callEvent(event);

		return event instanceof Cancellable ? !((Cancellable) event).isCancelled() : true;
	}

	/**
	 * Convenience method for registering events as our instance
	 *
	 * @param listener
	 */
	public static void registerEvents(Listener listener) {
		Bukkit.getPluginManager().registerEvents(listener, SimplePlugin.getInstance());
	}

	/**
	 * Resolves the inner Map in a Bukkit's {@link MemorySection}
	 *
	 * @param mapOrSection
	 * @return
	 */
	public static Map<String, Object> getMapFromSection(@NonNull Object mapOrSection) {
		final Map<String, Object> map = mapOrSection instanceof Map ? (Map<String, Object>) mapOrSection : mapOrSection instanceof MemorySection ? ReflectionUtil.getField(mapOrSection, "map", Map.class) : null;
		Valid.checkNotNull(map, "Unexpected " + mapOrSection.getClass().getSimpleName() + " '" + mapOrSection + "'. Must be Map or MemorySection! (Do not just send config name here, but the actual section with get('section'))");

		return map;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Connecting to the internet
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if the domain is reachable. Method is blocking.
	 *
	 * @param url
	 * @param timeout
	 * @return
	 */
	public static boolean isDomainReachable(String url, int timeout) {
		url = url.replaceFirst("^https", "http");

		try {
			final HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();

			c.setConnectTimeout(timeout);
			c.setReadTimeout(timeout);
			c.setRequestMethod("HEAD");

			final int responseCode = c.getResponseCode();
			return 200 <= responseCode && responseCode <= 399;

		} catch (final IOException exception) {
			return false;
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Java convenience methods
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Checked sleep method from {@link Thread#sleep(long)} but without the try-catch need
	 *
	 * @param millis
	 */
	public static void sleep(int millis) {
		try {
			Thread.sleep(millis);

		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
}

/**
 * Represents a timed chat sequence, used when checking for
 * regular expressions so we time how long it takes and
 * stop the execution if takes too long
 */
final class TimedCharSequence implements CharSequence {

	/**
	 * The timed message
	 */
	private final CharSequence message;

	/**
	 * The timeout limit in millis
	 */
	private final int timeoutLimit;

	/**
	 * Create a new timed message for the given message with a timeout in millis
	 *
	 * @param message
	 * @param timeoutLimit
	 */
	public TimedCharSequence(CharSequence message, Integer timeoutLimit) {
		Valid.checkNotNull(message, "msg = null");
		Valid.checkNotNull(timeoutLimit, "timeout = null");

		this.message = message;
		this.timeoutLimit = timeoutLimit;
	}

	/**
	 * Gets a character at the given index, or throws an error if
	 * this is called too late after the constructor, see {@link #timeoutLimit}
	 */
	@Override
	public char charAt(int index) {
		if (System.currentTimeMillis() > System.currentTimeMillis() + timeoutLimit)
			throw new RegexTimeoutException(message, timeoutLimit);

		return message.charAt(index);
	}

	@Override
	public int length() {
		return message.length();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return new TimedCharSequence(message.subSequence(start, end), timeoutLimit);
	}

	@Override
	public String toString() {
		return message.toString();
	}
}