package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.GeoAPI;
import org.mineacademy.fo.GeoAPI.GeoResponse;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * A simple engine that replaces variables in a message.
 */
public final class Variables {

	/**
	 * The pattern to find simple {} placeholders
	 */
	public static final Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile("[({|%)]([^{}]+)[(}|%)]");

	/**
	 * The patter to find simple {} placeholders starting with {rel_ (used for PlaceholderAPI)
	 */
	public static final Pattern BRACKET_REL_PLACEHOLDER_PATTERN = Pattern.compile("[({|%)](rel_)([^}]+)[(}|%)]");

	/**
	 * Player - [Original Message - Translated Message]
	 */
	private static final Map<String, Map<String, String>> cache = ExpiringMap.builder().expiration(10, TimeUnit.MILLISECONDS).build();

	/**
	 * Should we replace javascript placeholders from variables/ folder automatically?
	 */
	public static boolean REPLACE_JAVASCRIPT = true;

	// ------------------------------------------------------------------------------------------------------------
	// Custom variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Variables added to Foundation by you or other plugins
	 * <p>
	 * You take in a command sender (may/may not be a player) and output a replaced string.
	 * The variable name (the key) is automatically surrounded by {} brackets
	 */
	private static final Map<String, Function<CommandSender, String>> customVariables = new HashMap<>();

	/**
	 * As a developer you can add or remove custom variables. Return an unmodifiable
	 * set of all added custom variables
	 *
	 * @return
	 */
	public static Set<String> getVariables() {
		return Collections.unmodifiableSet(customVariables.keySet());
	}

	/**
	 * Register a new variable. The variable will be found inside {} block so if you give the variable
	 * name player_health it will be {player_health}. The function takes in a command sender (can be player)
	 * and outputs the variable value.
	 * <p>
	 * Please keep in mind we replace your variables AFTER PlaceholderAPI and Javascript variables
	 *
	 * @param variable
	 * @param replacer
	 */
	public static void addVariable(String variable, Function<CommandSender, String> replacer) {
		customVariables.put(variable, replacer);
	}

	/**
	 * Removes an existing variable, only put the name here without brackets, e.g. player_name not {player_name}
	 * This fails when the variables does not exist
	 *
	 * @param variable
	 */
	public static void removeVariable(String variable) {
		customVariables.remove(variable);
	}

	/**
	 * Checks if the given variable exist. Warning: only put the name here without brackets,
	 * e.g. player_name not {player_name}
	 *
	 * @param variable
	 * @return
	 */
	public static boolean hasVariable(String variable) {
		return customVariables.containsKey(variable);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacing
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @deprecated, use {@link #replace(String, CommandSender)} as it will work the same
	 */
	@Deprecated
	public static String replace(boolean replaceCustom, String message, CommandSender sender) {
		return replace(message, sender);
	}

	/**
	 * Replaces variables in the messages using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI (only if sender is a Player).
	 *
	 * @param messages
	 * @param sender
	 * @return
	 */
	public static List<String> replace(Iterable<String> messages, @Nullable CommandSender sender) {

		// Trick: Join the lines to only parse variables at once -- performance++ -- then split again
		final String deliminer = "%FLVJ%";

		return Arrays.asList(replace(String.join(deliminer, messages), sender, null).split(deliminer));
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI (only if sender is a Player).
	 *
	 * @param message
	 * @param sender
	 * @return
	 */
	public static String replace(String message, @Nullable CommandSender sender) {
		return replace(message, sender, null);
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI (only if sender is a Player).
	 *
	 * @param message
	 * @param sender
	 * @return
	 */
	public static String replace(String message, @Nullable CommandSender sender, @Nullable Map<String, Object> replacements) {
		if (message == null || message.isEmpty())
			return "";

		final String original = message;
		final boolean senderIsPlayer = sender instanceof Player;

		// Replace custom variables first
		if (replacements != null && !replacements.isEmpty())
			message = Replacer.replaceArray(message, replacements);

		if (senderIsPlayer) {
			// Already cached ? Return.
			final Map<String, String> cached = cache.get(sender.getName());
			final String cachedVar = cached != null ? cached.get(message) : null;

			if (cachedVar != null)
				return cachedVar;

			// Custom placeholders
			if (REPLACE_JAVASCRIPT)
				message = replaceJavascriptVariables0(message, (Player) sender);

			// PlaceholderAPI and MvdvPlaceholderAPI
			message = HookManager.replacePlaceholders((Player) sender, message);
		}

		// Default
		message = replaceHardVariables0(sender, message);

		// Support the & color system
		message = Common.colorize(message);

		if (senderIsPlayer) {
			final Map<String, String> map = cache.get(sender.getName());

			if (map != null)
				map.put(original, message);
			else
				cache.put(sender.getName(), Common.newHashMap(original, message));
		}

		return message;
	}

	/*
	 * Replaces JavaScript variables in the message
	 */
	private static String replaceJavascriptVariables0(String message, Player player) {

		for (final Variable variable : Variable.getVariables()) {
			final String key = variable.getKey();

			if (message.contains(key))
				try {
					message = message.replace(key, variable.getValue(player));

				} catch (final Throwable t) {
					Common.throwError(t,
							"Failed to replace a custom variable!",
							"Message: " + message,
							"Variable: " + key,
							"%error");
				}
		}

		return message;
	}

	/*
	 * Replaces our hardcoded variables in the message, using a cache for better performance
	 */
	private static String replaceHardVariables0(@Nullable CommandSender sender, String message) {
		final Matcher matcher = Variables.BRACKET_PLACEHOLDER_PATTERN.matcher(message);
		final Player player = sender instanceof Player ? (Player) sender : null;

		while (matcher.find()) {
			String variable = matcher.group(1);
			boolean addSpace = false;

			if (variable.endsWith("+")) {
				variable = variable.substring(0, variable.length() - 1);

				addSpace = true;
			}

			String value = lookupVariable0(player, sender, variable);

			if (value != null) {
				value = value.isEmpty() ? "" : Common.colorize(value) + (addSpace ? " " : "");

				message = message.replace(matcher.group(), value);
			}
		}

		return message;
	}

	/*
	 * Replaces the given variable with a few hardcoded within the plugin, see below
	 */
	private static String lookupVariable0(Player player, CommandSender console, String variable) {
		GeoResponse geoResponse = null;

		if (player != null && Arrays.asList("country_code", "country_name", "region_name", "isp").contains(variable))
			geoResponse = GeoAPI.getCountry(player.getAddress());

		{ // Replace custom variables
			final Function<CommandSender, String> customReplacer = customVariables.get(variable);

			if (customReplacer != null)
				return customReplacer.apply(console);
		}

		switch (variable) {
			case "server_name":
				return SimpleSettings.SERVER_NAME;
			case "nms_version":
				return MinecraftVersion.getServerVersion();
			case "timestamp":
				return TimeUtil.getFormattedDate();

			case "town":
				return player == null ? "" : HookManager.getTownName(player);
			case "nation":
				return player == null ? "" : HookManager.getNation(player);
			case "faction":
				return player == null ? "" : HookManager.getFaction(player);

			case "world":
				return player == null ? "" : HookManager.getWorldAlias(player.getWorld());
			case "health":
				return player == null ? "" : formatHealth0(player) + ChatColor.RESET;

			case "player":
			case "player_name":
				return player == null ? Common.resolveSenderName(console) : player.getName();
			case "tab_name":
				return player == null ? Common.resolveSenderName(console) : player.getPlayerListName();
			case "display_name":
				return player == null ? Common.resolveSenderName(console) : player.getDisplayName();
			case "nick":
				return player == null ? Common.resolveSenderName(console) : HookManager.getNickColored(player);

			case "player_prefix":
			case "pl_prefix":
				return player == null ? "" : HookManager.getPlayerPrefix(player);
			case "player_suffix":
			case "pl_suffix":
				return player == null ? "" : HookManager.getPlayerSuffix(player);
			case "player_group":
			case "pl_group":
				return player == null ? "" : HookManager.getPlayerPermissionGroup(player);
			case "player_primary_group":
			case "pl_primary_group":
				return player == null ? "" : HookManager.getPlayerPrimaryGroup(player);
			case "ip_address":
			case "pl_address":
				return player == null ? "" : formatIp0(player);

			case "country_code":
				return player == null ? "" : geoResponse.getCountryCode();
			case "country_name":
				return player == null ? "" : geoResponse.getCountryName();
			case "region_name":
				return player == null ? "" : geoResponse.getRegionName();
			case "isp":
				return player == null ? "" : geoResponse.getIsp();

			case "label":
				return SimplePlugin.getInstance().getMainCommand() != null ? SimplePlugin.getInstance().getMainCommand().getLabel() : "noMainCommandLabel";
			case "sender_is_player":
				return player != null ? "true" : "false";
			case "sender_is_discord":
				return console instanceof DiscordSender ? "true" : "false";
			case "sender_is_console":
				return console instanceof ConsoleCommandSender ? "true" : "false";

			case "info_prefix":
			case "prefix_info":
				return org.mineacademy.fo.Messenger.getInfoPrefix();
			case "success_prefix":
			case "prefix_success":
				return org.mineacademy.fo.Messenger.getSuccessPrefix();
			case "warn_prefix":
			case "prefix_warn":
				return org.mineacademy.fo.Messenger.getWarnPrefix();
			case "error_prefix":
			case "prefix_error":
				return org.mineacademy.fo.Messenger.getErrorPrefix();
			case "question_prefix":
			case "prefix_question":
				return org.mineacademy.fo.Messenger.getQuestionPrefix();
			case "announce_prefix":
			case "prefix_announce":
				return org.mineacademy.fo.Messenger.getAnnouncePrefix();
		}

		return null;
	}

	/*
	 * Formats the {health} variable
	 */
	private static String formatHealth0(Player player) {
		final int hp = Remain.getHealth(player);

		return (hp > 10 ? ChatColor.DARK_GREEN : hp > 5 ? ChatColor.GOLD : ChatColor.RED) + "" + hp;
	}

	/*
	 * Formats the IP address variable for the player
	 */
	private static String formatIp0(Player player) {
		try {
			return player.getAddress().toString().split("\\:")[0];
		} catch (final Throwable t) {
			return player.getAddress() != null ? player.getAddress().toString() : "";
		}
	}
}