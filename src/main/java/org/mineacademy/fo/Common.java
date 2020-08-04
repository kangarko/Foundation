package org.mineacademy.fo;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.MemorySection;
import org.bukkit.conversations.Conversable;
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
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictArrayList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.RegexTimeoutException;
import org.mineacademy.fo.model.DiscordSender;
import org.mineacademy.fo.model.LocalCommandSender;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
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

	/**
	 * Holds words that differ in their plural form (nominative case only)
	 */
	private static final SerializedMap PLURAL_EXCEPTIONS = SerializedMap.ofArray("life", "lives", "wolf", "wolves");

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
	 * If {@link #ADD_TELL_PREFIX} is true, shall we automatically add prefix even in conversations?
	 */
	public static boolean ADD_TELL_PREFIX_IN_CONVERSATION = false;

	/**
	 * Should we add a prefix to the messages we send to the console?
	 * <p>
	 * True by default
	 */
	public static boolean ADD_LOG_PREFIX = true;

	/**
	 * Should we send tellX methods even if the player is conversing?
	 * Defaults to false
	 */
	public static boolean SEND_TELL_TO_CONVERSING = false;

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
		Common.tellPrefix = Common.colorize(prefix);
	}

	/**
	 * Set the log prefix applied for messages in the console from log() methods.
	 * <p>
	 * Colors with & letter are translated automatically.
	 *
	 * @param prefix
	 */
	public static void setLogPrefix(final String prefix) {
		Common.logPrefix = Common.colorize(prefix);
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
	public static void broadcastWithPlayer(final String message, final CommandSender sender) {
		Common.broadcastWithPlayer(message, Common.resolveSenderName(sender));
	}

	/**
	 * Broadcast the message replacing {player} variable with the given player replacement
	 *
	 * @param message
	 * @param playerReplacement
	 */
	public static void broadcastWithPlayer(final String message, final String playerReplacement) {
		Common.broadcast(message.replace("{player}", playerReplacement));
	}

	/**
	 * Broadcast the message to everyone and logs it
	 *
	 * @param message
	 */
	public static void broadcast(final String message) {
		Common.broadcast(message, true);
	}

	/**
	 * Broadcast the message and also may log it into the console
	 *
	 * @param message
	 * @param log
	 */
	public static void broadcast(final String message, final boolean log) {
		if (message != null && !message.equals("none")) {
			for (final Player online : Remain.getOnlinePlayers())
				Common.tellJson(online, message);

			if (log)
				Common.log(message);
		}
	}

	/**
	 * Broadcast the message to everyone and logs it
	 *
	 * @param messages
	 */
	public static void broadcast(final Collection<String> messages) {
		for (final String message : messages)
			Common.broadcast(message, true);
	}

	/**
	 * Broadcast the messages and also may log it into the console
	 *
	 * @param messages
	 * @param log
	 */
	public static void broadcast(final Collection<String> messages, final boolean log) {
		for (final String message : messages)
			Common.broadcast(message, log);
	}

	/**
	 * Broadcast the message to everyone with permission
	 *
	 * @param permission
	 * @param message
	 * @param log
	 */
	public static void broadcastWithPerm(final String permission, final String message, final boolean log) {
		if (message != null && !message.equals("none")) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, permission))
					Common.tellJson(online, message);

			if (log)
				Common.log(message);
		}
	}

	/**
	 * Broadcast the text component message to everyone with permission
	 *
	 * @param permission
	 * @param message
	 */
	public static void broadcastWithPerm(final String permission, @NonNull final TextComponent message) {
		final String legacy = message.toLegacyText();

		if (!legacy.equals("none")) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, permission))
					Remain.sendComponent(online, message);

			Common.log(legacy);
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
			Common.tell(sender, messages);
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
		final boolean hadPrefix = Common.ADD_TELL_PREFIX;
		Common.ADD_TELL_PREFIX = false;

		Common.tellTimed(delaySeconds, sender, message);

		Common.ADD_TELL_PREFIX = hadPrefix;
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
		if (!Common.TIMED_TELL_CACHE.containsKey(message)) {
			Common.tell(sender, message);

			Common.TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
			return;
		}

		if (TimeUtil.currentTimeSeconds() - Common.TIMED_TELL_CACHE.get(message) > delaySeconds) {
			Common.tell(sender, message);

			Common.TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
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
		Common.runLater(delayTicks, () -> Common.tellConversing(conversable, message));
	}

	/**
	 * Sends the conversable player a colorized message
	 *
	 * @param conversable
	 * @param message
	 */
	public static void tellConversing(final Conversable conversable, final String message) {
		conversable.sendRawMessage(Common.colorize((Common.ADD_TELL_PREFIX && Common.ADD_TELL_PREFIX_IN_CONVERSATION ? Common.tellPrefix : "") + Common.removeFirstSpaces(message)).trim());
	}

	/**
	 * Sends a message to the sender with a given delay, colors & are supported
	 *
	 * @param sender
	 * @param delayTicks
	 * @param messages
	 */
	public static void tellLater(final int delayTicks, final CommandSender sender, final String... messages) {
		Common.runLater(delayTicks, () -> Common.tell(sender, messages));
	}

	/**
	 * Sends sender a message with {} variables replaced and colors supported
	 * without the {@link #getTellPrefix()}
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tellNoPrefix(final CommandSender sender, final Replacer replacer) {
		Common.tellNoPrefix(sender, replacer.getReplacedMessage());
	}

	/**
	 * Sends the sender a bunch of messages, colors & are supported
	 * without {@link #getTellPrefix()} prefix
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tellNoPrefix(final CommandSender sender, final String... messages) {
		final boolean was = Common.ADD_TELL_PREFIX;

		Common.ADD_TELL_PREFIX = false;
		Common.tell(sender, messages);
		Common.ADD_TELL_PREFIX = was;
	}

	/**
	 * Sends sender a message with {} variables replaced and colors supported
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tell(final CommandSender sender, final Replacer replacer) {
		Common.tell(sender, replacer.getReplacedMessage());
	}

	/**
	 * Send the sender a bunch of messages, colors & are supported
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tell(final CommandSender sender, final Collection<String> messages) {
		Common.tell(sender, Common.toArray(messages));
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
				Common.tellJson(sender, message);
	}

	/**
	 * Tells the sender a basic message with & colors replaced and {player} with his variable replaced.
	 * <p>
	 * If the message starts with [JSON] than we remove the [JSON] prefix and handle the message
	 * as a valid JSON component.
	 * <p>
	 * Finally, a prefix to non-json messages is added, see {@link #getTellPrefix()}
	 *
	 * @param sender
	 * @param message
	 */
	public static void tellJson(@NonNull final CommandSender sender, String message) {
		if (message.isEmpty() || "none".equals(message))
			return;

		// Has prefix already? This is replaced when colorizing
		final boolean hasPrefix = message.contains("{prefix}");

		// Add colors and replace player
		message = Replacer.of(message).find("player", "plugin_name", "plugin.name", "plugin_version", "plugin.version").replace(Common.resolveSenderName(sender), SimplePlugin.getNamed(), SimplePlugin.getNamed(), SimplePlugin.getVersion(), SimplePlugin.getVersion()).getReplacedMessageJoined();
		message = Common.colorize(message);

		// Send [JSON] prefixed messages as json component
		if (message.startsWith("[JSON]")) {
			String stripped = message.substring(6);

			if (stripped.startsWith(" "))
				stripped = stripped.substring(1);

			if (!stripped.isEmpty())
				Remain.sendJson(sender, stripped);

		} else
			for (final String part : Common.splitNewline(message)) {
				final String prefix = Common.removeSurroundingSpaces(Common.tellPrefix);
				final String toSend = (Common.ADD_TELL_PREFIX && !hasPrefix && !prefix.isEmpty() ? prefix + " " : "") + part;

				if (Common.SEND_TELL_TO_CONVERSING && sender instanceof Conversable && ((Conversable) sender).isConversing())
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
		message = Common.getOrEmpty(message);

		while (message.startsWith(" "))
			message = message.substring(1);

		return message;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Colorizing messages
	// ------------------------------------------------------------------------------------------------------------

	public static String colorizeIfPermission(final String message, final Player player, final String permission) {
		if (!player.hasPermission(permission))
			return message;
		return Common.colorize(message);
	}

	public static String colorizeIf(final String message, final boolean conditition) {
		if (!conditition)
			return message;
		return Common.colorize(message);
	}

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
				copy.set(i, Common.colorize(message));
		}

		return copy;
	}

	/**
	 * Replace the & letter with the {@link org.bukkit.ChatColor.COLOR_CHAR} in the message.
	 *
	 * @param messages the messages to replace color codes with '&'
	 * @return the colored message
	 */
	public static String colorize(final String... messages) {
		return Common.colorize(StringUtils.join(messages, "\n"));
	}

	/**
	 * Replace the & letter with the {@link org.bukkit.ChatColor.COLOR_CHAR} in the message.
	 * <p>
	 * Also replaces {prefix} with {@link #getTellPrefix()} and {server} with {@link SimplePlugin#getServerPrefix()}
	 *
	 * @param message the message to replace color codes with '&'
	 * @return the colored message
	 */
	public static String colorize(final String message) {
		String result = message == null || message.isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', message.replace("{prefix}", message.startsWith(Common.tellPrefix) ? "" : Common.removeSurroundingSpaces(Common.tellPrefix.trim())).replace("{server}", SimpleLocalization.SERVER_PREFIX).replace("{plugin.name}", SimplePlugin.getNamed().toLowerCase()));

		// Don't replace rgb colors when older than 1.16
		if (!MinecraftVersion.newerThan(MinecraftVersion.V.v1_15))
			return result;
		// RGB colors
		final Pattern pattern = Pattern.compile(Pattern.quote("{#") + "(.*?)" + Pattern.quote("}"));
		final Matcher match = pattern.matcher(result);
		while (match.find()) {
			final String colorCode = match.group(1);
			result = result.replaceAll("\\{#" + colorCode + "\\}", Common.getColor("#" + colorCode).toString());
		}
		return result;

	}

	private static net.md_5.bungee.api.ChatColor getColor(@NonNull final String message) {
		Valid.checkBoolean(MinecraftVersion.newerThan(MinecraftVersion.V.v1_15), "Minecraft version must be newer than 1.15!");
		try {
			final Class<?> clazz = net.md_5.bungee.api.ChatColor.class;
			final Method method = clazz.getMethod("of", String.class);
			final Object object = method.invoke(null, message);
			return (net.md_5.bungee.api.ChatColor) object;

		} catch (final Exception e) {
			return net.md_5.bungee.api.ChatColor.WHITE;
		}
	}

	// Remove first and last spaces from the given message
	private static String removeSurroundingSpaces(String message) {
		message = Common.getOrEmpty(message);

		while (message.endsWith(" "))
			message = message.substring(0, message.length() - 1);

		return Common.removeFirstSpaces(message);
	}

	/**
	 * Replaces the {@link ChatColor#COLOR_CHAR} colors with & letters
	 *
	 * @param messages
	 * @return
	 */
	public static String[] revertColorizing(final String[] messages) {
		for (int i = 0; i < messages.length; i++)
			messages[i] = Common.revertColorizing(messages[i]);

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
	public static String stripColors(final String message) {
		return message == null ? "" : message.replaceAll("(" + ChatColor.COLOR_CHAR + "|&)([0-9a-fk-or])", "");
	}

	/**
	 * Returns if the message contains either {@link ChatColor#COLOR_CHAR} or & letter colors
	 *
	 * @param message
	 * @return
	 */
	public static boolean hasColors(final String message) {
		return Common.COLOR_REGEX.matcher(message).find();
	}

	/**
	 * Returns the last color, either & or {@link ChatColor#COLOR_CHAR} from the given message
	 *
	 * @param message, or empty if none
	 * @return
	 */
	public static String lastColor(final String message) {
		final String andLetter = Common.lastColorLetter(message);
		final String colorChat = Common.lastColorChar(message);

		return !andLetter.isEmpty() ? andLetter : !colorChat.isEmpty() ? colorChat : "";
	}

	/**
	 * Return last color & + the color letter from the message, or empty if not exist
	 *
	 * @param message
	 * @return
	 */
	public static String lastColorLetter(final String message) {
		return Common.lastColor(message, '&');
	}

	/**
	 * Return last {@link ChatColor#COLOR_CHAR} + the color letter from the message, or empty if not exist
	 *
	 * @param message
	 * @return
	 */
	public static String lastColorChar(final String message) {
		return Common.lastColor(message, ChatColor.COLOR_CHAR);
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
			return Common.lastColor(msg.substring(0, c), colorChar);
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
		return "*----------------------------------------------------*";
	}

	/**
	 * Returns a long ----------- chat line with strike color
	 *
	 * @return
	 */
	public static String chatLineSmooth() {
		return ChatColor.STRIKETHROUGH + "――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――";
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
	 * Limits length to 60 chars.
	 * <p>
	 * If JSON, unpacks it and display [json] prefix.
	 */
	public static String formatStringHover(final String msg) {
		String finalText = msg;

		if (msg.startsWith("[JSON]")) {
			final String stripped = msg.replaceFirst("\\[JSON\\]", "").trim();

			if (!stripped.isEmpty())
				finalText = "&8[&6json&8] &r" + StringUtils.join(Common.splitNewline(Remain.toLegacyText(stripped, false)));
		}

		return finalText.length() <= 60 ? finalText : finalText.substring(0, 60) + "...";
	}

	/**
	 * Return a formated list as a string separated by gray and white colors evenly
	 *
	 * @param list
	 * @return
	 */
	public static String formatList(final Collection<String> list) {
		return Common.formatList(list, ChatColor.GRAY, ChatColor.WHITE);
	}

	/**
	 * Return a formated list as a string separated by comma and colors
	 *
	 * @param list
	 * @param primary
	 * @param secondary
	 * @return
	 */
	public static String formatList(final Collection<String> list, final ChatColor primary, final ChatColor secondary) {
		String formatted = "";
		boolean toggle = true;

		// Add all types and add variated color separation
		for (final String type : list) {
			formatted += (toggle ? primary : secondary) + type + "&8, ";

			toggle = !toggle;
		}

		// Remove the last color + ", " from the string
		return primary + "(" + list.size() + ") " + (formatted.length() > 3 ? formatted.substring(0, formatted.length() - 4) : formatted);
	}

	/**
	 * If the count is 0 or over 1, adds an "s" to the given string
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String plural(final long count, final String ofWhat) {
		final String exception = Common.getException(count, ofWhat);

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
		final String exception = Common.getException(count, ofWhat);

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
		final String exception = Common.getException(count, ofWhat);

		return exception != null ? exception : count + " " + (count == 0 || count > 1 && !ofWhat.endsWith("ies") ? ofWhat.substring(0, ofWhat.length() - 1) + "ies" : ofWhat);
	}

	/*
	 * Return the plural word from the exception list or null if none
	 */
	private static String getException(final long count, final String ofWhat) {
		return Common.PLURAL_EXCEPTIONS.containsKey(ofWhat) ? count + " " + (count == 0 || count > 1 ? Common.PLURAL_EXCEPTIONS.getString(ofWhat) : ofWhat) : null;
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
	public static boolean doesPluginExist(final String plugin) {
		final boolean hooked = Common.doesPluginExistSilently(plugin);

		if (hooked)
			Common.log("&3Hooked into&8: &f" + plugin);

		return hooked;
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
			Common.runLaterAsync(0, () -> Valid.checkBoolean(found.isEnabled(), SimplePlugin.getNamed() + " could not hook into " + pluginName + " as the plugin is disabled! (DO NOT REPORT THIS TO " + SimplePlugin.getNamed() + ", look for errors above and contact support " + pluginName + ")"));

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Running commands
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Runs the given command (without /) as the console, replacing {player} with sender
	 *
	 * @param playerReplacement
	 * @param command
	 */
	public static void dispatchCommand(@Nullable final CommandSender playerReplacement, @NonNull final String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		Common.runLater(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Common.colorize(command.replace("{player}", playerReplacement == null ? "" : Common.resolveSenderName(playerReplacement)))));
	}

	/**
	 * Runs the given command (without /) as if the sender would type it, replacing {player} with his name
	 *
	 * @param playerSender
	 * @param command
	 */
	public static void dispatchCommandAsPlayer(@NonNull final Player playerSender, @NonNull final String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		Common.runLater(() -> playerSender.performCommand(Common.colorize(command.replace("{player}", Common.resolveSenderName(playerSender)))));
	}

	/**
	 * Return true if the command label is registered
	 *
	 * @param label
	 * @return
	 */
	public boolean isRegistered(final String label) {
		final String[] split = label.split(" ");
		final PluginCommand command = Bukkit.getServer().getPluginCommand(split[0]);

		return command != null && command.isRegistered();
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
		if (!Common.TIMED_LOG_CACHE.containsKey(msg)) {
			Common.log(msg);
			Common.TIMED_LOG_CACHE.put(msg, TimeUtil.currentTimeSeconds());
			return;
		}

		if (TimeUtil.currentTimeSeconds() - Common.TIMED_LOG_CACHE.get(msg) > delaySec) {
			Common.log(msg);
			Common.TIMED_LOG_CACHE.put(msg, TimeUtil.currentTimeSeconds());
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
		final String formatted = Common.format(format, args);

		Common.log(false, formatted);
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
				args[i] = Replacer.simplify(arg);
		}

		return String.format(format, args);
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param messages
	 */
	public static void log(final List<String> messages) {
		Common.log(Common.toArray(messages));
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param messages
	 */
	public static void log(final String... messages) {
		Common.log(true, messages);
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 * <p>
	 * Does not add {@link #getLogPrefix()}
	 *
	 * @param messages
	 */
	public static void logNoPrefix(final String... messages) {
		Common.log(false, messages);
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param addLogPrefix should we add {@link #getLogPrefix()} ?
	 * @param messages
	 */
	public static void log(final boolean addLogPrefix, final String... messages) {
		if (messages == null)
			return;

		for (String message : messages) {
			if (message.equals("none") || message.isEmpty())
				continue;

			if (Common.stripColors(message).replace(" ", "").isEmpty()) {
				if (Common.CONSOLE_SENDER == null)
					System.out.println(" ");
				else
					Common.CONSOLE_SENDER.sendMessage("  ");

				continue;
			}

			message = Replacer.of(message).find("plugin_name", "plugin.name", "plugin_version", "plugin.version").replace(SimplePlugin.getNamed(), SimplePlugin.getNamed(), SimplePlugin.getVersion(), SimplePlugin.getVersion()).getReplacedMessageJoined();
			message = Common.colorize(message);

			if (message.startsWith("[JSON]")) {
				final String stripped = message.replaceFirst("\\[JSON\\]", "").trim();

				if (!stripped.isEmpty())
					Common.log(Remain.toLegacyText(stripped, false));

			} else
				for (final String part : Common.splitNewline(message)) {
					final String log = ((addLogPrefix && Common.ADD_LOG_PREFIX ? Common.removeSurroundingSpaces(Common.logPrefix) + " " : "") + Common.getOrEmpty(part).replace("\n", Common.colorize("\n&r"))).trim();

					if (Common.CONSOLE_SENDER != null)
						Common.CONSOLE_SENDER.sendMessage(log);
					else
						System.out.println("[" + SimplePlugin.getNamed() + "] " + Common.stripColors(log));
				}
		}

	}

	/**
	 * Logs a bunch of messages to the console in a {@link #consoleLine()} frame.
	 *
	 * @param messages
	 */
	public static void logFramed(final String... messages) {
		Common.logFramed(false, messages);
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
			Common.log("&7" + Common.consoleLine());
			for (final String msg : messages)
				Common.log(" &c" + msg);

			if (disablePlugin)
				Common.log(" &cPlugin is now disabled.");

			Common.log("&7" + Common.consoleLine());
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
		Common.error(false, t, messages);
	}

	/**
	 * Saves the error, prints the stack trace and logs it in frame.
	 * Possible to use %error variable
	 *
	 * @param disablePlugin shall we disable this plugin ?
	 * @param t
	 * @param messages
	 */
	public static void error(final boolean disablePlugin, final Throwable t, final String... messages) {
		if (!(t instanceof FoException))
			Debugger.saveError(t, messages);

		Debugger.printStackTrace(t);
		Common.logFramed(disablePlugin, Common.replaceErrorVariable(t, messages));
	}

	/**
	 * Logs the messages in frame (if not null),
	 * saves the error to errors.log and then throws it
	 * <p>
	 * Possible to use %error variable
	 *
	 * @param throwable
	 * @param messages
	 */
	public static void throwError(Throwable throwable, final String... messages) {
		if (throwable.getCause() != null)
			throwable = throwable.getCause();

		if (messages != null)
			Common.logFramed(false, Common.replaceErrorVariable(throwable, messages));

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
	private static String[] replaceErrorVariable(Throwable throwable, final String... msgs) {
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
	public static boolean regExMatch(final String regex, final String message) {
		return Common.regExMatch(Common.compilePattern(regex), message);
	}

	/**
	 * Returns true if the given pattern matches the given message
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static boolean regExMatch(final Pattern regex, final String message) {
		return Common.regExMatch(Common.compileMatcher(regex, message));
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
		try {
			return matcher != null ? matcher.find() : false;

		} catch (final RegexTimeoutException ex) {
			FileUtil.writeFormatted(FoConstants.File.ERRORS, null, "Matching timed out (bad regex?) (plugin ver. " + SimplePlugin.getVersion() + ")! \nString checked: " + ex.getCheckedMessage() + "\nRegex: " + (matcher != null ? matcher.pattern().pattern() : "null") + "");

			Common.logFramed(false, "&cRegex check took too long! (allowed: " + SimpleSettings.REGEX_TIMEOUT + "ms)", "&cRegex:&f " + (matcher != null ? matcher.pattern().pattern() : matcher), "&cMessage:&f " + ex.getCheckedMessage());

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
			final String strippedMessage = SimplePlugin.getInstance().regexStripColors() ? Common.stripColors(message) : message;
			final int timeout = SimpleSettings.REGEX_TIMEOUT;

			return pattern.matcher(new TimedCharSequence(strippedMessage, timeout));

		} catch (final RegexTimeoutException ex) {
			FileUtil.writeFormatted(FoConstants.File.ERRORS, null, "Regex check timed out (bad regex?) (plugin ver. " + SimplePlugin.getVersion() + ")! \nString checked: " + ex.getCheckedMessage() + "\nRegex: " + pattern.pattern() + "");

			Common.throwError(ex, "&cChecking a message took too long! (limit: " + SimpleSettings.REGEX_TIMEOUT + ")", "&cReg-ex:&f " + pattern.pattern(), "&cString:&f " + ex.getCheckedMessage());
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
		return Common.compileMatcher(Common.compilePattern(regex), message);
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

		regex = SimplePlugin.getInstance().regexStripColors() ? Common.stripColors(regex) : regex;

		try {
			if (instance.regexCaseInsensitive())
				pattern = Pattern.compile(regex, instance.regexUnicode() ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : Pattern.CASE_INSENSITIVE);

			else
				pattern = instance.regexUnicode() ? Pattern.compile(regex, Pattern.UNICODE_CASE) : Pattern.compile(regex);

		} catch (final PatternSyntaxException ex) {
			Common.throwError(ex, "Malformed regex: \'" + regex + "\'", "Use online services (like &fregex101.com&f) for fixing errors");

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
	@SafeVarargs
	public static <T> List<T> joinArrays(final Iterable<T>... arrays) {
		final List<T> all = new ArrayList<>();

		for (final Iterable<T> array : arrays)
			for (final T element : array)
				all.add(element);

		return all;
	}

	/**
	 * Join a strict list array into one big list
	 *
	 * @param <T>
	 * @param lists
	 * @return
	 */
	@SafeVarargs
	public static <T> StrictArrayList<T> join(final StrictArrayList<T>... lists) {
		final StrictArrayList<T> joined = new StrictArrayList<>();

		for (final StrictArrayList<T> list : lists)
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
	public static String joinRange(final int startIndex, final String[] array) {
		return Common.joinRange(startIndex, array.length, array);
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
		return Common.joinRange(startIndex, stopIndex, array, " ");
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

		return Common.join(Arrays.asList(array), delimiter, stringer);
	}

	/**
	 * A convenience method for converting array of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String joinToString(final T[] array) {
		return array == null ? "null" : Common.joinToString(Arrays.asList(array));
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String joinToString(final Iterable<T> array) {
		return array == null ? "null" : Common.joinToString(array, ", ");
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
	public static <T> String joinToString(final Iterable<T> array, final String delimiter) {
		return Common.join(array, delimiter, object -> object == null ? "" : Replacer.simplify(object));
	}

	/**
	 * A convenience method for converting array of command senders into array of their names
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T extends CommandSender> String joinPlayers(final Iterable<T> array) {
		return Common.join(array, ", ", (Stringer<T>) CommandSender::getName);
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
	 * Convenience method for getting a list of world names
	 *
	 * @return
	 */
	public static List<String> getWorldNames() {
		return Common.convert(Bukkit.getWorlds(), World::getName);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting and retyping
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for getting a list of player names
	 *
	 * @return
	 */
	public static List<String> getPlayerNames() {
		return Common.getPlayerNames(true);
	}

	/**
	 * Convenience method for getting a list of player names
	 *
	 * @param includeVanished
	 * @return
	 */
	public static List<String> getPlayerNames(final boolean includeVanished) {
		final List<String> found = new ArrayList<>();

		for (final Player online : Remain.getOnlinePlayers()) {
			if (PlayerUtil.isVanished(online) && !includeVanished)
				continue;

			found.add(online.getName());
		}

		return found;
	}

	/**
	 * Convenience method for getting a list of players that the
	 * sender can see and are not vanished
	 *
	 * @param sender
	 * @return
	 */
	public static List<String> getPlayerNames(final Player sender) {
		final List<String> found = new ArrayList<>();

		for (final Player online : Remain.getOnlinePlayers())
			if (!PlayerUtil.isVanished(online, sender))
				found.add(online.getName());

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
	 * Converts a set from one type to another
	 *
	 * @param <OLD>
	 * @param <NEW>
	 * @param list
	 * @param converter
	 * @return
	 */
	public static <OLD, NEW> Set<NEW> convertSet(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
		final Set<NEW> copy = new HashSet<>();

		for (final OLD old : list)
			copy.add(converter.convert(old));

		return copy;
	}

	/**
	 * Converts a list having one type object into another
	 *
	 * @param list      the old list
	 * @param converter the converter
	 * @return the new list
	 */
	public static <OLD, NEW> StrictArrayList<NEW> convertStrict(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
		final StrictArrayList<NEW> copy = new StrictArrayList<>();

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
	public static <LIST_KEY, OLD_KEY, OLD_VALUE> StrictArrayList<LIST_KEY> convertToList(final Map<OLD_KEY, OLD_VALUE> map, final MapToListConverter<LIST_KEY, OLD_KEY, OLD_VALUE> converter) {
		final StrictArrayList<LIST_KEY> list = new StrictArrayList<>();

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
	 * Replaces string by a substitute for each element in the array
	 *
	 * @param what
	 * @param byWhat
	 * @param messages
	 * @return
	 */
	public static String[] replace(final String what, final String byWhat, final String... messages) {
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
	public static List<String> replace(final String what, final String byWhat, final List<String> messages) {
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
	public static String[] replaceNuls(final String[] list) {
		for (int i = 0; i < list.length; i++)
			if (list[i] == null)
				list[i] = "";

		return list;
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
	public static <T> List<T> removeNulsAndEmpties(final T[] array) {
		return array != null ? Common.removeNulsAndEmpties(Arrays.asList(array)) : new ArrayList<>();
	}

	/**
	 * Creates a new list only containing non-null and not empty string elements
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static <T> List<T> removeNulsAndEmpties(final List<T> list) {
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
	 * Get an array from the given object. If the object
	 * is a list, return its array, otherwise return an array only containing the
	 * object as the first element
	 *
	 * @param obj
	 * @return
	 */
	public static String[] getListOrString(final Object obj) {
		if (obj instanceof List) {
			final List<String> cast = (List<String>) obj;

			return Common.toArray(cast);
		}

		return new String[] { obj.toString() };
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
	 * @param value the primary value
	 * @param def   the default value
	 * @return the value, or default it the value is null
	 */
	public static <T> T getOrDefault(final T value, final T def) {
		Valid.checkNotNull(def, "The default value must not be null!");

		return value != null ? value : def;
	}

	/**
	 * Return the default value if the given string is null, "" or equals to "none"
	 *
	 * @param input
	 * @param def
	 * @return
	 */
	public static String getOrSupply(final String input, final String def) {
		return input == null || "none".equalsIgnoreCase(input) || input.isEmpty() ? def : input;
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

		return Common.getNext(given, array, forward);
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
		return array.toArray(new String[array.size()]);
	}

	/**
	 * Creates a new modifiable array list from array
	 *
	 * @param array
	 * @return
	 */
	public static <T> ArrayList<T> toList(final T... array) {
		return new ArrayList<>(Arrays.asList(array));
	}

	/**
	 * Converts {@link Iterable} to {@link List}
	 *
	 * @param it the iterable
	 * @return the new list
	 */
	public static <T> List<T> toList(final Iterable<T> it) {
		final List<T> list = new ArrayList<>();
		it.forEach(el -> list.add(el));

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
	 * Lowercases all items in the array
	 *
	 * @param list
	 * @return
	 */
	public static String[] toLowerCase(final String... list) {
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
	public static <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);

		return map;
	}

	/**
	 * Runs the task if the condition is met
	 *
	 * @param condition
	 * @param task
	 */
	public static void runLaterIf(final boolean condition, final Runnable task) {
		if (condition)
			Common.runLater(1, task);
		else
			task.run();
	}

	/**
	 * Runs the task if the plugin is enabled correctly
	 *
	 * @param task the task
	 * @return the task or null
	 */
	public static <T extends Runnable> BukkitTask runLater(final T task) {
		return Common.runLater(1, task);
	}

	/**
	 * Runs the task even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param task
	 * @return the task or null
	 */
	public static BukkitTask runLater(final int delayTicks, final Runnable task) {
		final BukkitScheduler scheduler = Bukkit.getScheduler();
		final JavaPlugin instance = SimplePlugin.getInstance();

		return Common.runIfDisabled(task) ? null : delayTicks == 0 ? task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTask(instance) : scheduler.runTask(instance, task) : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskLater(instance, delayTicks) : scheduler.runTaskLater(instance, task, delayTicks);
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
		return Common.runLaterAsync(0, task);
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
		return Common.runLaterAsync(0, task);
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
	public static BukkitTask runLaterAsync(final int delayTicks, final Runnable task) {
		final BukkitScheduler scheduler = Bukkit.getScheduler();
		final JavaPlugin instance = SimplePlugin.getInstance();

		return Common.runIfDisabled(task) ? null : delayTicks == 0 ? task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskAsynchronously(instance) : scheduler.runTaskAsynchronously(instance, task) : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskLaterAsynchronously(instance, delayTicks) : scheduler.runTaskLaterAsynchronously(instance, task, delayTicks);
	}

	/**
	 * Runs the task timer even if the plugin is disabled.
	 *
	 * @param repeatTicks the delay between each execution
	 * @param task        the task
	 * @return the bukkit task or null
	 */
	public static BukkitTask runTimer(final int repeatTicks, final Runnable task) {
		return Common.runTimer(0, repeatTicks, task);
	}

	/**
	 * Runs the task timer even if the plugin is disabled.
	 *
	 * @param delayTicks  the delay before first run
	 * @param repeatTicks the delay between each run
	 * @param task        the task
	 * @return the bukkit task or null if error
	 */
	public static BukkitTask runTimer(final int delayTicks, final int repeatTicks, final Runnable task) {
		return Common.runIfDisabled(task) ? null : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskTimer(SimplePlugin.getInstance(), delayTicks, repeatTicks) : Bukkit.getScheduler().runTaskTimer(SimplePlugin.getInstance(), task, delayTicks, repeatTicks);
	}

	/**
	 * Runs the task timer async even if the plugin is disabled.
	 *
	 * @param repeatTicks
	 * @param task
	 * @return
	 */
	public static BukkitTask runTimerAsync(final int repeatTicks, final Runnable task) {
		return Common.runTimerAsync(0, repeatTicks, task);
	}

	/**
	 * Runs the task timer async even if the plugin is disabled.
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param task
	 * @return
	 */
	public static BukkitTask runTimerAsync(final int delayTicks, final int repeatTicks, final Runnable task) {
		return Common.runIfDisabled(task) ? null : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskTimerAsynchronously(SimplePlugin.getInstance(), delayTicks, repeatTicks) : Bukkit.getScheduler().runTaskTimerAsynchronously(SimplePlugin.getInstance(), task, delayTicks, repeatTicks);
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

	// ------------------------------------------------------------------------------------------------------------
	// Bukkit
	// ------------------------------------------------------------------------------------------------------------

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

	// ------------------------------------------------------------------------------------------------------------
	// Connecting to the internet
	// ------------------------------------------------------------------------------------------------------------

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

	// ------------------------------------------------------------------------------------------------------------
	// Java convenience methods
	// ------------------------------------------------------------------------------------------------------------

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
	private final int timeoutLimit;

	/**
	 * Create a new timed message for the given message with a timeout in millis
	 *
	 * @param message
	 * @param timeoutLimit
	 */
	public TimedCharSequence(final CharSequence message, final Integer timeoutLimit) {
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
	public char charAt(final int index) {
		if (System.currentTimeMillis() > System.currentTimeMillis() + timeoutLimit)
			throw new RegexTimeoutException(message, timeoutLimit);

		return message.charAt(index);
	}

	@Override
	public int length() {
		return message.length();
	}

	@Override
	public CharSequence subSequence(final int start, final int end) {
		return new TimedCharSequence(message.subSequence(start, end), timeoutLimit);
	}

	@Override
	public String toString() {
		return message.toString();
	}
}
