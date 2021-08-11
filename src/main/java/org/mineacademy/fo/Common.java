package org.mineacademy.fo;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
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
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.RegexTimeoutException;
import org.mineacademy.fo.model.DiscordSender;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Our main utility class hosting a large variety of different convenience functions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Common {

	// ------------------------------------------------------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Pattern used to match colors with & or {@link ChatColor#COLOR_CHAR}
	 */
	private static final Pattern COLOR_AND_DECORATION_REGEX = Pattern.compile("(&|" + ChatColor.COLOR_CHAR + ")[0-9a-fk-orA-FK-OR]");

	/**
	 * Pattern used to match colors with #HEX code for MC 1.16+
	 */
	public static final Pattern RGB_HEX_COLOR_REGEX = Pattern.compile("(?<!\\\\)(&|)#((?:[0-9a-fA-F]{3}){1,2})");

	/**
	 * Pattern used to match colors with {#HEX} code for MC 1.16+
	 */
	public static final Pattern RGB_HEX_BRACKET_COLOR_REGEX = Pattern.compile("\\{#((?:[0-9a-fA-F]{3}){1,2})\\}");

	/**
	 * Pattern used to match colors with #HEX code for MC 1.16+
	 */
	private static final Pattern RGB_X_COLOR_REGEX = Pattern.compile("(" + ChatColor.COLOR_CHAR + "x)(" + ChatColor.COLOR_CHAR + "[0-9a-fA-F]){6}");

	/**
	 * We use this to send messages with colors to your console
	 */
	private static final CommandSender CONSOLE_SENDER = Bukkit.getServer() != null ? Bukkit.getServer().getConsoleSender() : null;

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
	 * <p>
	 * False by default
	 */
	public static boolean ADD_TELL_PREFIX = false;

	/**
	 * Should we add a prefix to the messages we send to the console?
	 * <p>
	 * True by default
	 */
	public static boolean ADD_LOG_PREFIX = true;

	/**
	 * The tell prefix applied on tell() methods
	 */
	@Getter
	private static String tellPrefix = "[" + SimplePlugin.getNamed() + "]";

	/**
	 * The log prefix applied on log() methods
	 */
	@Getter
	private static String logPrefix = "[" + SimplePlugin.getNamed() + "]";

	/**
	 * Set the tell prefix applied for messages to players from tell() methods
	 * <p>
	 * Colors with & letter are translated automatically.
	 *
	 * @param prefix
	 */
	public static void setTellPrefix(final String prefix) {
		tellPrefix = colorize(prefix);
	}

	/**
	 * Set the log prefix applied for messages in the console from log() methods.
	 * <p>
	 * Colors with & letter are translated automatically.
	 *
	 * @param prefix
	 */
	public static void setLogPrefix(final String prefix) {
		logPrefix = colorize(prefix);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Broadcasting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Broadcast the message as per {@link Replacer#replaceArray(String, Object...)} mechanics
	 * such as broadcastReplaced("Hello {world} from {player}", "world", "survival_world", "player", "kangarko")
	 *
	 * @param message
	 * @param replacements
	 */
	public static void broadcastReplaced(final String message, final Object... replacements) {
		broadcast(Replacer.replaceArray(message, replacements));
	}

	/**
	 * Broadcast the message replacing {player} variable with the given command sender
	 *
	 * @param message
	 * @param sender
	 */
	public static void broadcast(final String message, final CommandSender sender) {
		broadcast(message, resolveSenderName(sender));
	}

	/**
	 * Broadcast the message replacing {player} variable with the given player replacement
	 *
	 * @param message
	 * @param playerReplacement
	 */
	public static void broadcast(final String message, final String playerReplacement) {
		broadcast(message.replace("{player}", playerReplacement));
	}

	/**
	 * Broadcast the message to everyone and logs it
	 *
	 * @param message
	 */
	public static void broadcast(final String... messages) {
		if (!Valid.isNullOrEmpty(messages))
			for (final String message : messages) {
				for (final Player online : Remain.getOnlinePlayers())
					tellJson(online, message);

				log(message);
			}
	}

	/**
	 * Sends messages to all recipients
	 *
	 * @param recipients
	 * @param messages
	 */
	public static void broadcastTo(final Iterable<? extends CommandSender> recipients, final String... messages) {
		for (final CommandSender sender : recipients)
			tell(sender, messages);
	}

	/**
	 * Broadcast the message to everyone with permission
	 *
	 * @param showPermission
	 * @param message
	 * @param log
	 */
	public static void broadcastWithPerm(final String showPermission, final String message, final boolean log) {
		if (message != null && !message.equals("none")) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, showPermission))
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
	 * @param log
	 */
	public static void broadcastWithPerm(final String permission, @NonNull final TextComponent message, final boolean log) {
		final String legacy = message.toLegacyText();

		if (!legacy.equals("none")) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, permission))
					Remain.sendComponent(online, message);

			if (log)
				log(legacy);
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Messaging
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Sends a message to the player and saves the time when it was sent.
	 * The delay in seconds is the delay between which we won't send player the
	 * same message, in case you call this method again.
	 * <p>
	 * Does not prepend the message with {@link #getTellPrefix()}
	 * <p>
	 * See {@link #TIMED_TELL_CACHE} for more explanation.
	 *
	 * @param delaySeconds
	 * @param sender
	 * @param message
	 */
	public static void tellTimedNoPrefix(final int delaySeconds, final CommandSender sender, final String message) {
		final boolean hadPrefix = ADD_TELL_PREFIX;
		ADD_TELL_PREFIX = false;

		tellTimed(delaySeconds, sender, message);

		ADD_TELL_PREFIX = hadPrefix;
	}

	/**
	 * Sends a message to the player and saves the time when it was sent.
	 * The delay in seconds is the delay between which we won't send player the
	 * same message, in case you call this method again.
	 * <p>
	 * See {@link #TIMED_TELL_CACHE} for more explanation.
	 *
	 * @param delaySeconds
	 * @param sender
	 * @param message
	 */
	public static void tellTimed(final int delaySeconds, final CommandSender sender, final String message) {

		// No previous message stored, just tell the player now
		if (!TIMED_TELL_CACHE.containsKey(message)) {
			tell(sender, message);

			TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
			return;
		}

		if (TimeUtil.currentTimeSeconds() - TIMED_TELL_CACHE.get(message) > delaySeconds) {
			tell(sender, message);

			TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
		}
	}

	/**
	 * Sends the conversable a message later
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	public static void tellLaterConversing(final int delayTicks, final Conversable conversable, final String message) {
		runLater(delayTicks, () -> tellConversing(conversable, message));
	}

	/**
	 * Sends the conversable player a colorized message
	 *
	 * @param conversable
	 * @param message
	 */
	public static void tellConversing(final Conversable conversable, final String message) {
		conversable.sendRawMessage(colorize((ADD_TELL_PREFIX ? tellPrefix : "") + removeFirstSpaces(message)).trim());
	}

	/**
	 * Sends a message to the sender with a given delay, colors & are supported
	 *
	 * @param sender
	 * @param delayTicks
	 * @param messages
	 */
	public static void tellLater(final int delayTicks, final CommandSender sender, final String... messages) {
		runLater(delayTicks, () -> {
			if (sender instanceof Player && !((Player) sender).isOnline())
				return;

			tell(sender, messages);
		});
	}

	/**
	 * Sends sender a message with {} variables replaced and colors supported
	 * without the {@link #getTellPrefix()}
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tellNoPrefix(final CommandSender sender, final Replacer replacer) {
		tellNoPrefix(sender, replacer.getReplacedMessage());
	}

	/**
	 * Sends the sender a bunch of messages, colors & are supported
	 * without {@link #getTellPrefix()} prefix
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tellNoPrefix(final CommandSender sender, final String... messages) {
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
	public static void tell(final CommandSender sender, final Collection<String> messages) {
		tell(sender, toArray(messages));
	}

	/**
	 * Sends sender a bunch of messages, ignoring the ones that equal "none" or null,
	 * replacing & colors and {player} with his variable
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tell(final CommandSender sender, final String... messages) {
		for (final String message : messages)
			if (message != null && !"none".equals(message))
				tellJson(sender, message);
	}

	/**
	 * Sends a message to the player replacing the given associative array of placeholders in the given message
	 *
	 * @param recipient
	 * @param message
	 * @param replacements
	 */
	public static void tellReplaced(CommandSender recipient, String message, Object... replacements) {
		tell(recipient, Replacer.replaceArray(message, replacements));
	}

	/*
	 * Tells the sender a basic message with & colors replaced and {player} with his variable replaced.
	 * <p>
	 * If the message starts with [JSON] than we remove the [JSON] prefix and handle the message
	 * as a valid JSON component.
	 * <p>
	 * Finally, a prefix to non-json messages is added, see {@link #getTellPrefix()}
	 */
	private static void tellJson(@NonNull final CommandSender sender, String message) {
		if (message.isEmpty() || "none".equals(message))
			return;

		// Has prefix already? This is replaced when colorizing
		final boolean hasPrefix = message.contains("{prefix}");
		final boolean hasJSON = message.startsWith("[JSON]");

		// Replace player
		message = message.replace("{player}", resolveSenderName(sender));

		// Replace colors
		if (!hasJSON)
			message = colorize(message);

		// Used for matching
		final String colorlessMessage = stripColors(message);

		// Send [JSON] prefixed messages as json component
		if (hasJSON) {
			final String stripped = message.substring(6).trim();

			if (!stripped.isEmpty())
				Remain.sendJson(sender, stripped);

		} else if (colorlessMessage.startsWith("<actionbar>")) {
			final String stripped = message.replace("<actionbar>", "");

			if (!stripped.isEmpty())
				if (sender instanceof Player)
					Remain.sendActionBar((Player) sender, stripped);
				else
					tellJson(sender, stripped);

		} else if (colorlessMessage.startsWith("<toast>")) {
			final String stripped = message.replace("<toast>", "");

			if (!stripped.isEmpty())
				if (sender instanceof Player)
					Remain.sendToast((Player) sender, stripped);
				else
					tellJson(sender, stripped);

		} else if (colorlessMessage.startsWith("<title>")) {
			final String stripped = message.replace("<title>", "");

			if (!stripped.isEmpty()) {
				final String[] split = stripped.split("\\|");
				final String title = split[0];
				final String subtitle = split.length > 1 ? Common.joinRange(1, split) : null;

				if (sender instanceof Player)
					Remain.sendTitle((Player) sender, title, subtitle);

				else {
					tellJson(sender, title);

					if (subtitle != null)
						tellJson(sender, subtitle);
				}
			}

		} else if (colorlessMessage.startsWith("<bossbar>")) {
			final String stripped = message.replace("<bossbar>", "");

			if (!stripped.isEmpty()) {
				if (sender instanceof Player)
					// cannot provide time here so we show it for 10 seconds
					Remain.sendBossbarTimed((Player) sender, stripped, 10);
				else
					tellJson(sender, stripped);
			}

		} else
			for (final String part : splitNewline(message)) {
				final String prefixStripped = removeSurroundingSpaces(tellPrefix);
				final String prefix = ADD_TELL_PREFIX && !hasPrefix && !prefixStripped.isEmpty() ? prefixStripped + " " : "";

				String toSend;

				if (Common.stripColors(part).startsWith("<center>"))
					toSend = ChatUtil.center(prefix + part.replace("<center>", ""));
				else
					toSend = prefix + part;

				if (MinecraftVersion.olderThan(V.v1_9) && toSend.length() + 1 >= Short.MAX_VALUE) {
					toSend = toSend.substring(0, Short.MAX_VALUE / 2);

					Common.log("Warning: Message to " + sender.getName() + " was too large, sending the first 16,000 letters: " + toSend);
				}

				// Make player engaged in a server conversation still receive the message
				if (sender instanceof Conversable && ((Conversable) sender).isConversing())
					((Conversable) sender).sendRawMessage(toSend);

				else
					sender.sendMessage(toSend);
			}
	}

	/**
	 * Return the sender's name if it's a player or discord sender, or simply {@link SimplePlugin#getConsoleName()} if it is a console
	 *
	 * @param sender
	 * @return
	 */
	public static String resolveSenderName(final CommandSender sender) {
		return sender instanceof Player || sender instanceof DiscordSender ? sender.getName() : SimpleLocalization.CONSOLE_NAME;
	}

	// Remove first spaces from the given message
	private static String removeFirstSpaces(String message) {
		message = getOrEmpty(message);

		while (message.startsWith(" "))
			message = message.substring(1);

		return message;
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
	public static List<String> colorize(final List<String> list) {
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
	 * Replace the & letter with the {@link org.bukkit.CompChatColor.COLOR_CHAR} in the message.
	 *
	 * @param messages the messages to replace color codes with '&'
	 * @return the colored message
	 */
	public static String colorize(final String... messages) {
		return colorize(StringUtils.join(messages, "\n"));
	}

	/**
	 * Replace the & letter with the {@link org.bukkit.CompChatColor.COLOR_CHAR} in the message.
	 *
	 * @param messages the messages to replace color codes with '&'
	 * @return the colored message
	 */
	public static String[] colorizeArray(final String... messages) {

		for (int i = 0; i < messages.length; i++)
			messages[i] = colorize(messages[i]);

		return messages;
	}

	/**
	 * Replace the & letter with the {@link org.bukkit.CompChatColor.COLOR_CHAR} in the message.
	 * <p>
	 * Also replaces {prefix} with {@link #getTellPrefix()} and {server} with {@link SimplePlugin#getServerPrefix()}
	 *
	 * @param message the message to replace color codes with '&'
	 * @return the colored message
	 */
	public static String colorize(final String message) {
		if (message == null || message.isEmpty())
			return "";

		String result = ChatColor.translateAlternateColorCodes('&', message
				.replace("{prefix}", message.startsWith(tellPrefix) ? "" : removeSurroundingSpaces(tellPrefix.trim()))
				.replace("{server}", SimpleLocalization.SERVER_PREFIX)
				.replace("{plugin_name}", SimplePlugin.getNamed())
				.replace("{plugin_version}", SimplePlugin.getVersion()));

		// RGB colors
		if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_16)) {

			// Preserve compatibility with former systems
			Matcher match = RGB_HEX_BRACKET_COLOR_REGEX.matcher(result);

			while (match.find()) {
				final String colorCode = match.group(1);
				String replacement = "";

				try {
					replacement = CompChatColor.of("#" + colorCode).toString();

				} catch (final IllegalArgumentException ex) {
				}

				result = result.replaceAll("\\{#" + colorCode + "\\}", replacement);
			}

			match = RGB_HEX_COLOR_REGEX.matcher(result);

			while (match.find()) {
				final String colorCode = match.group(2);
				String replacement = "";

				try {
					replacement = CompChatColor.of("#" + colorCode).toString();

				} catch (final IllegalArgumentException ex) {
				}

				result = result.replaceAll("(&|)#" + colorCode, replacement);
			}

			result = result.replace("\\#", "#");
		}

		return result;
	}

	// Remove first and last spaces from the given message
	private static String removeSurroundingSpaces(String message) {
		message = getOrEmpty(message);

		while (message.endsWith(" "))
			message = message.substring(0, message.length() - 1);

		return removeFirstSpaces(message);
	}

	/**
	 * Replaces the {@link ChatColor#COLOR_CHAR} colors with & letters
	 *
	 * @param messages
	 * @return
	 */
	public static String[] revertColorizing(final String[] messages) {
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
	public static String revertColorizing(final String message) {
		return message.replaceAll("(?i)" + ChatColor.COLOR_CHAR + "([0-9a-fk-or])", "&$1");
	}

	/**
	 * Remove all {@link ChatColor#COLOR_CHAR} as well as & letter colors from the message
	 *
	 * @param message
	 * @return
	 */
	public static String stripColors(String message) {

		if (message == null || message.isEmpty())
			return message;

		// Replace & color codes
		Matcher matcher = COLOR_AND_DECORATION_REGEX.matcher(message);

		while (matcher.find())
			message = matcher.replaceAll("");

		// Replace hex colors, both raw and parsed
		if (Remain.hasHexColors()) {
			matcher = RGB_HEX_COLOR_REGEX.matcher(message);

			while (matcher.find())
				message = matcher.replaceAll("");

			matcher = RGB_X_COLOR_REGEX.matcher(message);

			while (matcher.find())
				message = matcher.replaceAll("");

			message = message.replace(ChatColor.COLOR_CHAR + "x", "");
		}

		return message;
	}

	/**
	 * Only remove the & colors from the message
	 *
	 * @param message
	 * @return
	 */
	public static String stripColorsLetter(final String message) {
		return message == null ? "" : message.replaceAll("&([0-9a-fk-orA-F-K-OR])", "");
	}

	/**
	 * Returns if the message contains either {@link ChatColor#COLOR_CHAR} or & letter colors
	 *
	 * @param message
	 * @return
	 */
	public static boolean hasColors(final String message) {
		return COLOR_AND_DECORATION_REGEX.matcher(message).find();
	}

	/**
	 * Returns the last color, either & or {@link ChatColor#COLOR_CHAR} from the given message
	 *
	 * @param message, or empty if none
	 * @return
	 */
	public static String lastColor(final String message) {

		// RGB colors
		if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_16)) {
			final int c = message.lastIndexOf(ChatColor.COLOR_CHAR);
			final Matcher match = RGB_X_COLOR_REGEX.matcher(message);

			String lastColor = null;

			while (match.find())
				lastColor = match.group(0);

			if (lastColor != null)
				if (c == -1 || c < message.lastIndexOf(lastColor) + lastColor.length())
					return lastColor;
		}

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
	public static String lastColorLetter(final String message) {
		return lastColor(message, '&');
	}

	/**
	 * Return last {@link ChatColor#COLOR_CHAR} + the color letter from the message, or empty if not exist
	 *
	 * @param message
	 * @return
	 */
	public static String lastColorChar(final String message) {
		return lastColor(message, ChatColor.COLOR_CHAR);
	}

	private static String lastColor(final String msg, final char colorChar) {
		final int c = msg.lastIndexOf(colorChar);

		// Contains our character
		if (c != -1) {

			// Contains a character after color character
			if (msg.length() > c + 1)
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
		return "*---------------------------------------------------*";
	}

	/**
	 * Returns a long &m----------- chat line with strike effect
	 *
	 * @return
	 */
	public static String chatLineSmooth() {
		return "&m-----------------------------------------------------";
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
	public static String scoreboardLine(final int length) {
		String fill = "";

		for (int i = 0; i < length; i++)
			fill += "-";

		return "&m|" + fill + "|";
	}

	/**
	 * If the count is 0 or over 1, adds an "s" to the given string
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String plural(final long count, final String ofWhat) {
		final String exception = getException(count, ofWhat);

		return exception != null ? exception : count + " " + ofWhat + (count == 0 || count > 1 && !ofWhat.endsWith("s") ? "s" : "");
	}

	/**
	 * If the count is 0 or over 1, adds an "es" to the given string
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String pluralEs(final long count, final String ofWhat) {
		final String exception = getException(count, ofWhat);

		return exception != null ? exception : count + " " + ofWhat + (count == 0 || count > 1 && !ofWhat.endsWith("es") ? "es" : "");
	}

	/**
	 * If the count is 0 or over 1, adds an "ies" to the given string
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String pluralIes(final long count, final String ofWhat) {
		final String exception = getException(count, ofWhat);

		return exception != null ? exception : count + " " + (count == 0 || count > 1 && !ofWhat.endsWith("ies") ? ofWhat.substring(0, ofWhat.length() - 1) + "ies" : ofWhat);
	}

	/**
	 * Return the plural word from the exception list or null if none
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 * @deprecated contains a very limited list of most common used English plural irregularities
	 */
	@Deprecated
	private static String getException(final long count, final String ofWhat) {
		final SerializedMap exceptions = SerializedMap.ofArray(
				"life", "lives",
				"class", "classes",
				"wolf", "wolves",
				"knife", "knives",
				"wife", "wives",
				"calf", "calves",
				"leaf", "leaves",
				"potato", "potatoes",
				"tomato", "tomatoes",
				"hero", "heroes",
				"torpedo", "torpedoes",
				"veto", "vetoes",
				"foot", "feet",
				"tooth", "teeth",
				"goose", "geese",
				"man", "men",
				"woman", "women",
				"mouse", "mice",
				"die", "dice",
				"ox", "oxen",
				"child", "children",
				"person", "people",
				"penny", "pence",
				"sheep", "sheep",
				"fish", "fish",
				"deer", "deer",
				"moose", "moose",
				"swine", "swine",
				"buffalo", "buffalo",
				"shrimp", "shrimp",
				"trout", "trout",
				"spacecraft", "spacecraft",
				"cactus", "cacti",
				"axis", "axes",
				"analysis", "analyses",
				"crisis", "crises",
				"thesis", "theses",
				"datum", "data",
				"index", "indices",
				"entry", "entries",
				"boss", "bosses");

		return exceptions.containsKey(ofWhat) ? count + " " + (count == 0 || count > 1 ? exceptions.getString(ofWhat) : ofWhat) : null;
	}

	/**
	 * Prepends the given string with either "a" or "an" (does a dummy syllable check)
	 *
	 * @param ofWhat
	 * @return
	 * @deprecated only a dummy syllable check, e.g. returns a hour
	 */
	@Deprecated
	public static String article(final String ofWhat) {
		Valid.checkBoolean(ofWhat.length() > 0, "String cannot be empty");
		final List<String> syllables = Arrays.asList("a", "e", "i", "o", "u", "y");

		return (syllables.contains(ofWhat.toLowerCase().trim().substring(0, 1)) ? "an" : "a") + " " + ofWhat;
	}

	/**
	 * Generates a bar indicating progress. Example:
	 * <p>
	 * ##-----
	 * ###----
	 * ####---
	 *
	 * @param min            the min progress
	 * @param minChar
	 * @param max            the max prograss
	 * @param maxChar
	 * @param delimiterColor
	 * @return
	 */
	public static String fancyBar(final int min, final char minChar, final int max, final char maxChar, final ChatColor delimiterColor) {
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
	public static String shortLocation(final Vector vec) {
		return " [" + MathUtil.formatOneDigit(vec.getX()) + ", " + MathUtil.formatOneDigit(vec.getY()) + ", " + MathUtil.formatOneDigit(vec.getZ()) + "]";
	}

	/**
	 * Formats the given location to block points without decimals
	 *
	 * @param loc
	 * @return
	 */
	public static String shortLocation(final Location loc) {
		if (loc == null)
			return "Location(null)";

		if (loc.equals(new Location(null, 0, 0, 0)))
			return "Location(null, 0, 0, 0)";

		Valid.checkNotNull(loc.getWorld(), "Cannot shorten a location with null world!");

		return loc.getWorld().getName() + " [" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]";
	}

	/**
	 * A very simple helper for duplicating the given text the given amount of times.
	 *
	 * Example: duplicate("apple", 2) will produce "appleapple"
	 *
	 * @param text
	 * @param nTimes
	 * @return
	 */
	public static String duplicate(String text, int nTimes) {
		if (nTimes == 0)
			return "";

		final String toDuplicate = new String(text);

		for (int i = 1; i < nTimes; i++)
			text += toDuplicate;

		return text;
	}

	/**
	 * Limits the string to the given length maximum
	 * appending "..." at the end when it is cut
	 *
	 * @param text
	 * @param maxLength
	 * @return
	 */
	public static String limit(String text, int maxLength) {
		final int length = text.length();

		return maxLength >= length ? text : text.substring(0, maxLength) + "...";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Plugins management
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @see #doesPluginExist(String)
	 *
	 * @deprecated subject for removal, please use {@link #doesPluginExist(String)} because it now works the same
	 *
	 * @param pluginName
	 * @return
	 */
	@Deprecated
	public static boolean doesPluginExistSilently(final String pluginName) {
		return doesPluginExist(pluginName);
	}

	/**
	 * Checks if a plugin is enabled. We also schedule an async task to make
	 * sure the plugin is loaded correctly when the server is done booting
	 * <p>
	 * Return true if it is loaded (this does not mean it works correctly)
	 *
	 * @param pluginName
	 * @return
	 */
	public static boolean doesPluginExist(final String pluginName) {
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
			runLaterAsync(0, () -> Valid.checkBoolean(found.isEnabled(), SimplePlugin.getNamed() + " could not hook into " + pluginName + " as the plugin is disabled! (DO NOT REPORT THIS TO " + SimplePlugin.getNamed() + ", look for errors above and contact support of '" + pluginName + "')"));

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Running commands
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Runs the given command (without /) as the console, replacing {player} with sender
	 *
	 * You can prefix the command with @(announce|warn|error|info|question|success) to send a formatted
	 * message to playerReplacement directly.
	 *
	 * @param playerReplacement
	 * @param command
	 */
	public static void dispatchCommand(final CommandSender playerReplacement, @NonNull String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		if (command.startsWith("@announce "))
			Messenger.announce(playerReplacement, command.replace("@announce ", ""));

		else if (command.startsWith("@warn "))
			Messenger.warn(playerReplacement, command.replace("@warn ", ""));

		else if (command.startsWith("@error "))
			Messenger.error(playerReplacement, command.replace("@error ", ""));

		else if (command.startsWith("@info "))
			Messenger.info(playerReplacement, command.replace("@info ", ""));

		else if (command.startsWith("@question "))
			Messenger.question(playerReplacement, command.replace("@question ", ""));

		else if (command.startsWith("@success "))
			Messenger.success(playerReplacement, command.replace("@success ", ""));

		else {
			command = command.startsWith("/") ? command.substring(1) : command;
			command = command.replace("{player}", playerReplacement == null ? "" : resolveSenderName(playerReplacement));

			// Workaround for JSON in tellraw getting HEX colors replaced
			if (!command.startsWith("tellraw"))
				command = colorize(command);

			final String finalCommand = command;

			runLater(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
		}
	}

	/**
	 * Runs the given command (without /) as if the sender would type it, replacing {player} with his name
	 *
	 * @param playerSender
	 * @param command
	 */
	public static void dispatchCommandAsPlayer(@NonNull final Player playerSender, @NonNull String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		runLater(() -> playerSender.performCommand(colorize(command.replace("{player}", resolveSenderName(playerSender)))));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Logging and error handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Logs the message, and saves the time it was logged. If you call this method
	 * to log exactly the same message within the delay in seconds, it will not be logged.
	 * <p>
	 * Saves console spam.
	 *
	 * @param delaySec
	 * @param msg
	 */
	public static void logTimed(final int delaySec, final String msg) {
		if (!TIMED_LOG_CACHE.containsKey(msg)) {
			log(msg);
			TIMED_LOG_CACHE.put(msg, TimeUtil.currentTimeSeconds());
			return;
		}

		if (TimeUtil.currentTimeSeconds() - TIMED_LOG_CACHE.get(msg) > delaySec) {
			log(msg);
			TIMED_LOG_CACHE.put(msg, TimeUtil.currentTimeSeconds());
		}
	}

	/**
	 * Works similarly to {@link String#format(String, Object...)} however
	 * all arguments are explored, so player names are properly given, location is shortened etc.
	 *
	 * @param format
	 * @param args
	 */
	public static void logF(final String format, @NonNull final Object... args) {
		final String formatted = format(format, args);

		log(false, formatted);
	}

	/**
	 * Replace boring CraftPlayer{name=noob} into a proper player name,
	 * works fine with entities, worlds, and locations
	 * <p>
	 * Example use: format("Hello %s from world %s", player, player.getWorld())
	 *
	 * @param format
	 * @param args
	 * @return
	 */
	public static String format(final String format, @NonNull final Object... args) {
		for (int i = 0; i < args.length; i++) {
			final Object arg = args[i];

			if (arg != null)
				args[i] = simplify(arg);
		}

		return String.format(format, args);
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param messages
	 */
	public static void log(final List<String> messages) {
		log(toArray(messages));
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param messages
	 */
	public static void log(final String... messages) {
		log(true, messages);
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 * <p>
	 * Does not add {@link #getLogPrefix()}
	 *
	 * @param messages
	 */
	public static void logNoPrefix(final String... messages) {
		log(false, messages);
	}

	/*
	 * Logs a bunch of messages to the console, & colors are supported
	 */
	private static void log(final boolean addLogPrefix, final String... messages) {
		if (messages == null)
			return;

		for (String message : messages) {
			if (message.equals("none"))
				continue;

			if (stripColors(message).replace(" ", "").isEmpty()) {
				if (CONSOLE_SENDER == null)
					System.out.println(" ");
				else
					CONSOLE_SENDER.sendMessage("  ");

				continue;
			}

			message = colorize(message);

			if (message.startsWith("[JSON]")) {
				final String stripped = message.replaceFirst("\\[JSON\\]", "").trim();

				if (!stripped.isEmpty())
					log(Remain.toLegacyText(stripped, false));

			} else
				for (final String part : splitNewline(message)) {
					final String log = ((addLogPrefix && ADD_LOG_PREFIX ? removeSurroundingSpaces(logPrefix) + " " : "") + getOrEmpty(part).replace("\n", colorize("\n&r"))).trim();

					if (CONSOLE_SENDER != null)
						CONSOLE_SENDER.sendMessage(log);
					else
						System.out.println("[" + SimplePlugin.getNamed() + "] " + stripColors(log));
				}
		}
	}

	/**
	 * Logs a bunch of messages to the console in a {@link #consoleLine()} frame.
	 *
	 * @param messages
	 */
	public static void logFramed(final String... messages) {
		logFramed(false, messages);
	}

	/**
	 * Logs a bunch of messages to the console in a {@link #consoleLine()} frame.
	 * <p>
	 * Used when an error occurs, can also disable the plugin
	 *
	 * @param disablePlugin
	 * @param messages
	 */
	public static void logFramed(final boolean disablePlugin, final String... messages) {
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
	public static void error(final Throwable t, final String... messages) {
		if (!(t instanceof FoException))
			Debugger.saveError(t, messages);

		Debugger.printStackTrace(t);
		logFramed(replaceErrorVariable(t, messages));
	}

	/**
	 * Logs the messages in frame (if not null),
	 * saves the error to errors.log and then throws it
	 * <p>
	 * Possible to use %error variable
	 *
	 * @param t
	 * @param messages
	 */
	public static void throwError(Throwable t, final String... messages) {

		// Get to the root cause of this problem
		while (t.getCause() != null)
			t = t.getCause();

		// Delegate to only print out the relevant stuff
		if (t instanceof FoException)
			throw (FoException) t;

		if (messages != null)
			logFramed(false, replaceErrorVariable(t, messages));

		Debugger.saveError(t, messages);
		Remain.sneaky(t);
	}

	/*
	 * Replace the %error variable with a smart error info, see above
	 */
	private static String[] replaceErrorVariable(Throwable throwable, final String... msgs) {
		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		final String throwableName = throwable == null ? "Unknown error." : throwable.getClass().getSimpleName();
		final String throwableMessage = throwable == null || throwable.getMessage() == null || throwable.getMessage().isEmpty() ? "" : ": " + throwable.getMessage();

		for (int i = 0; i < msgs.length; i++) {
			final String error = throwableName + throwableMessage;

			msgs[i] = msgs[i]
					.replace("%error%", error)
					.replace("%error", error);
		}

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
	public static boolean regExMatch(final String regex, final String message) {
		return regExMatch(compilePattern(regex), message);
	}

	/**
	 * Returns true if the given pattern matches the given message
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static boolean regExMatch(final Pattern regex, final String message) {
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
	public static boolean regExMatch(final Matcher matcher) {
		Valid.checkNotNull(matcher, "Cannot call regExMatch on null matcher");

		try {
			return matcher.find();

		} catch (final RegexTimeoutException ex) {
			handleRegexTimeoutException(ex, matcher.pattern());

			return false;
		}
	}

	/**
	 * Compiles a matches for the given pattern and message. Colors are stripped.
	 * <p>
	 * We also evaluate how long the evaluation took and stop it in case it takes too long,
	 * see {@link SimplePlugin#getRegexTimeout()}
	 *
	 * @param pattern
	 * @param message
	 * @return
	 */
	public static Matcher compileMatcher(@NonNull final Pattern pattern, final String message) {

		try {
			String strippedMessage = SimplePlugin.getInstance().regexStripColors() ? stripColors(message) : message;
			strippedMessage = SimplePlugin.getInstance().regexStripAccents() ? ChatUtil.replaceDiacritic(strippedMessage) : strippedMessage;

			return pattern.matcher(TimedCharSequence.withSettingsLimit(strippedMessage));

		} catch (final RegexTimeoutException ex) {
			handleRegexTimeoutException(ex, pattern);

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
	public static Matcher compileMatcher(final String regex, final String message) {
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
		final SimplePlugin instance = SimplePlugin.getInstance();
		Pattern pattern = null;

		regex = SimplePlugin.getInstance().regexStripColors() ? stripColors(regex) : regex;
		regex = SimplePlugin.getInstance().regexStripAccents() ? ChatUtil.replaceDiacritic(regex) : regex;

		try {

			if (instance.regexCaseInsensitive())
				pattern = Pattern.compile(regex, instance.regexUnicode() ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : Pattern.CASE_INSENSITIVE);

			else
				pattern = instance.regexUnicode() ? Pattern.compile(regex, Pattern.UNICODE_CASE) : Pattern.compile(regex);

		} catch (final PatternSyntaxException ex) {
			throwError(ex,
					"Your regular expression is malformed!",
					"Expression: '" + regex + "'",
					"",
					"IF YOU CREATED IT YOURSELF, we unfortunately",
					"can't provide support for custom expressions.",
					"Use online services like regex101.com to put your",
					"expression there (without '') and discover where",
					"the syntax error lays and how to fix it.");

			return null;
		}

		return pattern;
	}

	/**
	 * A special call handling regex timeout exception, do not use
	 *
	 * @param ex
	 * @param pattern
	 */
	public static void handleRegexTimeoutException(RegexTimeoutException ex, Pattern pattern) {
		final boolean caseInsensitive = SimplePlugin.getInstance().regexCaseInsensitive();

		Common.error(ex,
				"A regular expression took too long to process, and was",
				"stopped to prevent freezing your server.",
				" ",
				"Limit " + SimpleSettings.REGEX_TIMEOUT + "ms ",
				"Expression: '" + (pattern == null ? "unknown" : pattern.pattern()) + "'",
				"Evaluated message: '" + ex.getCheckedMessage() + "'",
				" ",
				"IF YOU CREATED THAT RULE YOURSELF, we unfortunately",
				"can't provide support for custom expressions.",
				" ",
				"Sometimes, all you need doing is increasing timeout",
				"limit in your settings.yml",
				" ",
				"Use services like regex101.com to test and fix it.",
				"Put the expression without '' and the message there.",
				"Ensure to turn flags 'insensitive' and 'unicode' " + (caseInsensitive ? "on" : "off"),
				"on there when testing: https://i.imgur.com/PRR5Rfn.png");
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
	@SafeVarargs
	public static <T> List<T> joinArrays(final Iterable<T>... arrays) {
		final List<T> all = new ArrayList<>();

		for (final Iterable<T> array : arrays)
			for (final T element : array)
				all.add(element);

		return all;
	}

	/**
	 * A convenience method for converting array of command senders into array of their names
	 * except the given player
	 *
	 * @param <T>
	 * @param array
	 * @param nameToIgnore
	 * @return
	 */
	public static <T extends CommandSender> String joinPlayersExcept(final Iterable<T> array, final String nameToIgnore) {
		final Iterator<T> it = array.iterator();
		String message = "";

		while (it.hasNext()) {
			final T next = it.next();

			if (!next.getName().equals(nameToIgnore))
				message += next.getName() + (it.hasNext() ? ", " : "");
		}

		return message.endsWith(", ") ? message.substring(0, message.length() - 2) : message;
	}

	/**
	 * Joins an array together using spaces from the given start index
	 *
	 * @param startIndex
	 * @param array
	 * @return
	 */
	public static String joinRange(final int startIndex, final String[] array) {
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
	public static String joinRange(final int startIndex, final int stopIndex, final String[] array) {
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
	public static String joinRange(final int start, final int stop, final String[] array, final String delimiter) {
		String joined = "";

		for (int i = start; i < MathUtil.range(stop, 0, array.length); i++)
			joined += (joined.isEmpty() ? "" : delimiter) + array[i];

		return joined;
	}

	/**
	 * A convenience method for converting array of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String join(final T[] array) {
		return array == null ? "null" : join(Arrays.asList(array));
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String join(final Iterable<T> array) {
		return array == null ? "null" : join(array, ", ");
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
	public static <T> String join(final Iterable<T> array, final String delimiter) {
		return join(array, delimiter, object -> object == null ? "" : simplify(object));
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
	public static <T> String join(final T[] array, final String delimiter, final Stringer<T> stringer) {
		Valid.checkNotNull(array, "Cannot join null array!");

		return join(Arrays.asList(array), delimiter, stringer);
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
	public static <T> String join(final Iterable<T> array, final String delimiter, final Stringer<T> stringer) {
		final Iterator<T> it = array.iterator();
		String message = "";

		while (it.hasNext()) {
			final T next = it.next();

			if (next != null)
				message += stringer.toString(next) + (it.hasNext() ? delimiter : "");
		}

		return message;
	}

	/**
	 * Replace some common classes such as entity to name automatically
	 *
	 * @param arg
	 * @return
	 */
	public static String simplify(Object arg) {
		if (arg instanceof Entity)
			return Remain.getName((Entity) arg);

		else if (arg instanceof CommandSender)
			return ((CommandSender) arg).getName();

		else if (arg instanceof World)
			return ((World) arg).getName();

		else if (arg instanceof Location)
			return Common.shortLocation((Location) arg);

		else if (arg.getClass() == double.class || arg.getClass() == float.class)
			return MathUtil.formatTwoDigits((double) arg);

		else if (arg instanceof Collection)
			return Common.join((Collection<?>) arg, ", ", Common::simplify);

		else if (arg instanceof ChatColor)
			return ((Enum<?>) arg).name().toLowerCase();

		else if (arg instanceof CompChatColor)
			return ((CompChatColor) arg).getName();

		else if (arg instanceof Enum)
			return ((Enum<?>) arg).toString().toLowerCase();

		try {
			if (arg instanceof net.md_5.bungee.api.ChatColor)
				return ((net.md_5.bungee.api.ChatColor) arg).getName();
		} catch (final Exception e) {
			// No MC compatible
		}

		return arg.toString();
	}

	/**
	 * Dynamically populates pages, used for pagination in commands or menus
	 *
	 * @param allItems all items that will be split
	 * @return the map containing pages and their items
	 */
	public static <T> Map<Integer, List<T>> fillPages(int cellSize, Iterable<T> items) {
		final List<T> allItems = Common.toList(items);

		final Map<Integer, List<T>> pages = new HashMap<>();
		final int pageCount = allItems.size() == cellSize ? 0 : allItems.size() / cellSize;

		for (int i = 0; i <= pageCount; i++) {
			final List<T> pageItems = new ArrayList<>();

			final int down = cellSize * i;
			final int up = down + cellSize;

			for (int valueIndex = down; valueIndex < up; valueIndex++)
				if (valueIndex < allItems.size()) {
					final T page = allItems.get(valueIndex);

					pageItems.add(page);
				} else
					break;

			pages.put(i, pageItems);
		}

		return pages;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting and retyping
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the last key in the list or null if list is null or empty
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static <T> T last(List<T> list) {
		return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
	}

	/**
	 * Return the last key in the array or null if array is null or empty
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> T last(T[] array) {
		return array == null || array.length == 0 ? null : array[array.length - 1];
	}

	/**
	 * Convenience method for getting a list of world names
	 *
	 * @return
	 */
	public static List<String> getWorldNames() {
		return convert(Bukkit.getWorlds(), World::getName);
	}

	/**
	 * Convenience method for getting a list of player names
	 *
	 * @return
	 */
	public static List<String> getPlayerNames() {
		return getPlayerNames(true, null);
	}

	/**
	 * Convenience method for getting a list of player names
	 * that optionally, are vanished
	 *
	 * @param includeVanished
	 * @return
	 */
	public static List<String> getPlayerNames(final boolean includeVanished) {
		return getPlayerNames(includeVanished, null);
	}

	/**
	 * Convenience method for getting a list of player names
	 * that optionally, the other player can see
	 *
	 * @param includeVanished
	 * @param otherPlayer
	 *
	 * @return
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
	 * Return nicknames of online players
	 *
	 * @param includeVanished
	 * @return
	 */
	public static List<String> getPlayerNicknames(final boolean includeVanished) {
		return getPlayerNicknames(includeVanished, null);
	}

	/**
	 * Return nicknames of online players
	 *
	 * @param includeVanished
	 * @param otherPlayer
	 * @return
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
	 * Converts a list having one type object into another
	 *
	 * @param list      the old list
	 * @param converter the converter;
	 * @return the new list
	 */
	public static <OLD, NEW> List<NEW> convert(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
		final List<NEW> copy = new ArrayList<>();

		for (final OLD old : list) {
			final NEW result = converter.convert(old);
			if (result != null)
				copy.add(converter.convert(old));
		}

		return copy;
	}

	/**
	 * Converts a set having one type object into another
	 *
	 * @param list      the old list
	 * @param converter the converter;
	 * @return the new list
	 */
	public static <OLD, NEW> Set<NEW> convertSet(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
		final Set<NEW> copy = new HashSet<>();

		for (final OLD old : list) {
			final NEW result = converter.convert(old);
			if (result != null)
				copy.add(converter.convert(old));
		}

		return copy;
	}

	/**
	 * Converts a list having one type object into another
	 *
	 * @param list      the old list
	 * @param converter the converter
	 * @return the new list
	 */
	public static <OLD, NEW> StrictList<NEW> convertStrict(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
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
	public static <OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> Map<NEW_KEY, NEW_VALUE> convert(final Map<OLD_KEY, OLD_VALUE> oldMap, final MapToMapConverter<OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> converter) {
		final Map<NEW_KEY, NEW_VALUE> newMap = new HashMap<>();
		oldMap.entrySet().forEach(e -> newMap.put(converter.convertKey(e.getKey()), converter.convertValue(e.getValue())));

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
	public static <OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> StrictMap<NEW_KEY, NEW_VALUE> convertStrict(final Map<OLD_KEY, OLD_VALUE> oldMap, final MapToMapConverter<OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> converter) {
		final StrictMap<NEW_KEY, NEW_VALUE> newMap = new StrictMap<>();
		oldMap.entrySet().forEach(e -> newMap.put(converter.convertKey(e.getKey()), converter.convertValue(e.getValue())));

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
	public static <LIST_KEY, OLD_KEY, OLD_VALUE> StrictList<LIST_KEY> convertToList(final Map<OLD_KEY, OLD_VALUE> map, final MapToListConverter<LIST_KEY, OLD_KEY, OLD_VALUE> converter) {
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
	public static <OLD_TYPE, NEW_TYPE> List<NEW_TYPE> convert(final OLD_TYPE[] oldArray, final TypeConverter<OLD_TYPE, NEW_TYPE> converter) {
		final List<NEW_TYPE> newList = new ArrayList<>();

		for (final OLD_TYPE old : oldArray)
			newList.add(converter.convert(old));

		return newList;
	}

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
	public static String[] splitNewline(final String message) {
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
	 * Split the given string into array of the given max line length
	 *
	 * @param input
	 * @param maxLineLength
	 * @return
	 */
	public static String[] split(String input, int maxLineLength) {
		final StringTokenizer tok = new StringTokenizer(input, " ");
		final StringBuilder output = new StringBuilder(input.length());

		int lineLen = 0;

		while (tok.hasMoreTokens()) {
			final String word = tok.nextToken();

			if (lineLen + word.length() > maxLineLength) {
				output.append("\n");

				lineLen = 0;
			}

			output.append(word);
			lineLen += word.length();
		}

		return output.toString().split("\n");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc message handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new list only containing non-null and not empty string elements
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> List<T> removeNullAndEmpty(final T[] array) {
		return array != null ? removeNullAndEmpty(Arrays.asList(array)) : new ArrayList<>();
	}

	/**
	 * Creates a new list only containing non-null and not empty string elements
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static <T> List<T> removeNullAndEmpty(final List<T> list) {
		final List<T> copy = new ArrayList<>();

		for (final T key : list)
			if (key != null)
				if (key instanceof String) {
					if (!((String) key).isEmpty())
						copy.add(key);
				} else
					copy.add(key);

		return copy;
	}

	/**
	 * REplaces all nulls with an empty string
	 *
	 * @param list
	 * @return
	 */
	public static String[] replaceNullWithEmpty(final String[] list) {
		for (int i = 0; i < list.length; i++)
			if (list[i] == null)
				list[i] = "";

		return list;
	}

	/**
	 * Return a value at the given index or the default if the index does not exist in array
	 *
	 * @param <T>
	 * @param array
	 * @param index
	 * @param def
	 * @return
	 */
	public static <T> T getOrDefault(final T[] array, final int index, final T def) {
		return index < array.length ? array[index] : def;
	}

	/**
	 * Return an empty String if the String is null or equals to none.
	 *
	 * @param input
	 * @return
	 */
	public static String getOrEmpty(final String input) {
		return input == null || "none".equalsIgnoreCase(input) ? "" : input;
	}

	/**
	 * If the String equals to none or is empty, return null
	 *
	 * @param input
	 * @return
	 */
	public static String getOrNull(final String input) {
		return input == null || "none".equalsIgnoreCase(input) || input.isEmpty() ? null : input;
	}

	/**
	 * Returns the value or its default counterpart in case it is null
	 *
	 * @param value
	 * @param def
	 *
	 * @deprecated subject for removal, use {@link #getOrDefault(Object, Object)}
	 * 			   as it works exactly the same now
	 * @return
	 */
	@Deprecated
	public static String getOrSupply(String value, String def) {
		return getOrDefault(value, def);
	}

	/**
	 * Returns the value or its default counterpart in case it is null
	 *
	 * PSA: If values are strings, we return default if the value is empty or equals to "none"
	 *
	 * @param value the primary value
	 * @param def   the default value
	 * @return the value, or default it the value is null
	 */
	public static <T> T getOrDefault(final T value, final T def) {
		if (value instanceof String && ("none".equalsIgnoreCase((String) value) || "".equals(value)))
			return def;

		return getOrDefaultStrict(value, def);
	}

	/**
	 * Returns the value or its default counterpart in case it is null
	 *
	 * @param <T>
	 * @param value
	 * @param def
	 * @return
	 */
	public static <T> T getOrDefaultStrict(final T value, final T def) {
		return value != null ? value : def;
	}

	/**
	 * Get next element in the list increasing the index by 1 if forward is true,
	 * or decreasing it by 1 if it is false
	 *
	 * @param <T>
	 * @param given
	 * @param list
	 * @param forward
	 * @return
	 */
	public static <T> T getNext(final T given, final List<T> list, final boolean forward) {
		if (given == null && list.isEmpty())
			return null;

		final T[] array = (T[]) Array.newInstance((given != null ? given : list.get(0)).getClass(), list.size());

		for (int i = 0; i < list.size(); i++)
			Array.set(array, i, list.get(i));

		return getNext(given, array, forward);
	}

	/**
	 * Get next element in the list increasing the index by 1 if forward is true,
	 * or decreasing it by 1 if it is false
	 *
	 * @param <T>
	 * @param given
	 * @param array
	 * @param forward
	 * @return
	 */
	public static <T> T getNext(final T given, final T[] array, final boolean forward) {
		if (array.length == 0)
			return null;

		int index = 0;

		for (int i = 0; i < array.length; i++) {
			final T element = array[i];

			if (element.equals(given)) {
				index = i;

				break;
			}
		}

		if (index != -1) {
			final int nextIndex = index + (forward ? 1 : -1);

			// Return the first slot if reached the end, or the last if vice versa
			return nextIndex >= array.length ? array[0] : nextIndex < 0 ? array[array.length - 1] : array[nextIndex];
		}

		return null;
	}

	/**
	 * Converts a list of string into a string array
	 *
	 * @param array
	 * @return
	 */
	public static String[] toArray(final Collection<String> array) {
		return array == null ? new String[0] : array.toArray(new String[array.size()]);
	}

	/**
	 * Creates a new modifiable array list from array
	 *
	 * @param array
	 * @return
	 */
	public static <T> ArrayList<T> toList(final T... array) {
		return array == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(array));
	}

	/**
	 * Converts {@link Iterable} to {@link List}
	 *
	 * @param it the iterable
	 * @return the new list
	 */
	public static <T> List<T> toList(final Iterable<T> it) {
		final List<T> list = new ArrayList<>();

		if (it != null)
			it.forEach(el -> {
				if (el != null)
					list.add(el);
			});

		return list;
	}

	/**
	 * Reverses elements in the array
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> T[] reverse(final T[] array) {
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
	 * Return a new hashmap having the given first key and value pair
	 *
	 * @param <A>
	 * @param <B>
	 * @param firstKey
	 * @param firstValue
	 * @return
	 */
	public static <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);

		return map;
	}

	/**
	 * Create a new hashset
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static <T> Set<T> newSet(final T... keys) {
		return new HashSet<>(Arrays.asList(keys));
	}

	/**
	 * Create a new array list that is mutable
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static <T> List<T> newList(final T... keys) {
		final List<T> list = new ArrayList<>();

		for (final T key : keys)
			list.add(key);

		return list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Scheduling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Runs the task if the plugin is enabled correctly
	 *
	 * @param task the task
	 * @return the task or null
	 */
	public static <T extends Runnable> BukkitTask runLater(final T task) {
		return runLater(1, task);
	}

	/**
	 * Runs the task even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param task
	 * @return the task or null
	 */
	public static BukkitTask runLater(final int delayTicks, Runnable task) {
		final BukkitScheduler scheduler = Bukkit.getScheduler();
		final JavaPlugin instance = SimplePlugin.getInstance();

		try {
			return runIfDisabled(task) ? null : delayTicks == 0 ? task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTask(instance) : scheduler.runTask(instance, task) : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskLater(instance, delayTicks) : scheduler.runTaskLater(instance, task, delayTicks);
		} catch (final NoSuchMethodError err) {

			return runIfDisabled(task) ? null
					: delayTicks == 0
							? task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTask(instance) : getTaskFromId(scheduler.scheduleSyncDelayedTask(instance, task))
							: task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskLater(instance, delayTicks) : getTaskFromId(scheduler.scheduleSyncDelayedTask(instance, task, delayTicks));
		}
	}

	/**
	 * Runs the task async even if the plugin is disabled for some reason.
	 * <p>
	 * Schedules the run on the next tick.
	 *
	 * @param task
	 * @return
	 */
	public static BukkitTask runAsync(final Runnable task) {
		return runLaterAsync(0, task);
	}

	/**
	 * Runs the task async even if the plugin is disabled for some reason.
	 * <p>
	 * Schedules the run on the next tick.
	 *
	 * @param task
	 * @return
	 */
	public static BukkitTask runLaterAsync(final Runnable task) {
		return runLaterAsync(0, task);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Bukkit scheduling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Runs the task async even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param task
	 * @return the task or null
	 */
	public static BukkitTask runLaterAsync(final int delayTicks, Runnable task) {
		final BukkitScheduler scheduler = Bukkit.getScheduler();
		final JavaPlugin instance = SimplePlugin.getInstance();

		try {
			return runIfDisabled(task) ? null : delayTicks == 0 ? task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskAsynchronously(instance) : scheduler.runTaskAsynchronously(instance, task) : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskLaterAsynchronously(instance, delayTicks) : scheduler.runTaskLaterAsynchronously(instance, task, delayTicks);

		} catch (final NoSuchMethodError err) {
			return runIfDisabled(task) ? null
					: delayTicks == 0
							? getTaskFromId(scheduler.scheduleAsyncDelayedTask(instance, task))
							: getTaskFromId(scheduler.scheduleAsyncDelayedTask(instance, task, delayTicks));
		}
	}

	/**
	 * Runs the task timer even if the plugin is disabled.
	 *
	 * @param repeatTicks the delay between each execution
	 * @param task        the task
	 * @return the bukkit task or null
	 */
	public static BukkitTask runTimer(final int repeatTicks, final Runnable task) {
		return runTimer(0, repeatTicks, task);
	}

	/**
	 * Runs the task timer even if the plugin is disabled.
	 *
	 * @param delayTicks  the delay before first run
	 * @param repeatTicks the delay between each run
	 * @param task        the task
	 * @return the bukkit task or null if error
	 */
	public static BukkitTask runTimer(final int delayTicks, final int repeatTicks, Runnable task) {

		try {
			return runIfDisabled(task) ? null : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskTimer(SimplePlugin.getInstance(), delayTicks, repeatTicks) : Bukkit.getScheduler().runTaskTimer(SimplePlugin.getInstance(), task, delayTicks, repeatTicks);

		} catch (final NoSuchMethodError err) {
			return runIfDisabled(task) ? null
					: getTaskFromId(Bukkit.getScheduler().scheduleSyncRepeatingTask(SimplePlugin.getInstance(), task, delayTicks, repeatTicks));
		}
	}

	/**
	 * Runs the task timer async even if the plugin is disabled.
	 *
	 * @param repeatTicks
	 * @param task
	 * @return
	 */
	public static BukkitTask runTimerAsync(final int repeatTicks, final Runnable task) {
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
	public static BukkitTask runTimerAsync(final int delayTicks, final int repeatTicks, Runnable task) {

		try {
			return runIfDisabled(task) ? null : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskTimerAsynchronously(SimplePlugin.getInstance(), delayTicks, repeatTicks) : Bukkit.getScheduler().runTaskTimerAsynchronously(SimplePlugin.getInstance(), task, delayTicks, repeatTicks);

		} catch (final NoSuchMethodError err) {
			return runIfDisabled(task) ? null
					: getTaskFromId(Bukkit.getScheduler().scheduleAsyncRepeatingTask(SimplePlugin.getInstance(), task, delayTicks, repeatTicks));
		}
	}

	/*
	 * A compatibility method that converts the given task id into a bukkit task
	 */
	private static BukkitTask getTaskFromId(int taskId) {

		for (final BukkitTask task : Bukkit.getScheduler().getPendingTasks())
			if (task.getTaskId() == taskId)
				return task;

		// TODO Fix for MC 1.2.5
		return null;
	}

	// Check our plugin instance if it's enabled
	// In case it is disabled, just runs the task and returns true
	// Otherwise we return false and the task will be run correctly in Bukkit scheduler
	// This is fail-safe to critical save-on-exit operations in case our plugin is improperly reloaded (PlugMan) or malfunctions
	private static boolean runIfDisabled(final Runnable run) {
		if (!SimplePlugin.getInstance().isEnabled()) {
			run.run();

			return true;
		}

		return false;
	}

	/**
	 * Call an event in Bukkit and return whether it was fired
	 * successfully through the pipeline (NOT cancelled)
	 *
	 * @param event the event
	 * @return true if the event was NOT cancelled
	 */
	public static boolean callEvent(final Event event) {
		Bukkit.getPluginManager().callEvent(event);

		return event instanceof Cancellable ? !((Cancellable) event).isCancelled() : true;
	}

	/**
	 * Convenience method for registering events as our instance
	 *
	 * @param listener
	 */
	public static void registerEvents(final Listener listener) {
		Bukkit.getPluginManager().registerEvents(listener, SimplePlugin.getInstance());
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Resolves the inner Map in a Bukkit's {@link MemorySection}
	 *
	 * @param mapOrSection
	 * @return
	 */
	public static Map<String, Object> getMapFromSection(@NonNull final Object mapOrSection) {
		final Map<String, Object> map = mapOrSection instanceof Map ? (Map<String, Object>) mapOrSection : mapOrSection instanceof MemorySection ? ReflectionUtil.getFieldContent(mapOrSection, "map") : null;
		Valid.checkNotNull(map, "Unexpected " + mapOrSection.getClass().getSimpleName() + " '" + mapOrSection + "'. Must be Map or MemorySection! (Do not just send config name here, but the actual section with get('section'))");

		return map;
	}

	/**
	 * Returns true if the domain is reachable. Method is blocking.
	 *
	 * @param url
	 * @param timeout
	 * @return
	 */
	public static boolean isDomainReachable(String url, final int timeout) {
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

	/**
	 * Checked sleep method from {@link Thread#sleep(long)} but without the try-catch need
	 *
	 * @param millis
	 */
	public static void sleep(final int millis) {
		try {
			Thread.sleep(millis);

		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

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
	@SuppressWarnings("hiding")
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
	private final long futureTimestampLimit;

	/*
	 * Create a new timed message for the given message with a timeout in millis
	 */
	private TimedCharSequence(@NonNull final CharSequence message, long futureTimestampLimit) {
		this.message = message;
		this.futureTimestampLimit = futureTimestampLimit;
	}

	/**
	 * Gets a character at the given index, or throws an error if
	 * this is called too late after the constructor, see {@link #futureTimestampLimit}
	 */
	@Override
	public char charAt(final int index) {

		// Temporarily disabled due to a rare condition upstream when we take this message
		// and run it in a runnable, then this is still being evaluated past limit and it fails
		//
		//if (System.currentTimeMillis() > futureTimestampLimit)
		//	throw new RegexTimeoutException(message, futureTimestampLimit);

		return message.charAt(index);
	}

	@Override
	public int length() {
		return message.length();
	}

	@Override
	public CharSequence subSequence(final int start, final int end) {
		return new TimedCharSequence(message.subSequence(start, end), futureTimestampLimit);
	}

	@Override
	public String toString() {
		return message.toString();
	}

	/**
	 * Compile a new char sequence with limit from settings.yml
	 *
	 * @param message
	 * @return
	 */
	static TimedCharSequence withSettingsLimit(CharSequence message) {
		return new TimedCharSequence(message, System.currentTimeMillis() + SimpleSettings.REGEX_TIMEOUT);
	}
}