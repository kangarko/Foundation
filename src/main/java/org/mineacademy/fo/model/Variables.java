package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.NonNull;
import net.kyori.adventure.text.Component;

/**
 * A simple engine that replaces variables in a message.
 */
public final class Variables {

	/**
	 * The pattern to find singular [syntax_name] variables.
	 */
	public static final Pattern MESSAGE_VARIABLE_PATTERN = Pattern.compile("[\\[]([^\\[\\]]+)[\\]]");

	/**
	 * The pattern to find simple {syntax} placeholders.
	 */
	public static final Pattern BRACKET_VARIABLE_PATTERN = Pattern.compile("[{]([^{}]+)[}]");

	/**
	 * The pattern to find simple {syntax} placeholders starting with {rel_} (used for PlaceholderAPI)
	 */
	public static final Pattern BRACKET_REL_VARIABLE_PATTERN = Pattern.compile("[({)](rel_)([^}]+)[(})]");

	/**
	 * Player - [Original Message - Translated Message]
	 */
	private static final Map<String, Map<String, String>> cache = ExpiringMap.builder().expiration(500, TimeUnit.MILLISECONDS).build();

	// ------------------------------------------------------------------------------------------------------------
	// Custom variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Variables added to Foundation by you or other plugins
	 *
	 * This is used to dynamically replace the variable based on its content, like
	 * PlaceholderAPI.
	 *
	 * We also hook into PlaceholderAPI, however, you'll have to use your plugin's prefix before
	 * all variables when called from there.
	 */
	private static final StrictList<SimpleExpansion> customExpansions = new StrictList<>();

	/**
	 * Return an immutable list of all currently loaded expansions
	 *
	 * @return
	 */
	public static List<SimpleExpansion> getExpansions() {
		return Collections.unmodifiableList(customExpansions.getSource());
	}

	/**
	 * Registers a new expansion if it was not already registered
	 *
	 * @param expansion
	 */
	public static void addExpansion(SimpleExpansion expansion) {
		customExpansions.addIfNotExist(expansion);
	}

	/**
	 * Unregisters an expansion if it was registered already
	 *
	 * @param expansion
	 */
	public static void removeExpansion(SimpleExpansion expansion) {
		customExpansions.remove(expansion);
	}

	/**
	 * Return true if the expansion has already been registered
	 *
	 * @param expansion
	 * @return
	 */
	public static boolean hasExpansion(SimpleExpansion expansion) {
		return customExpansions.contains(expansion);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacing
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MVdWPlaceholderAPI (only if sender is a Player).
	 *
	 * @param message
	 * @param sender
	 * @return
	 */
	public static String replace(String message, CommandSender sender) {
		return replace(message, sender, null);
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MVdWPlaceholderAPI (only if sender is a Player).
	 *
	 * @param message
	 * @param sender
	 * @param replacements
	 * @return
	 */
	public static String replace(String message, CommandSender sender, Map<String, Object> replacements) {
		return replace(message, sender, replacements, true);
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MVdWPlaceholderAPI (only if sender is a Player).
	 *
	 * @param message
	 * @param sender
	 * @param replacements
	 * @param colorize
	 * @return
	 */
	public static String replace(String message, CommandSender sender, Map<String, Object> replacements, boolean colorize) {
		return replace(message, sender, replacements, colorize, true);
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MVdWPlaceholderAPI (only if sender is a Player).
	 *
	 * @param message
	 * @param sender
	 * @param replacements
	 * @param colorize
	 * @param replaceScript
	 * @return
	 */
	public static String replace(String message, CommandSender sender, Map<String, Object> replacements, boolean colorize, boolean replaceScript) {
		if (message == null || message.isEmpty() || message.equals("none"))
			return "";

		final String original = message;
		final boolean senderIsPlayer = sender instanceof Player;

		if (senderIsPlayer) {

			// Already cached ? Return.
			final Map<String, String> cached = cache.get(sender.getName());
			final String cachedVar = cached != null ? cached.get(message) : null;

			if (cachedVar != null && !cachedVar.contains("flpm_") && !cachedVar.contains("flps_"))
				return cachedVar;
		}

		// Replace custom variables first
		if (replacements != null && !replacements.isEmpty())
			message = Replacer.replaceArray(message, replacements);

		// PlaceholderAPI and MVdWPlaceholderAPI
		// TODO implement below
		if (senderIsPlayer)
			message = HookManager.replacePlaceholders((Player) sender, message);

		// TODO implement below
		else if (sender instanceof DiscordSender)
			message = HookManager.replacePlaceholders(((DiscordSender) sender).getOfflinePlayer(), message);

		// Replace hard variables
		message = replaceHardVariables0(sender, message, Variables.BRACKET_VARIABLE_PATTERN.matcher(message));
		message = Messenger.replacePrefixes(message);

		// Custom placeholders
		if (replaceScript)
			message = replaceJavascriptVariables0(message, sender, replacements);

		if (!message.startsWith("[JSON]") && colorize)
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

	public static String replaceVariablesNew(String message, CommandSender sender) {
		final Matcher matcher = Variables.BRACKET_VARIABLE_PATTERN.matcher(message);
		final Map<String, Object> replacements = new HashMap<>();

		while (matcher.find()) {
			final String variable = matcher.group();
			final String value = replaceOneVariableNew(variable, sender, replacements);

			if (value != null)
				message = message.replace(variable, value);
		}

		return message;
	}

	public static Component replaceVariablesNew(Component message) {
		return replaceVariablesNew(message, null);
	}

	public static Component replaceVariablesNew(Component message, CommandSender sender) {
		return replaceVariablesNew(message, sender, new HashMap<>());
	}

	public static Component replaceVariablesNew(Component message, CommandSender sender, Map<String, Object> replacements) {
		return message.replaceText(b -> b.match(BRACKET_VARIABLE_PATTERN).replacement((result, input) -> {
			final String variable = result.group();
			final String value = replaceOneVariableNew(variable, sender, replacements);

			return value == null ? Component.empty() : Remain.convertLegacyToAdventure(value); // TODO add minimessage support
		}));
	}

	/**
	 * Draft API
	 *
	 * @param variable
	 * @param replacements
	 * @return
	 */
	public static String replaceOneVariableNew(String variable, @Nullable CommandSender sender, @NonNull Map<String, Object> replacements) {
		final Player player = sender instanceof Player ? (Player) sender : null;

		boolean frontSpace = false;
		boolean backSpace = false;

		if (variable.startsWith("{"))
			variable = variable.substring(1);

		if (variable.endsWith("}"))
			variable = variable.substring(0, variable.length() - 1);

		if (variable.startsWith("+")) {
			variable = variable.substring(1);

			frontSpace = true;
		}

		if (variable.endsWith("+")) {
			variable = variable.substring(0, variable.length() - 1);

			backSpace = true;
		}

		// Replace custom expansions
		if (sender != null && !HookManager.isPlaceholderAPILoaded()) // TODO test if it works with PAPI still
			for (final SimpleExpansion expansion : customExpansions) {
				final String value = expansion.replacePlaceholders(sender, variable);

				if (value != null)
					return value.isEmpty() ? "" : (frontSpace ? " " : "") + value + (backSpace ? " " : "");
			}

		GeoResponse geoResponse = null;

		if (sender instanceof Player && Arrays.asList("country_code", "country_name", "region_name", "isp").contains(variable))
			geoResponse = GeoAPI.getCountry(player.getAddress());

		replacements.put("prefix", SimpleSettings.PLUGIN_PREFIX);
		replacements.put("plugin_prefix", SimpleSettings.PLUGIN_PREFIX);
		replacements.put("info", Messenger.getInfoPrefix());
		replacements.put("info_prefix", Messenger.getInfoPrefix());
		replacements.put("prefix_info", Messenger.getInfoPrefix());
		replacements.put("success", Messenger.getSuccessPrefix());
		replacements.put("success_prefix", Messenger.getSuccessPrefix());
		replacements.put("prefix_success", Messenger.getSuccessPrefix());
		replacements.put("warn", Messenger.getWarnPrefix());
		replacements.put("warn_prefix", Messenger.getWarnPrefix());
		replacements.put("prefix_warn", Messenger.getWarnPrefix());
		replacements.put("error", Messenger.getErrorPrefix());
		replacements.put("error_prefix", Messenger.getErrorPrefix());
		replacements.put("prefix_error", Messenger.getErrorPrefix());
		replacements.put("question", Messenger.getQuestionPrefix());
		replacements.put("question_prefix", Messenger.getQuestionPrefix());
		replacements.put("prefix_question", Messenger.getQuestionPrefix());
		replacements.put("announce", Messenger.getAnnouncePrefix());
		replacements.put("announce_prefix", Messenger.getAnnouncePrefix());
		replacements.put("prefix_announce", Messenger.getAnnouncePrefix());
		replacements.put("server", Remain.getServerName());
		replacements.put("server_name", Remain.getServerName());
		replacements.put("server_version", MinecraftVersion.getFullVersion());
		replacements.put("nms_version", MinecraftVersion.getServerVersion());
		replacements.put("date", TimeUtil.getFormattedDate());
		replacements.put("date_short", TimeUtil.getFormattedDateShort());
		replacements.put("date_month", TimeUtil.getFormattedDateMonth());
		replacements.put("chat_line", Common.chatLine());
		replacements.put("chat_line_smooth", Common.chatLineSmooth());
		replacements.put("label", SimplePlugin.getInstance().getMainCommand() != null ? SimplePlugin.getInstance().getMainCommand().getLabel() : SimpleLocalization.NONE);

		replacements.put("player", Common.resolveSenderName(sender));
		replacements.put("player_name", Common.resolveSenderName(sender));
		replacements.put("town", player == null ? "" : HookManager.getTownName(player));
		replacements.put("nation", player == null ? "" : HookManager.getNation(player));
		replacements.put("faction", player == null ? "" : HookManager.getFaction(player));
		replacements.put("world", player == null ? "" : HookManager.getWorldAlias(player.getWorld()));
		replacements.put("health", player == null ? "" : formatHealth0(player) + ChatColor.RESET);
		replacements.put("location", player == null ? "" : Common.shortLocation(player.getLocation()));
		replacements.put("x", player == null ? "" : String.valueOf(player.getLocation().getBlockX()));
		replacements.put("y", player == null ? "" : String.valueOf(player.getLocation().getBlockY()));
		replacements.put("z", player == null ? "" : String.valueOf(player.getLocation().getBlockZ()));
		replacements.put("tab_name", player == null ? Common.resolveSenderName(sender) : player.getPlayerListName());
		replacements.put("display_name", player == null ? Common.resolveSenderName(sender) : player.getDisplayName());
		replacements.put("player_nick", player == null ? Common.resolveSenderName(sender) : HookManager.getNickColored(player));
		replacements.put("nick", player == null ? Common.resolveSenderName(sender) : HookManager.getNickColored(player));
		replacements.put("player_prefix", player == null ? "" : HookManager.getPlayerPrefix(player));
		replacements.put("player_suffix", player == null ? "" : HookManager.getPlayerSuffix(player));
		replacements.put("player_group", player == null ? "" : HookManager.getPlayerPermissionGroup(player));
		replacements.put("player_primary_group", player == null ? "" : HookManager.getPlayerPrimaryGroup(player));
		replacements.put("ip_address", player == null ? "" : formatIp0(player));
		replacements.put("player_vanished", player == null ? "false" : String.valueOf(PlayerUtil.isVanished(player)));
		replacements.put("country_code", geoResponse == null ? "" : geoResponse.getCountryCode());
		replacements.put("country_name", geoResponse == null ? "" : geoResponse.getCountryName());
		replacements.put("region_name", geoResponse == null ? "" : geoResponse.getRegionName());
		replacements.put("isp", geoResponse == null ? "" : geoResponse.getIsp());
		replacements.put("sender_is_player", player != null ? "true" : "false");
		replacements.put("sender_is_discord", sender instanceof DiscordSender ? "true" : "false");
		replacements.put("sender_is_console", sender instanceof ConsoleCommandSender ? "true" : "false");

		// Replace JavaScript variables
		final Variable javascriptKey = Variable.findVariable(variable, Variable.Type.FORMAT);

		if (javascriptKey != null) {
			String value = javascriptKey.buildPlain(sender, replacements);

			// And we remove the white prefix that is by default added in every component
			if (value.startsWith(ChatColor.COLOR_CHAR + "f" + ChatColor.COLOR_CHAR + "f"))
				value = value.substring(4);

			replacements.put(variable, value);
		}

		// Replace PlaceholderAPI variables
		final Map<String, Object> placeholderApiHooks = HookManager.getPlaceholderAPIHooks();

		if (!placeholderApiHooks.isEmpty()) {
			final int index = variable.indexOf("_");

			if (!(index <= 0 || index >= variable.length())) {
				final String identifier = variable.substring(0, index).toLowerCase();
				final String params = variable.substring(index + 1);

				if (placeholderApiHooks.containsKey(identifier)) {
					final String value = HookManager.getPlaceholderAPIValue(placeholderApiHooks.get(identifier), identifier, player, params);

					if (value != null)
						replacements.put(variable, value);
				}
			}
		}

		// Replace variables
		for (final Map.Entry<String, Object> entry : replacements.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue() != null ? entry.getValue().toString() : "";

			Valid.checkBoolean(!key.startsWith("{"), "Variable key cannot start with {, found: " + key);
			Valid.checkBoolean(!key.endsWith("}"), "Variable key cannot end with }, found: " + key);

			if (key.equals(variable))
				return value.isEmpty() ? "" : (frontSpace ? " " : "") + value + (backSpace ? " " : "");
		}

		return null;
	}

	/*
	 * Replaces JavaScript variables in the message
	 */
	private static String replaceJavascriptVariables0(String message, CommandSender sender, Map<String, Object> replacements) {
		message = replaceJavascriptVariables0(message, sender, replacements, BRACKET_VARIABLE_PATTERN.matcher(message));

		return message;
	}

	private static String replaceJavascriptVariables0(String message, CommandSender sender, Map<String, Object> replacements, Matcher matcher) {
		while (matcher.find()) {
			final String variableKey = matcher.group();

			// Find the variable key without []
			final Variable variable = Variable.findVariable(variableKey.substring(1, variableKey.length() - 1));

			if (variable != null && variable.getType() == Variable.Type.FORMAT) {
				String plain = variable.buildPlain(sender, replacements);

				// And we remove the white prefix that is by default added in every component
				if (plain.startsWith(ChatColor.COLOR_CHAR + "f" + ChatColor.COLOR_CHAR + "f"))
					plain = plain.substring(4);

				message = message.replace(variableKey, plain);
			}
		}

		return message;
	}

	private static String replaceHardVariables0(CommandSender sender, String message, Matcher matcher) {
		final Player player = sender instanceof Player ? (Player) sender : null;

		while (matcher.find()) {
			String variable = matcher.group(1);
			boolean frontSpace = false;
			boolean backSpace = false;

			if (variable.startsWith("+")) {
				variable = variable.substring(1);

				frontSpace = true;
			}

			if (variable.endsWith("+")) {
				variable = variable.substring(0, variable.length() - 1);

				backSpace = true;
			}

			String value = lookupVariable0(player, sender, variable);

			if (value != null) {
				final boolean emptyColorless = Common.removeColors(value).isEmpty();
				value = value.isEmpty() ? "" : (frontSpace && !emptyColorless ? " " : "") + Common.colorize(value) + (backSpace && !emptyColorless ? " " : "");

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

		if (console != null)
			// Replace custom expansions
			for (final SimpleExpansion expansion : customExpansions) {
				final String value = expansion.replacePlaceholders(console, variable);

				if (value != null)
					return value;
			}

		switch (variable) {

			case "server":
			case "server_name":
				return Remain.getServerName();
			case "server_version":
				return MinecraftVersion.getFullVersion();
			case "nms_version":
				return MinecraftVersion.getServerVersion();
			case "date":
				return TimeUtil.getFormattedDate();
			case "date_short":
				return TimeUtil.getFormattedDateShort();
			case "date_month":
				return TimeUtil.getFormattedDateMonth();
			case "chat_line":
				return Common.chatLine();
			case "chat_line_smooth":
				return Common.chatLineSmooth();
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
			case "location":
				return player == null ? "" : Common.shortLocation(player.getLocation());
			case "x":
				return player == null ? "" : String.valueOf(player.getLocation().getBlockX());
			case "y":
				return player == null ? "" : String.valueOf(player.getLocation().getBlockY());
			case "z":
				return player == null ? "" : String.valueOf(player.getLocation().getBlockZ());

			case "player":
			case "player_name": {
				if (console == null)
					return null;

				return player == null ? Common.resolveSenderName(console) : player.getName();
			}

			case "tab_name":
				return player == null ? Common.resolveSenderName(console) : player.getPlayerListName();
			case "display_name":
				return player == null ? Common.resolveSenderName(console) : player.getDisplayName();
			case "player_nick":
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

			case "player_vanished":
				return player == null ? "false" : String.valueOf(PlayerUtil.isVanished(player));

			case "country_code":
				return player == null ? "" : geoResponse.getCountryCode();
			case "country_name":
				return player == null ? "" : geoResponse.getCountryName();
			case "region_name":
				return player == null ? "" : geoResponse.getRegionName();
			case "isp":
				return player == null ? "" : geoResponse.getIsp();

			case "label":
				return SimplePlugin.getInstance().getMainCommand() != null ? SimplePlugin.getInstance().getMainCommand().getLabel() : SimpleLocalization.NONE;
			case "sender_is_player":
				return player != null ? "true" : "false";
			case "sender_is_discord":
				return console instanceof DiscordSender ? "true" : "false";
			case "sender_is_console":
				return console instanceof ConsoleCommandSender ? "true" : "false";
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
