package org.mineacademy.fo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.RemainCore;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Our main utility class hosting a large variety of different convenience functions
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class CommonCore {

	/**
	 * Stores legacy colors
	 */
	private static final Map<String, String> LEGACY_COLOR_MAP = new HashMap<>();

	/**
	 * The pattern for matching MiniMessage tags
	 */
	private static final Pattern MINIMESSAGE_PATTERN = Pattern.compile("<[!?#]?[a-z0-9_-]*>");

	/**
	 * Used to send messages to player without repetition, e.g. if they attempt to break a block
	 * in a restricted region, we will not spam their chat with "You cannot break this block here" 120x times,
	 * instead, we only send this message once per X seconds. This cache holds the last times when we
	 * sent that message so we know how long to wait before the next one.
	 */
	private static final Map<Component, Long> TIMED_TELL_CACHE = new HashMap<>();

	/**
	 * See {@link #TIMED_TELL_CACHE}, but this is for sending messages to your console
	 */
	private static final Map<String, Long> TIMED_LOG_CACHE = new HashMap<>();

	static {
		LEGACY_COLOR_MAP.put("&0", "<black>");
		LEGACY_COLOR_MAP.put("&1", "<dark_blue>");
		LEGACY_COLOR_MAP.put("&2", "<dark_green>");
		LEGACY_COLOR_MAP.put("&3", "<dark_aqua>");
		LEGACY_COLOR_MAP.put("&4", "<dark_red>");
		LEGACY_COLOR_MAP.put("&5", "<dark_purple>");
		LEGACY_COLOR_MAP.put("&6", "<gold>");
		LEGACY_COLOR_MAP.put("&7", "<gray>");
		LEGACY_COLOR_MAP.put("&8", "<dark_gray>");
		LEGACY_COLOR_MAP.put("&9", "<blue>");
		LEGACY_COLOR_MAP.put("&a", "<green>");
		LEGACY_COLOR_MAP.put("&b", "<aqua>");
		LEGACY_COLOR_MAP.put("&c", "<red>");
		LEGACY_COLOR_MAP.put("&d", "<light_purple>");
		LEGACY_COLOR_MAP.put("&e", "<yellow>");
		LEGACY_COLOR_MAP.put("&f", "<white>");
		LEGACY_COLOR_MAP.put("&n", "<u>");
		LEGACY_COLOR_MAP.put("&m", "<st>");
		LEGACY_COLOR_MAP.put("&k", "<obf>");
		LEGACY_COLOR_MAP.put("&o", "<i>");
		LEGACY_COLOR_MAP.put("&l", "<b>");
		LEGACY_COLOR_MAP.put("&r", "<r>");
		LEGACY_COLOR_MAP.put("§0", "<black>");
		LEGACY_COLOR_MAP.put("§1", "<dark_blue>");
		LEGACY_COLOR_MAP.put("§2", "<dark_green>");
		LEGACY_COLOR_MAP.put("§3", "<dark_aqua>");
		LEGACY_COLOR_MAP.put("§4", "<dark_red>");
		LEGACY_COLOR_MAP.put("§5", "<dark_purple>");
		LEGACY_COLOR_MAP.put("§6", "<gold>");
		LEGACY_COLOR_MAP.put("§7", "<gray>");
		LEGACY_COLOR_MAP.put("§8", "<dark_gray>");
		LEGACY_COLOR_MAP.put("§9", "<blue>");
		LEGACY_COLOR_MAP.put("§a", "<green>");
		LEGACY_COLOR_MAP.put("§b", "<aqua>");
		LEGACY_COLOR_MAP.put("§c", "<red>");
		LEGACY_COLOR_MAP.put("§d", "<light_purple>");
		LEGACY_COLOR_MAP.put("§e", "<yellow>");
		LEGACY_COLOR_MAP.put("§f", "<white>");
		LEGACY_COLOR_MAP.put("§n", "<u>");
		LEGACY_COLOR_MAP.put("§m", "<st>");
		LEGACY_COLOR_MAP.put("§k", "<obf>");
		LEGACY_COLOR_MAP.put("§o", "<i>");
		LEGACY_COLOR_MAP.put("§l", "<b>");
		LEGACY_COLOR_MAP.put("§r", "<r>");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Plugin prefixes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * The tell prefix applied on tell() methods, defaults to empty
	 */
	@Getter
	private static Component tellPrefix = Component.empty();

	/**
	 * The log prefix applied on log() methods, defaults to [PluginName]
	 */
	@Getter
	private static String logPrefix = "[" + Platform.getPluginName() + "]";

	/**
	 * Set the tell prefix applied for messages to players from tell() methods
	 * <p>
	 * Colors with & letter are translated automatically.
	 *
	 * @param prefix
	 */
	public static void setTellPrefix(final Component prefix) {
		tellPrefix = prefix == null ? Component.empty() : prefix;
	}

	/**
	 * Set the log prefix applied for messages in the console from log() methods.
	 * <p>
	 * Colors with & letter are translated automatically.
	 *
	 * @param prefix
	 */
	public static void setLogPrefix(final String prefix) {
		logPrefix = prefix == null ? "" : prefix;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Broadcasting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Broadcast the message replacing {player} variable with the given command sender
	 *
	 * @param message
	 * @param playerReplacement
	 */
	public static void broadcast(final Component message, final Audience playerReplacement) {
		broadcast(message, Platform.resolveSenderName(playerReplacement));
	}

	/**
	 * Broadcast the message replacing {player} variable with the given player replacement
	 *
	 * @param message
	 * @param playerReplacement
	 */
	public static void broadcast(final Component message, final String playerReplacement) {
		broadcast(message.replaceText(b -> b.matchLiteral("{player}").replacement(playerReplacement)));
	}

	/**
	 * Broadcast the message to everyone and logs it
	 *
	 * @param message
	 */
	public static void broadcast(final Component message) {
		if (message != null) {
			final String legacy = RemainCore.convertAdventureToLegacy(message);

			for (final Audience online : Platform.getOnlinePlayers())
				tell(online, message);

			log(legacy);
		}
	}

	/**
	 * Sends messages to all recipients
	 *
	 * @param recipients
	 * @param message
	 */
	public static void broadcastTo(final Iterable<Audience> recipients, final Component message) {
		for (final Audience recipient : recipients)
			tell(recipient, message);
	}

	/**
	 * Broadcast the text component message to everyone with permission
	 *
	 * @param showPermission
	 * @param message
	 * @param log
	 */
	public static void broadcastWithPerm(final String showPermission, @NonNull final Component message, final boolean log) {
		final String legacy = RemainCore.convertAdventureToLegacy(message);
		final String plain = RemainCore.convertAdventureToPlain(message);

		if (!plain.equals("none")) {
			for (final Audience online : Platform.getOnlinePlayers())
				if (Platform.hasPermission(online, showPermission))
					online.sendMessage(message);

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
	 *
	 * Does not prepend the message with {@link #getTellPrefix()}
	 *
	 * @param delaySeconds
	 * @param sender
	 * @param message
	 */
	public static void tellTimedNoPrefix(final int delaySeconds, final Audience sender, final Component message) {
		final Component oldPrefix = tellPrefix;
		tellPrefix = Component.empty();

		tellTimed(delaySeconds, sender, message);
		tellPrefix = oldPrefix;
	}

	/**
	 * Sends a message to the player and saves the time when it was sent.
	 * The delay in seconds is the delay between which we won't send player the
	 * same message, in case you call this method again.
	 *
	 * @param delaySeconds
	 * @param sender
	 * @param message
	 */
	public static void tellTimed(final int delaySeconds, final Audience sender, final Component message) {

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
	 * Sends a message to the sender with a given delay, colors & are supported
	 *
	 * @param sender
	 * @param delayTicks
	 * @param message
	 */
	public static void tellLater(final int delayTicks, final Audience sender, final Component message) {
		Platform.runTask(delayTicks, () -> {
			if (Platform.isOnline(sender))
				tell(sender, message);
		});
	}

	/**
	 * Sends the sender a bunch of messages, colors & are supported
	 * without {@link #getTellPrefix()} prefix
	 *
	 * @param sender
	 * @param message
	 */
	public static void tellNoPrefix(final Audience sender, final Component message) {
		final Component oldPrefix = tellPrefix;

		tellPrefix = Component.empty();
		tell(sender, message);
		tellPrefix = oldPrefix;
	}

	/**
	 * Sends a message to the audience. Supports {prefix} and {player} variable.
	 * Supports \<actionbar\>, \<toast\>, \<title\>, \<bossbar\> and \<center\>.
	 * Properly sends the message to the player if he is conversing with the server.
	 *
	 * @param audience
	 * @param message
	 */
	public static void tell(@NonNull final Audience audience, Component message) {
		if (message == null)
			return;

		final String plainMessage = RemainCore.convertAdventureToPlain(message);
		final String plainTellPrefix = RemainCore.convertAdventureToPlain(tellPrefix);

		// Support localization being none or empty
		if (plainMessage.isEmpty() || "none".equals(plainMessage))
			return;

		// Add tell prefix if not present already
		if (!plainTellPrefix.isEmpty() && !plainMessage.startsWith(plainTellPrefix) && !plainMessage.contains("{prefix}"))
			message = tellPrefix.append(message);

		// Replace player variable
		message = message.replaceText(b -> b.matchLiteral("{player}").replacement(Platform.resolveSenderName(audience)));

		if (plainMessage.startsWith("<actionbar>")) {
			Platform.sendActionBar(audience, message.replaceText(b -> b.matchLiteral("<actionbar>").replacement("")));

		} else if (plainMessage.startsWith("<toast>")) {
			Platform.sendToast(audience, message.replaceText(b -> b.matchLiteral("<toast>").replacement("")));

		} else if (plainMessage.startsWith("<title>")) {
			final String stripped = RemainCore.convertAdventureToLegacy(message).replace("<title>", "").trim();

			if (!stripped.isEmpty()) {
				final String[] split = stripped.split("\\|");
				final String title = split[0];
				final String subtitle = split.length > 1 ? CommonCore.joinRange(1, split) : null;

				RemainCore.sendTitle(audience, 0, 60, 0, RemainCore.convertLegacyToAdventure(title), RemainCore.convertLegacyToAdventure(subtitle));
			}

		} else if (plainMessage.startsWith("<bossbar>")) {
			RemainCore.sendBossbarTimed(audience, message.replaceText(b -> b.matchLiteral("<bossbar>").replacement("")), 10, 1F);

		} else {

			if (plainMessage.startsWith("<center>")) {
				final String centeredLegacyMessage = ChatUtil.center(RemainCore.convertAdventureToLegacy(message).replace("\\<center\\>(\\s|)", ""));

				if (Platform.isConversing(audience))
					Platform.sendConversingMessage(audience, colorize(centeredLegacyMessage));

				else
					audience.sendMessage(RemainCore.convertLegacyToAdventure(centeredLegacyMessage));

			} else {
				if (Platform.isConversing(audience))
					Platform.sendConversingMessage(audience, message);

				else
					audience.sendMessage(message);
			}
		}
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
	/*public static List<String> colorize(final List<String> list) {
		final List<String> copy = new ArrayList<>();
		copy.addAll(list);
	
		for (int i = 0; i < copy.size(); i++) {
			final String message = copy.get(i);
	
			if (message != null)
				copy.set(i, colorize(message));
		}
	
		return copy;
	}*/

	/**
	 * Replace the & letter with the {@link CompChatColor#COLOR_CHAR} in the message.
	 *
	 * @param messages the messages to replace color codes with '&'
	 * @return the colored message
	 */
	/*public static String colorize(final String... messages) {
		return colorize(String.join("\n", messages));
	}*/

	/**
	 * Replace the & letter with the {@link CompChatColor#COLOR_CHAR} in the message.
	 *
	 * @param messages the messages to replace color codes with '&'
	 * @return the colored message
	 */
	/*public static String[] colorizeArray(final String... messages) {
		for (int i = 0; i < messages.length; i++)
			messages[i] = colorize(messages[i]);
	
		return messages;
	}*/

	/**
	 * Replaces & color codes and MiniMessage tags in the message.
	 * Also replaces {prefix}, {plugin_name} and {plugin_version} with their respective values.
	 *
	 * @param message
	 * @return
	 */
	public static String colorizeLegacy(final String message) {
		return RemainCore.convertAdventureToLegacy(colorize(message));
	}

	/**
	 * See colorize(). This method is for an array of messages
	 *
	 * @param messages
	 * @return
	 */
	public static Component[] colorizeList(String[] messages) {
		final List<Component> components = new ArrayList<>();

		for (final String message : messages)
			components.add(colorize(message));

		return components.toArray(new Component[components.size()]);
	}

	/**
	 * Replaces & color codes and MiniMessage tags in the message.
	 * Also replaces {prefix}, {plugin_name} and {plugin_version} with their respective values.
	 *
	 * @param message
	 * @return
	 */
	public static Component colorize(String message) {
		return colorize0(message)
				.replaceText(b -> b.matchLiteral("{prefix}").replacement(tellPrefix))
				.replaceText(b -> b.matchLiteral("{plugin_name}").replacement(Platform.getPluginName()))
				.replaceText(b -> b.matchLiteral("{plugin_version}").replacement(Platform.getPluginVersion()));
	}

	/*
	 * Replaces & color codes and MiniMessage tags in the message
	 */
	private static Component colorize0(String message) {
		if (message == null || message.isEmpty())
			return Component.empty();

		// First, replace legacy & color codes
		final StringBuilder result = new StringBuilder();

		for (int i = 0; i < message.length(); i++) {
			if (i + 1 < message.length() && (message.charAt(i) == '&' || message.charAt(i) == '§')) {
				final String code = message.substring(i, i + 2);

				if (LEGACY_COLOR_MAP.containsKey(code)) {
					result.append(LEGACY_COLOR_MAP.get(code));
					i++;

					continue;
				}

				if (i + 7 < message.length() && message.charAt(i + 1) == '#' && message.substring(i + 2, i + 8).matches("[0-9a-fA-F]{6}")) {
					result.append("<#").append(message.substring(i + 2, i + 8)).append(">");
					i += 7;

					continue;
				}
			}

			result.append(message.charAt(i));
		}

		message = result.toString();
		message = escapeInvalidTags(message);

		try {
			return MiniMessage.miniMessage().deserialize(message);

		} catch (final Throwable t) {
			Debugger.printStackTrace("Error parsing mini message tags in: " + message);

			RemainCore.sneaky(t);
			return null;
		}
	}

	/*
	 * Escapes invalid minimessage tags in the message.
	 */
	private static String escapeInvalidTags(String input) {
		final Matcher matcher = Pattern.compile("<[^>]*>").matcher(input);
		final StringBuffer buffer = new StringBuffer();

		while (matcher.find()) {
			String match = matcher.group(0);

			if (!MINIMESSAGE_PATTERN.matcher(match).matches())
				match = match.replace("<", "\\\\<").replace(">", "\\>");

			matcher.appendReplacement(buffer, match);
		}

		matcher.appendTail(buffer);
		return buffer.toString();
	}

	/**
	 * Remove all & and § colors as well as MiniMessage tags.
	 *
	 * @param message
	 * @return
	 */
	public static String removeColors(String message) {
		if (message == null || message.isEmpty())
			return message;

		final Component component = CommonCore.colorize(message);

		return RemainCore.convertAdventureToPlain(component);
	}

	/**
	 * Returns if the message contains & or § color codes, or MiniMessage tags.
	 *
	 * @param message
	 * @return
	 */
	public static boolean hasColorTags(final String message) {
		final Component component = CommonCore.colorize(message);
		final String legacy = RemainCore.convertAdventureToLegacy(component).toLowerCase();

		return Pattern.compile("§([0-9a-fk-or])").matcher(legacy).find();
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
	 * Convenience method for printing count with what the list actually contains.
	 * Example:
	 * "X bosses: Creeper, Zombie
	 *
	 * @param iterable
	 * @param ofWhat
	 * @return
	 */
	public static <T> String plural(final Collection<T> iterable, final String ofWhat) {
		return plural(iterable.size(), ofWhat) + ": " + join(iterable);
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
				"boss", "bosses",
				"iron", "iron",
				"Iron", "Iron",
				"gold", "gold",
				"Gold", "Gold");

		return exceptions.containsKey(ofWhat) ? count + " " + (count == 0 || count > 1 ? exceptions.getString(ofWhat) : ofWhat) : null;
	}

	/**
	 * Prepends the given string with either "a" or "an" (does a dummy syllable check)
	 *
	 * @param ofWhat
	 * @return
	 * @deprecated only a dummy syllable check
	 */
	@Deprecated
	public static String article(final String ofWhat) {
		ValidCore.checkBoolean(ofWhat.length() > 0, "String cannot be empty");
		final List<String> syllables = Arrays.asList("a", "e", "i", "o", "u", "y");

		// Special cases
		if ("hour".equals(ofWhat))
			return "an hour";

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
	public static String fancyBar(final int min, final char minChar, final int max, final char maxChar, final CompChatColor delimiterColor) {
		String formatted = "";

		for (int i = 0; i < min; i++)
			formatted += minChar;

		formatted += delimiterColor;

		for (int i = 0; i < max - min; i++)
			formatted += maxChar;

		return formatted;
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
	public static void dispatchCommand(Audience playerReplacement, @NonNull String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		if (command.startsWith("@announce ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @announce without a player in: " + command);

			MessengerCore.announce(playerReplacement, command.replace("@announce ", ""));
		}

		else if (command.startsWith("@warn ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @warn without a player in: " + command);

			MessengerCore.warn(playerReplacement, command.replace("@warn ", ""));
		}

		else if (command.startsWith("@error ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @error without a player in: " + command);

			MessengerCore.error(playerReplacement, command.replace("@error ", ""));
		}

		else if (command.startsWith("@info ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @info without a player in: " + command);

			MessengerCore.info(playerReplacement, command.replace("@info ", ""));
		}

		else if (command.startsWith("@question ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @question without a player in: " + command);

			MessengerCore.question(playerReplacement, command.replace("@question ", ""));
		}

		else if (command.startsWith("@success ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @success without a player in: " + command);

			MessengerCore.success(playerReplacement, command.replace("@success ", ""));
		}

		else {
			command = command.startsWith("/") && !command.startsWith("//") ? command.substring(1) : command;
			command = command.replace("{player}", playerReplacement == null ? "" : Platform.resolveSenderName(playerReplacement));

			// Workaround for JSON in tellraw getting HEX colors replaced
			if (!command.startsWith("tellraw"))
				command = colorizeLegacy(command);

			checkBlockedCommands(playerReplacement, command);

			final String finalCommand = command;

			Platform.runTask(0, () -> Platform.dispatchConsoleCommand(finalCommand));
		}
	}

	/**
	 * Runs the given command (without /) as if the sender would type it, replacing {player} with his name
	 *
	 * @param sender
	 * @param command
	 */
	public static void dispatchCommandAsPlayer(@NonNull final Audience sender, @NonNull String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		// Remove trailing /
		if (command.startsWith("/") && !command.startsWith("//"))
			command = command.substring(1);

		checkBlockedCommands(sender, command);

		final String finalCommand = command;

		Platform.runTask(0, () -> Platform.dispatchCommand(sender, colorizeLegacy(finalCommand.replace("{player}", Platform.resolveSenderName(sender)))));
	}

	/*
	 * A pitiful attempt at blocking a few known commands which might be used for malicious intent.
	 * We log the attempt to a file for manual review.
	 */
	private static boolean checkBlockedCommands(Audience sender, String command) {
		if (command.startsWith("op ") || command.startsWith("minecraft:op ")) {
			final String errorMessage = Platform.resolveSenderName(sender) + " tried to run blocked command: " + command;
			FileUtil.writeFormatted("blocked-commands.log", errorMessage);

			throw new FoException(errorMessage);
		}

		return false;
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
	 * A dummy helper method adding "&cWarning: &f" to the given message
	 * and logging it.
	 *
	 * @param message
	 */
	public static void warning(String message) {
		log("&cWarning: &7" + message);
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
			if (message == null || "none".equals(message))
				continue;

			if (message.replace(" ", "").isEmpty()) {
				Platform.logToConsole("  ");

				continue;
			}

			if (message.startsWith("[JSON]")) {
				final String stripped = message.replaceFirst("\\[JSON\\]", "").trim();

				if (!stripped.isEmpty()) {
					final Component component = RemainCore.convertJsonToAdventure(stripped);

					log(RemainCore.convertAdventureToLegacy(component));
				}

			} else {
				message = colorizeLegacy(message);

				for (final String part : message.split("\n")) {
					final String log = (addLogPrefix && !logPrefix.isEmpty() ? logPrefix + " " : "") + getOrEmpty(part);

					Platform.logToConsole(log);
				}
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
		if (messages != null && !ValidCore.isNullOrEmpty(messages)) {
			log("&7" + consoleLine());
			for (final String msg : messages)
				log(" &c" + msg);

			if (disablePlugin)
				log(" &cPlugin is now disabled.");

			log("&7" + consoleLine());
		}

		if (disablePlugin)
			Platform.disablePlugin();
	}

	/**
	 * Saves the error, prints the stack trace and logs it in frame.
	 * Possible to use %error variable
	 *
	 * @param throwable
	 * @param messages
	 */
	public static void error(@NonNull Throwable throwable, String... messages) {

		if (throwable instanceof InvocationTargetException && throwable.getCause() != null)
			throwable = throwable.getCause();

		if (!(throwable instanceof FoException))
			Debugger.saveError(throwable, messages);

		Debugger.printStackTrace(throwable);
		logFramed(replaceErrorVariable(throwable, messages));
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

		// Delegate to only print out the relevant stuff
		if (t instanceof FoException)
			throw (FoException) t;

		if (messages != null)
			logFramed(false, replaceErrorVariable(t, messages));

		Debugger.saveError(t, messages);

		t.printStackTrace();

		RemainCore.sneaky(t);
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
	 * Compiles a pattern from the given regex, stripping colors and making
	 * it case insensitive
	 *
	 * @param regex
	 * @return
	 */
	public static Pattern compilePattern(String regex) {
		regex = Platform.isRegexStrippingColors() ? CommonCore.removeColors(regex) : regex;
		regex = Platform.isRegexStrippingAccents() ? ChatUtil.replaceDiacritic(regex) : regex;

		if (Platform.isRegexCaseInsensitive())
			return Pattern.compile(regex, Platform.isRegexUnicode() ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : Pattern.CASE_INSENSITIVE);

		else
			return Platform.isRegexUnicode() ? Pattern.compile(regex, Pattern.UNICODE_CASE) : Pattern.compile(regex);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Joining strings and lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Joins an array of lists together into one big array
	 *
	 * @param <T>
	 * @param arrays
	 * @return
	 */
	@SafeVarargs
	public static <T> Object[] joinArrays(final T[]... arrays) {
		final List<T> all = new ArrayList<>();

		for (final T[] array : arrays)
			for (final T element : array)
				all.add(element);

		return all.toArray();
	}

	/**
	 * Joins an array of lists together into one big list
	 *
	 * @param <T>
	 * @param arrays
	 * @return
	 */
	@SafeVarargs
	public static <T> List<T> joinLists(final Iterable<T>... arrays) {
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
	public static <T extends Audience> String joinPlayersExcept(final Iterable<T> array, final String nameToIgnore) {
		final Iterator<T> it = array.iterator();
		String message = "";

		while (it.hasNext()) {
			final T next = it.next();
			final String nextName = Platform.resolveSenderName(next);

			if (!nameToIgnore.equals(nextName))
				message += nextName + (it.hasNext() ? ", " : "");
		}

		return message.endsWith(", ") ? message.substring(0, message.length() - 2) : message;
	}

	/**
	 * A special method that will return all key names from the given enum. The enum
	 * must have "getKey()" method for every constant.
	 *
	 * Returns for example: "apple, banana, carrot" etc.
	 *
	 * @param <T>
	 * @param enumeration
	 * @return
	 */
	public static <T extends Enum<?>> String keys(Class<T> enumeration) {
		return CommonCore.join(enumeration.getEnumConstants(), (Stringer<T>) object -> ReflectionUtilCore.invoke("getKey", object));
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

		for (int i = start; i < MathUtilCore.range(stop, 0, array.length); i++)
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
	public static <T> String join(final T[] array, final String delimiter) {
		return join(array, delimiter, object -> object == null ? "" : simplify(object));
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
	 * Joins an array of a given type using the ", " delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param stringer
	 * @return
	 */
	public static <T> String join(final T[] array, final Stringer<T> stringer) {
		return join(array, ", ", stringer);
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
		ValidCore.checkNotNull(array, "Cannot join null array!");

		return join(Arrays.asList(array), delimiter, stringer);
	}

	/**
	 * Joins a list of a given type using the comma delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param stringer
	 * @return
	 */
	public static <T> String join(final Iterable<T> array, final Stringer<T> stringer) {
		return join(array, ", ", stringer);
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
		if (arg == null)
			return "";

		else if (arg.getClass() == double.class || arg.getClass() == float.class)
			return MathUtilCore.formatTwoDigits((double) arg);

		else if (arg instanceof Collection)
			return CommonCore.join((Collection<?>) arg, ", ", CommonCore::simplify);

		else if (arg instanceof CompChatColor)
			return ((CompChatColor) arg).getName();

		else if (arg instanceof Enum)
			return ((Enum<?>) arg).toString().toLowerCase();

		return arg.toString();
	}

	/**
	 * Dynamically populates pages, used for pagination in commands or menus
	 *
	 * @param <T>
	 * @param cellSize
	 * @param items
	 * @return
	 */
	public static <T> Map<Integer, List<T>> fillPages(int cellSize, Iterable<T> items) {
		final List<T> allItems = CommonCore.toList(items);

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

			// If the menu is completely empty, at least allow the first page
			if (i == 0 || !pageItems.isEmpty())
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

			output.append(word + " ");
			lineLen += word.length() + 1;
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
	 * Creates a new {@link HashMap} with a single key-value pair.
	 *
	 * @param <A>        the type of the key.
	 * @param <B>        the type of the value.
	 * @param firstKey   the key of the first entry.
	 * @param firstValue the value of the first entry.
	 * @param secondKey
	 * @param secondValue
	 * @return a new {@link HashMap} with the specified key-value pair.
	 */
	public static <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue, final A secondKey, final B secondValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);
		map.put(secondKey, secondValue);

		return map;
	}

	/**
	 * Creates a new {@link HashMap} with a single key-value pair.
	 *
	 * @param <A>        the type of the key.
	 * @param <B>        the type of the value.
	 * @param firstKey   the key of the first entry.
	 * @param firstValue the value of the first entry.
	 * @param secondKey
	 * @param secondValue
	 * @param thirdKey
	 * @param thirdValue
	 * @return a new {@link HashMap} with the specified key-value pair.
	 */
	public static <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue, final A secondKey, final B secondValue, final A thirdKey, final B thirdValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);
		map.put(secondKey, secondValue);
		map.put(thirdKey, thirdValue);

		return map;
	}

	/**
	 * Creates a new {@link HashMap} with a single key-value pair.
	 *
	 * @param <A>        the type of the key.
	 * @param <B>        the type of the value.
	 * @param firstKey   the key of the first entry.
	 * @param firstValue the value of the first entry.
	 * @param secondKey
	 * @param secondValue
	 * @param thirdKey
	 * @param thirdValue
	 * @param forthKey
	 * @param forthValue
	 * @return a new {@link HashMap} with the specified key-value pair.
	 */
	public static <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue, final A secondKey, final B secondValue, final A thirdKey, final B thirdValue, final A forthKey, final B forthValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);
		map.put(secondKey, secondValue);
		map.put(thirdKey, thirdValue);
		map.put(forthKey, forthValue);

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
	 * Create a new array list that is mutable (if you call Arrays.asList that is unmodifiable)
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static <T> List<T> newList(final T... keys) {
		final List<T> list = new ArrayList<>();

		Collections.addAll(list, keys);

		return list;
	}

	/**
	 * Return a map sorted by values (i.e. from smallest to highest for numbers)
	 *
	 * @param map
	 * @return
	 */
	public static Map<String, Integer> sortByValue(Map<String, Integer> map) {
		final List<Map.Entry<String, Integer>> list = new LinkedList<>(map.entrySet());
		list.sort(Map.Entry.comparingByValue());

		final Map<String, Integer> sortedMap = new LinkedHashMap<>();

		for (final Map.Entry<String, Integer> entry : list)
			sortedMap.put(entry.getKey(), entry.getValue());

		return sortedMap;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------------------------

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

	/**
	 * Compress the given string into a byte array
	 *
	 * @param data
	 * @return
	 */
	public static byte[] compress(String data) {
		try {
			final byte[] input = data.getBytes("UTF-8");
			final Deflater deflater = new Deflater();

			deflater.setInput(input);
			deflater.finish();

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length)) {
				final byte[] buffer = new byte[1024];

				while (!deflater.finished()) {
					final int count = deflater.deflate(buffer);

					outputStream.write(buffer, 0, count);
				}

				return outputStream.toByteArray();
			}

		} catch (final Exception ex) {
			CommonCore.throwError(ex, "Failed to compress data");

			return new byte[0];
		}
	}

	/**
	 * Decompress the given byte array into a string
	 *
	 * @param data
	 * @return
	 */
	public static String decompress(byte[] data) {
		final Inflater inflater = new Inflater();
		inflater.setInput(data);

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
			final byte[] buffer = new byte[1024];

			while (!inflater.finished()) {
				final int count = inflater.inflate(buffer);

				outputStream.write(buffer, 0, count);
			}

			return new String(outputStream.toByteArray(), "UTF-8");

		} catch (final Exception ex) {
			CommonCore.throwError(ex, "Failed to decompress data");

			return "";
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
	 * @param <Val>
	 */
	public interface MapToListConverter<O, K, Val> {

		/**
		 * Converts the given map key-value pair into a new type stored in a list
		 *
		 * @param key
		 * @param value
		 * @return
		 */
		O convert(K key, Val value);
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

	/**
	 * Represents a timed chat sequence, used when checking for
	 * regular expressions so we time how long it takes and
	 * stop the execution if takes too long
	 */
	public final static class TimedCharSequence implements CharSequence {

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
		 * this is called too late after the constructor.
		 */
		@Override
		public char charAt(final int index) {

			// Temporarily disabled due to a rare condition upstream when we take this message
			// and run it in a runnable, then this is still being evaluated past limit and it fails
			//
			//if (System.currentTimeMillis() > futureTimestampLimit)
			//	throw new RegexTimeoutException(message, futureTimestampLimit);

			try {
				return this.message.charAt(index);
			} catch (final StringIndexOutOfBoundsException ex) {

				// Odd case: Java 8 seems to overflow for too-long unicode characters, security feature
				return ' ';
			}
		}

		@Override
		public int length() {
			return this.message.length();
		}

		@Override
		public CharSequence subSequence(final int start, final int end) {
			return new TimedCharSequence(this.message.subSequence(start, end), this.futureTimestampLimit);
		}

		@Override
		public String toString() {
			return this.message.toString();
		}

		/**
		 * Compile a new char sequence with limit from settings.yml
		 *
		 * @param message
		 * @return
		 */
		public static TimedCharSequence withSettingsLimit(CharSequence message) {
			return new TimedCharSequence(message, System.currentTimeMillis() + SimpleSettings.REGEX_TIMEOUT);
		}
	}
}