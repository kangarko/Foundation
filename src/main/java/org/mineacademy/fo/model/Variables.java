package org.mineacademy.fo.model;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.GeoAPI;
import org.mineacademy.fo.GeoAPI.GeoResponse;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.model.Variable.VariableScope;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple engine that replaces lots of variables in a message.
 * <p>
 * Utilizes {@link FileReader}
 */
public final class Variables {

	/**
	 * A static flag indicating whether we should replace & color codes in the replace() methods below
	 */
	public static boolean REPLACE_COLORS = true;

	/**
	 * The pattern to find simple {} placeholders
	 */
	protected static final Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile("[{]([^{}]+)[}]");

	/**
	 * The patter to find simple {} placeholders starting with {rel_ (used for PlaceholderAPI)
	 */
	protected static final Pattern BRACKET_REL_PLACEHOLDER_PATTERN = Pattern.compile("[{](rel_)([^}]+)[}]");

	// ------------------------------------------------------------------------------------------------------------
	// Changing variables for loading
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Variables added to Foundation by you or other plugins
	 * <p>
	 * You take in a command sender (may/may not be a player) and output a replaced string.
	 * The variable name (the key) is automatically surrounded by {} brackets
	 */
	private static final StrictMap<String, Function<CommandSender, String>> customVariables = new StrictMap<>();

	/**
	 * Player, Their Cached Variables
	 */
	private static final StrictMap<String, Map<String, String>> cache = new StrictMap<>();

	/**
	 * Player, Original Message, Translated Message
	 */
	private static final Map<String, Map<String, String>> customCache = makeNewFastCache();

	// ------------------------------------------------------------------------------------------------------------
	// Custom variables
	// ------------------------------------------------------------------------------------------------------------

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
		return customVariables.contains(variable);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacing
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @param messages
	 * @param sender
	 * @return
	 * @see #replace(VariableScope, String, CommandSender)
	 */
	public static List<String> replaceAll(List<String> messages, CommandSender sender) {
		return replaceAll(VariableScope.FORMAT, messages, sender);
	}

	/**
	 * @param messages
	 * @param sender
	 * @return
	 * @see #replace(VariableScope, String, CommandSender)
	 */
	public static List<String> replaceAll(List<String> messages, CommandSender sender, Map<String, Object> replacements) {
		return replaceAll(VariableScope.FORMAT, messages, sender, replacements);
	}

	/**
	 * @param scope
	 * @param messages
	 * @param sender
	 * @return
	 * @see #replace(VariableScope, String, CommandSender)
	 */
	public static List<String> replaceAll(VariableScope scope, List<String> messages, CommandSender sender) {
		return Variables.replaceAll(scope, messages, sender, null);
	}

	/**
	 * @param scope
	 * @param messages
	 * @param sender
	 * @return
	 * @see #replace(VariableScope, String, CommandSender)
	 */
	public static List<String> replaceAll(VariableScope scope, List<String> messages, CommandSender sender, Map<String, Object> replacements) {
		final List<String> replaced = new ArrayList<>(messages); // Make a copy to ensure writeable

		for (int i = 0; i < replaced.size(); i++) {
			final String message = replaced.get(i);

			replaced.set(i, Variables.replace(scope, message, sender, replacements));
		}

		return replaced;
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 * <p>
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI.
	 *
	 * @param message
	 * @param sender
	 * @return
	 */
	public static String replace(String message, CommandSender sender) {
		return replace(VariableScope.FORMAT, message, sender);
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders. We also replace custom replacements from the Map
	 * <p>
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI.
	 *
	 * @param message
	 * @param sender
	 * @return
	 */
	public static String replace(String message, CommandSender sender, Map<String, Object> replacements) {
		return replace(VariableScope.FORMAT, message, sender, replacements);
	}

	/**
	 * @param replaceCustom
	 * @param message
	 * @param sender
	 * @return
	 * @deprecated replacing custom variables has been removed
	 */
	@Deprecated
	public static String replace(boolean replaceCustom, String message, CommandSender sender) {
		return replace(message, sender);
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 * <p>
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI (only if sender is a Player).
	 *
	 * @param scope
	 * @param message
	 * @param sender
	 * @return
	 */
	public static String replace(VariableScope scope, String message, CommandSender sender) {
		return replace(scope, message, sender, null);
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 * <p>
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI (only if sender is a Player).
	 *
	 * @param scope
	 * @param message
	 * @param sender
	 * @return
	 */
	public static String replace(VariableScope scope, String message, CommandSender sender, Map<String, Object> replacements) {
		if (message == null || message.isEmpty())
			return "";

		final String original = message;
		final boolean senderIsPlayer = sender instanceof Player;

		// Replace custom variables first
		if (replacements != null && !replacements.isEmpty())
			message = Replacer.replaceArray(message, replacements);

		if (senderIsPlayer) {
			// Already cached ? Return.
			final Map<String, String> cached = customCache.get(sender.getName());

			if (cached != null && cached.containsKey(message))
				return cached.get(message);

			// Custom placeholders
			message = replaceJavascriptVariables0(scope, message, sender);

			// PlaceholderAPI and MvdvPlaceholderAPI
			message = HookManager.replacePlaceholders((Player) sender, message);

		}

		// Default
		message = replaceHardVariables0(sender, message);

		// Support the & color system
		if (REPLACE_COLORS)
			message = Common.colorize(message);

		if (senderIsPlayer) {
			final Map<String, String> map = customCache.get(sender.getName());

			if (map != null)
				map.put(original, message);
			else
				customCache.put(sender.getName(), Common.newHashMap(original, message));
		}

		return message;
	}

	/**
	 * Replaces javascript variables in the message
	 *
	 * @param scope
	 * @param message
	 * @param sender
	 * @return
	 */
	private static String replaceJavascriptVariables0(VariableScope scope, String message, CommandSender sender) {
		for (final Variable variable : Variable.getVariables()) {
			final String key = variable.getKey();

			if (variable.getScope() != scope)
				continue;

			if (message.contains(key))
				try {
					message = message.replace(key, variable.getValue(sender));

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

	/**
	 * Replaces our hardcoded variables in the message, using a cache for better performance
	 *
	 * @param sender
	 * @param message
	 * @return
	 */
	private static String replaceHardVariables0(CommandSender sender, String message) {
		final Matcher matcher = Variables.BRACKET_PLACEHOLDER_PATTERN.matcher(message);
		final Player player = sender instanceof Player ? (Player) sender : null;

		while (matcher.find()) {
			final String variable = matcher.group(1);

			final boolean isSenderCached = cache.contains(sender.getName());
			boolean makeCache = true;

			String value = null;

			// Player is cached
			if (isSenderCached) {
				final Map<String, String> senderCache = cache.get(sender.getName());
				final String storedVariable = senderCache.get(variable);

				// This specific variable is cached
				if (storedVariable != null) {
					value = storedVariable;
					makeCache = false;
				}
			}

			if (makeCache) {
				value = replaceVariable0(variable, player, sender);

				if (value != null) {
					final Map<String, String> speciCache = cache.getOrPut(sender.getName(), makeNewCache());

					speciCache.put(variable, value);
				}
			}

			if (value != null)
				message = message.replace("{" + variable + "}", Common.colorize(value));
		}

		return message;
	}

	/**
	 * Replaces the given variable with a few hardcoded within the plugin, see below
	 * <p>
	 * Also, if the variable ends with +, we insert a space after it if it is not empty
	 *
	 * @param variable
	 * @param player
	 * @param console
	 * @return
	 */
	private static String replaceVariable0(String variable, Player player, CommandSender console) {
		final boolean insertSpace = variable.endsWith("+");

		if (insertSpace)
			variable = variable.substring(0, variable.length() - 1); // Remove the + symbol

		final String found = lookupVariable0(player, console, variable);

		return found == null ? null : found + (insertSpace && !found.isEmpty() ? " " : "");
	}

	/**
	 * Replaces the given variable with a few hardcoded within the plugin, see below
	 *
	 * @param player
	 * @param console
	 * @param variable
	 * @return
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
			case "bungee_server_name":
				return SimpleSettings.BUNGEE_SERVER_NAME;
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
				return player == null ? Common.resolveSenderName(console) : HookManager.getNick(player);

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
		}

		return null;
	}

	/**
	 * Formats the {health} variable
	 *
	 * @param player
	 * @return
	 */
	private static String formatHealth0(Player player) {
		final int hp = Remain.getHealth(player);

		return (hp > 10 ? ChatColor.DARK_GREEN : hp > 5 ? ChatColor.GOLD : ChatColor.RED) + "" + hp;
	}

	/**
	 * Formats the {pl_address} variable for the player
	 *
	 * @param player
	 * @return
	 */
	private static String formatIp0(Player player) {
		try {
			return player.getAddress().toString().split("\\:")[0];
		} catch (final Throwable t) {
			return player.getAddress() != null ? player.getAddress().toString() : "";
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Cache making
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new expiring map with 10 millisecond expiration
	 *
	 * @return
	 */
	private static Map<String, Map<String, String>> makeNewFastCache() {
		return ExpiringMap.builder()
			.maxSize(300)
			.expiration(10, TimeUnit.MILLISECONDS)
			.build();
	}

	/**
	 * Create a new expiring map with 1 second expiration, used to cache player-related
	 * variables that are called 10x after each other to save performance
	 *
	 * @return
	 */
	private static Map<String, String> makeNewCache() {
		return ExpiringMap.builder()
			.maxSize(300)
			.expiration(1, TimeUnit.SECONDS)
			.build();
	}
}