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

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.conversations.Conversable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleTask;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.ConfigSection;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Our main utility class hosting a large variety of different convenience functions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Common {

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
	private static final Map<String, Long> TIMED_TELL_CACHE = new HashMap<>();

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
	private static String tellPrefix = "";

	/**
	 * The log prefix applied on log() methods, defaults to [PluginName]
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
		tellPrefix = prefix == null ? "" : prefix;
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
	public static void broadcast(final String message, final CommandSender playerReplacement) {
		broadcast(message, resolveSenderName(playerReplacement));
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
	 * @param messages
	 */
	public static void broadcast(final String... messages) {
		if (messages != null)
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
		for (final CommandSender recipient : recipients)
			tell(recipient, messages);
	}

	/**
	 * Broadcast the message to everyone with permission
	 *
	 * @param showPermission
	 * @param message
	 * @param log
	 */
	public static void broadcastWithPerm(final String showPermission, final String message, final boolean log) {
		if (message != null) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, showPermission))
					tellJson(online, message);

			if (log)
				log(message);
		}
	}

	/**
	 * Broadcast the message to everyone with permission without {@link #getTellPrefix()} and {@link #getLogPrefix()}
	 *
	 * @param showPermission
	 * @param message
	 * @param log
	 */
	public static void broadcastWithPermNoPrefix(final String showPermission, final String message, final boolean log) {
		if (message != null) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, showPermission))
					tellNoPrefix(online, message);

			if (log)
				logNoPrefix(message);
		}
	}

	/**
	 * Broadcast the text component message to everyone with permission
	 *
	 * @param permission
	 * @param message
	 * @param log
	 */
	public static void broadcastWithPerm(final String permission, @NonNull final Component message, final boolean log) {
		final String legacy = Remain.convertAdventureToLegacy(message);

		if (!legacy.equals("none")) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, permission))
					Remain.toAudience(online).sendMessage(message);

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
	public static void tellTimedNoPrefix(final int delaySeconds, final CommandSender sender, final String message) {
		final String oldPrefix = new String(tellPrefix);
		tellPrefix = "";

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
		final String prefix = message.contains(tellPrefix) || tellPrefix.isEmpty() ? "" : tellPrefix + " ";

		conversable.sendRawMessage(colorize(prefix + message));
	}

	/**
	 * Sends the conversable a message later
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	public static void tellLaterConversingNoPrefix(final int delayTicks, final Conversable conversable, final String message) {
		runLater(delayTicks, () -> tellConversingNoPrefix(conversable, message));
	}

	/**
	 * Sends the conversable player a colorized message
	 *
	 * @param conversable
	 * @param message
	 */
	public static void tellConversingNoPrefix(final Conversable conversable, final String message) {
		conversable.sendRawMessage(colorize(message));
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
	 * Sends the sender a bunch of messages, colors & are supported
	 * without {@link #getTellPrefix()} prefix
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tellNoPrefix(final CommandSender sender, final Collection<String> messages) {
		tellNoPrefix(sender, Common.toArray(messages));
	}

	/**
	 * Sends the sender a bunch of messages, colors & are supported
	 * without {@link #getTellPrefix()} prefix
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tellNoPrefix(final CommandSender sender, final String... messages) {
		final String oldPrefix = new String(tellPrefix);

		tellPrefix = "";
		tell(sender, messages);
		tellPrefix = oldPrefix;
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
		if (message == null || message.isEmpty() || "none".equals(message))
			return;

		// Has prefix already? This is replaced when colorizing
		final boolean hasPrefix = message.contains("{prefix}");
		final boolean hasJSON = message.startsWith("[JSON]");

		// Replace player
		message = message.replace("{player}", resolveSenderName(sender));

		// Replace colors
		if (!hasJSON)
			message = colorize(message);

		// Send [JSON] prefixed messages as json component
		if (hasJSON) {
			final String stripped = message.replace("[JSON]", "").trim();

			if (!stripped.isEmpty())
				Remain.sendJson(sender, stripped);

		} else if (message.startsWith("<actionbar>")) {
			final String stripped = message.replace("<actionbar>", "");

			if (!stripped.isEmpty())
				if (sender instanceof Player)
					Remain.sendActionBar(sender, stripped);
				else
					tellJson(sender, stripped);

		} else if (message.startsWith("<toast>")) {
			final String stripped = message.replace("<toast>", "");

			if (!stripped.isEmpty())
				if (sender instanceof Player)
					Remain.sendToast((Player) sender, stripped);
				else
					tellJson(sender, stripped);

		} else if (message.startsWith("<title>")) {
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

		} else if (message.startsWith("<bossbar>")) {
			final String stripped = message.replace("<bossbar>", "");

			if (!stripped.isEmpty())
				if (sender instanceof Player)
					// cannot provide time here so we show it for 10 seconds
					Remain.sendBossbarTimed((Player) sender, stripped, 10, 1F);
				else
					tellJson(sender, stripped);

		} else
			for (final String part : message.split("\n")) {
				final String prefix = !hasPrefix && !tellPrefix.isEmpty() ? tellPrefix + " " : "";
				final String toSend = colorize(part.startsWith("<center>") ? ChatUtil.center(prefix + part.replaceFirst("\\<center\\>(\\s|)", "")) : prefix + part);

				// Make player engaged in a server conversation still receive the message
				if (sender instanceof Conversable && ((Conversable) sender).isConversing())
					((Conversable) sender).sendRawMessage(toSend);

				else
					sender.sendMessage(toSend);
			}
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

		final String plainMessage = Remain.convertAdventureToPlain(message);

		if (plainMessage.isEmpty() || "none".equals(plainMessage))
			return;

		final boolean hasPrefix = plainMessage.contains("{prefix}");

		// Replace some variables
		message = message.replaceText(b -> b.matchLiteral("{player}").replacement(Remain.convertLegacyToAdventure(resolveSenderName(audience))));
		message = message.replaceText(b -> b.matchLiteral("{prefix}").replacement(Remain.convertLegacyToAdventure(SimpleSettings.PLUGIN_PREFIX)));

		if (plainMessage.startsWith("<actionbar>")) {
			final String stripped = plainMessage.replace("<actionbar>", "").trim();

			if (!stripped.isEmpty())
				Remain.sendActionBar(audience, message.replaceText(b -> b.matchLiteral("<actionbar>").replacement("")));

		} else if (plainMessage.startsWith("<toast>")) {
			final String stripped = plainMessage.replace("<toast>", "").trim();

			if (!stripped.isEmpty())
				if (audience instanceof Player)
					Remain.sendToast((Player) audience, stripped);
				else
					Remain.tell(audience, message.replaceText(b -> b.matchLiteral("<toast>").replacement("")));

		} else if (plainMessage.startsWith("<title>")) {
			final String stripped = Remain.convertAdventureToLegacy(message).replace("<title>", "").trim();

			if (!stripped.isEmpty()) {
				final String[] split = stripped.split("\\|");
				final String title = split[0];
				final String subtitle = split.length > 1 ? Common.joinRange(1, split) : null;

				Remain.sendTitle(audience, 0, 60, 0, Remain.convertLegacyToAdventure(title), Remain.convertLegacyToAdventure(subtitle));
			}

		} else if (plainMessage.startsWith("<bossbar>")) {
			final String stripped = plainMessage.replace("<bossbar>", "").trim();

			if (!stripped.isEmpty())
				Remain.sendBossbarTimed(audience, message.replaceText(b -> b.matchLiteral("<bossbar>").replacement("")), 10, 1F);

		} else {
			final String prefix = !hasPrefix && !tellPrefix.isEmpty() ? tellPrefix + " " : "";
			String legacyMessage = prefix + Remain.convertAdventureToLegacy(message);

			if (plainMessage.startsWith("<center>")) {
				legacyMessage = ChatUtil.center(legacyMessage.replace("\\<center\\>(\\s|)", ""));

				if (audience instanceof Conversable && ((Conversable) audience).isConversing())
					((Conversable) audience).sendRawMessage(colorize(legacyMessage));
				else
					audience.sendMessage(Remain.convertLegacyToAdventure(colorize(legacyMessage)));

			} else {
				if (audience instanceof Conversable && ((Conversable) audience).isConversing())
					((Conversable) audience).sendRawMessage(Remain.convertAdventureToLegacy(message));

				else
					audience.sendMessage(Remain.convertLegacyToAdventure(colorize(legacyMessage)));
			}
		}
	}

	/**
	 * Return the sender's name if it's a player or discord sender, or simply {@link SimpleLocalization#CONSOLE_NAME} if it is a console
	 *
	 * @param sender
	 * @return
	 */
	public static String resolveSenderName(final CommandSender sender) {
		return sender instanceof ConsoleCommandSender ? SimpleLocalization.CONSOLE_NAME : sender != null ? sender.getName() : "";
	}

	/**
	 * Return the sender's name if it's a player or discord sender, or simply {@link SimpleLocalization#CONSOLE_NAME} if it is a console
	 *
	 * @param sender
	 * @return
	 */
	public static String resolveSenderName(final Audience sender) {
		return sender instanceof ConsoleCommandSender ? SimpleLocalization.CONSOLE_NAME : sender != null ? sender instanceof CommandSender ? ((CommandSender) sender).getName() : "" : "";
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
	 * Replace the & letter with the {@link CompChatColor#COLOR_CHAR} in the message.
	 *
	 * @param messages the messages to replace color codes with '&'
	 * @return the colored message
	 */
	public static String colorize(final String... messages) {
		return colorize(String.join("\n", messages));
	}

	/**
	 * Replace the & letter with the {@link CompChatColor#COLOR_CHAR} in the message.
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
	 * Replaces & color codes and MiniMessage tags in the message.
	 * Also replaces {prefix}, {plugin_name} and {plugin_version} with their respective values.
	 *
	 * @param message
	 * @return
	 */
	public static String colorize(final String message) {
		return Remain.convertAdventureToLegacy(colorizeLegacy(message));
	}

	/**
	 * Replaces & color codes and MiniMessage tags in the message.
	 * Also replaces {prefix}, {plugin_name} and {plugin_version} with their respective values.
	 *
	 * @param message
	 * @return
	 */
	public static Component colorizeLegacy(String message) {
		if (message == null || message.isEmpty())
			return Component.empty();

		Component component = colorize0(message);

		component = component.replaceText(TextReplacementConfig.builder().matchLiteral("{prefix}").replacement(colorize0(message.startsWith(tellPrefix) ? "" : tellPrefix)).build());
		component = component.replaceText(TextReplacementConfig.builder().matchLiteral("{plugin_name}").replacement(SimplePlugin.getNamed()).build());
		component = component.replaceText(TextReplacementConfig.builder().matchLiteral("{plugin_version}").replacement(SimplePlugin.getVersion()).build());

		return component;
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

			Remain.sneaky(t);
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

		final Component component = Common.colorizeLegacy(message);

		return Remain.convertAdventureToPlain(component);
	}

	/**
	 * Returns if the message contains & or § color codes, or MiniMessage tags.
	 *
	 * @param message
	 * @return
	 */
	public static boolean hasColorTags(final String message) {
		final Component component = Common.colorizeLegacy(message);
		final String legacy = Remain.convertAdventureToLegacy(component).toLowerCase();

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
	 * Formats the vector location to one digit decimal points
	 *
	 * DO NOT USE FOR SAVING, ONLY INTENDED FOR DEBUGGING
	 * Use {@link SerializeUtil#serialize(Object)} to save a vector
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
	 * DO NOT USE FOR SAVING, ONLY INTENDED FOR DEBUGGING
	 * Use {@link SerializeUtil#serialize(Object)} to save a location
	 *
	 * @param location
	 * @return
	 */
	public static String shortLocation(final Location location) {
		if (location == null)
			return "Location(null)";

		if (location.equals(new Location(null, 0, 0, 0)))
			return "Location(null, 0, 0, 0)";

		Valid.checkNotNull(location.getWorld(), "Cannot shorten a location with null world!");

		return Replacer.replaceArray(SimpleSettings.LOCATION_FORMAT,
				"world", location.getWorld().getName(),
				"x", location.getBlockX(),
				"y", location.getBlockY(),
				"z", location.getBlockZ());
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
			if (otherPlugin.getDescription().getName().equals(pluginName)) {
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
	 * Runs the given command (without /) as the console, replacing {player} with sender
	 *
	 * You can prefix the command with @(announce|warn|error|info|question|success) to send a formatted
	 * message to playerReplacement directly.
	 *
	 * @param playerReplacement
	 * @param command
	 */
	public static void dispatchCommand(@Nullable CommandSender playerReplacement, @NonNull String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		if (command.startsWith("@announce ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @announce without a player in: " + command);

			Messenger.announce(playerReplacement, command.replace("@announce ", ""));
		}

		else if (command.startsWith("@warn ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @warn without a player in: " + command);

			Messenger.warn(playerReplacement, command.replace("@warn ", ""));
		}

		else if (command.startsWith("@error ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @error without a player in: " + command);

			Messenger.error(playerReplacement, command.replace("@error ", ""));
		}

		else if (command.startsWith("@info ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @info without a player in: " + command);

			Messenger.info(playerReplacement, command.replace("@info ", ""));
		}

		else if (command.startsWith("@question ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @question without a player in: " + command);

			Messenger.question(playerReplacement, command.replace("@question ", ""));
		}

		else if (command.startsWith("@success ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @success without a player in: " + command);

			Messenger.success(playerReplacement, command.replace("@success ", ""));
		}

		else {
			command = command.startsWith("/") && !command.startsWith("//") ? command.substring(1) : command;
			command = command.replace("{player}", playerReplacement == null ? "" : resolveSenderName(playerReplacement));

			// Workaround for JSON in tellraw getting HEX colors replaced
			if (!command.startsWith("tellraw"))
				command = colorize(command);

			checkBlockedCommands(playerReplacement, command);

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

		// Remove trailing /
		if (command.startsWith("/") && !command.startsWith("//"))
			command = command.substring(1);

		checkBlockedCommands(playerSender, command);

		final String finalCommand = command;

		runLater(() -> playerSender.performCommand(colorize(finalCommand.replace("{player}", resolveSenderName(playerSender)))));
	}

	/*
	 * A pitiful attempt at blocking a few known commands which might be used for malicious intent.
	 * We log the attempt to a file for manual review.
	 */
	private static boolean checkBlockedCommands(@Nullable CommandSender sender, String command) {
		if (command.startsWith("op ") || command.startsWith("minecraft:op ")) {
			final String errorMessage = (sender != null ? sender.getName() : "Console") + " tried to run blocked command: " + command;
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

		final CommandSender console = Bukkit.getConsoleSender();
		Valid.checkNotNull(console, "Failed to initialize Console Sender, are you running Foundation under a Bukkit/Spigot server?");

		for (String message : messages) {
			if (message == null || "none".equals(message))
				continue;

			if (message.replace(" ", "").isEmpty()) {
				console.sendMessage("  ");

				continue;
			}

			message = colorize(message);

			if (message.startsWith("[JSON]")) {
				final String stripped = message.replaceFirst("\\[JSON\\]", "").trim();

				if (!stripped.isEmpty()) {
					final Component component = Remain.convertJsonToAdventure(stripped);

					log(Remain.convertAdventureToLegacy(component));
				}

			} else
				for (final String part : message.split("\n")) {
					final String log = (addLogPrefix && !logPrefix.isEmpty() ? logPrefix + " " : "") + getOrEmpty(part);

					console.sendMessage(log);
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
	 * Compiles a matches for the given pattern and message. Colors are stripped.
	 * <p>
	 * We also evaluate how long the evaluation took and stop it in case it takes too long,
	 * see {@link SimplePlugin#getRegexTimeout()}
	 *
	 * @param pattern
	 * @param message
	 * @return
	 */
	/*public static Matcher compileMatcher(@NonNull final Pattern pattern, final String message) {

		try {
			final SimplePlugin instance = SimplePlugin.getInstance();

			String strippedMessage = instance.regexStripColors() ? stripColors(message) : message;
			strippedMessage = instance.regexStripAccents() ? ChatUtil.replaceDiacritic(strippedMessage) : strippedMessage;

			return pattern.matcher(TimedCharSequence.withSettingsLimit(strippedMessage));

		} catch (final RegexTimeoutException ex) {
			handleRegexTimeoutException(ex, pattern);

			return null;
		}
	}*/

	/**
	 * Compiles a matcher for the given regex and message
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	/*public static Matcher compileMatcher(final String regex, final String message) {
		return compileMatcher(compilePattern(regex), message);
	}*/

	/**
	 * Compiles a pattern from the given regex, stripping colors and making
	 * it case insensitive
	 *
	 * @param regex
	 * @return
	 */
	public static Pattern compilePattern(String regex) {
		final SimplePlugin instance = SimplePlugin.getInstance();

		regex = instance.regexStripColors() ? Common.removeColors(regex) : regex;
		regex = instance.regexStripAccents() ? ChatUtil.replaceDiacritic(regex) : regex;

		if (instance.regexCaseInsensitive())
			return Pattern.compile(regex, instance.regexUnicode() ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : Pattern.CASE_INSENSITIVE);

		else
			return instance.regexUnicode() ? Pattern.compile(regex, Pattern.UNICODE_CASE) : Pattern.compile(regex);
	}

	/**
	 * A special call handling regex timeout exception, do not use
	 *
	 * @param ex
	 * @param pattern
	 */
	/*public static void handleRegexTimeoutException(RegexTimeoutException ex, Pattern pattern) {
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
	}*/

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
		return Common.join(enumeration.getEnumConstants(), (Stringer<T>) object -> ReflectionUtil.invoke("getKey", object));
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
		Valid.checkNotNull(array, "Cannot join null array!");

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

		else if (arg instanceof Entity)
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
	 * Convenience method for getting a list of world names
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
	// Scheduling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Runs the task if the plugin is enabled correctly
	 *
	 * @param task the task
	 * @return the task or null
	 */
	public static SimpleTask runLater(final Runnable task) {
		return runLater(1, task);
	}

	/**
	 * Runs the task even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return the task or null
	 */
	public static SimpleTask runLater(final int delayTicks, Runnable runnable) {
		return Remain.runLater(delayTicks, runnable);
	}

	/**
	 * Runs the task async even if the plugin is disabled for some reason.
	 * <p>
	 * Schedules the run on the next tick.
	 *
	 * @param task
	 * @return
	 */
	public static SimpleTask runAsync(final Runnable task) {
		return runLaterAsync(0, task);
	}

	/**
	 * Runs the task async even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return the task or null
	 */
	public static SimpleTask runLaterAsync(final int delayTicks, Runnable runnable) {
		return Remain.runLaterAsync(delayTicks, runnable);
	}

	/**
	 * Runs the task timer even if the plugin is disabled.
	 *
	 * @param repeatTicks the delay between each execution
	 * @param task        the task
	 * @return the bukkit task or null
	 */
	public static SimpleTask runTimer(final int repeatTicks, final Runnable task) {
		return runTimer(0, repeatTicks, task);
	}

	/**
	 * Runs the task timer even if the plugin is disabled.
	 *
	 * @param delayTicks  the delay before first run
	 * @param repeatTicks the delay between each run
	 * @param runnable        the task
	 * @return the bukkit task or null if error
	 */
	public static SimpleTask runTimer(final int delayTicks, final int repeatTicks, Runnable runnable) {
		return Remain.runTimer(delayTicks, repeatTicks, runnable);
	}

	/**
	 * Runs the task timer async even if the plugin is disabled.
	 *
	 * @param repeatTicks
	 * @param task
	 * @return
	 */
	public static SimpleTask runTimerAsync(final int repeatTicks, final Runnable task) {
		return runTimerAsync(0, repeatTicks, task);
	}

	/**
	 * Runs the task timer async even if the plugin is disabled.
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param runnable
	 * @return
	 */
	public static SimpleTask runTimerAsync(final int delayTicks, final int repeatTicks, Runnable runnable) {
		return Remain.runTimerAsync(delayTicks, repeatTicks, runnable);
	}

	/**
	 * Attempts to cancel all tasks of this plugin
	 */
	public static void cancelTasks() {
		Remain.cancelTasks();
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
	public static Map<String, Object> getMapFromSection(@NonNull Object mapOrSection) {
		mapOrSection = Remain.getRootOfSectionPathData(mapOrSection);

		final Map<String, Object> map = mapOrSection instanceof ConfigSection ? ((ConfigSection) mapOrSection).getValues(false)
				: mapOrSection instanceof Map ? (Map<String, Object>) mapOrSection
						: mapOrSection instanceof MemorySection ? ReflectionUtil.getFieldContent(mapOrSection, "map") : null;

		Valid.checkNotNull(map, "Unexpected " + mapOrSection.getClass().getSimpleName() + " '" + mapOrSection + "'. Must be Map or MemorySection! (Do not just send config name here, but the actual section with get('section'))");

		final Map<String, Object> copy = new LinkedHashMap<>();

		for (final Map.Entry<String, Object> entry : map.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();

			copy.put(key, Remain.getRootOfSectionPathData(value));
		}

		return copy;
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
			Common.throwError(ex, "Failed to compress data");

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
			Common.throwError(ex, "Failed to decompress data");

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