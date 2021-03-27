package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * A simple engine that replaces variables in a message.
 */
public final class Variables {

	/**
	 * The pattern to find singular [syntax_name] variables
	 */
	public static final Pattern MESSAGE_PLACEHOLDER_PATTERN = Pattern.compile("[\\[]([^\\[\\]]+)[\\]]");

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
	 * Used internally to prevent race condition
	 */
	static boolean REPLACE_JAVASCRIPT = true;

	// ------------------------------------------------------------------------------------------------------------
	// Custom variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Variables added to Foundation by you or other plugins
	 *
	 * You take in a command sender (may/may not be a player) and output a replaced string.
	 * The variable name (the key) is automatically surrounded by {} brackets
	 */
	private static final StrictMap<String, Function<CommandSender, String>> customVariables = new StrictMap<>();

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
	 * Return the variable for the given key that is a function of replacing
	 * itself for the player. Returns null if no such variable by key is present.
	 *
	 * @return
	 */
	@Nullable
	public static Function<CommandSender, String> getVariable(String key) {
		return customVariables.get(key);
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
		customVariables.override(variable, replacer);
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
	public static List<String> replace(Iterable<String> messages, @Nullable CommandSender sender, @Nullable Map<String, Object> replacements) {

		// Trick: Join the lines to only parse variables at once -- performance++ -- then split again
		final String deliminer = "%FLVJ%";

		return Arrays.asList(replace(String.join(deliminer, messages), sender, replacements).split(deliminer));
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
		return replace(message, sender, replacements, true);
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI (only if sender is a Player).
	 *
	 * @param message
	 * @param sender
	 * @param colorize
	 * @return
	 */
	public static String replace(String message, @Nullable CommandSender sender, @Nullable Map<String, Object> replacements, boolean colorize) {
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

		}

		// Custom placeholders
		if (REPLACE_JAVASCRIPT) {
			REPLACE_JAVASCRIPT = false;

			try {
				message = replaceJavascriptVariables0(message, sender, replacements);

			} finally {
				REPLACE_JAVASCRIPT = true;
			}
		}

		if (senderIsPlayer) {

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
	private static String replaceJavascriptVariables0(String message, CommandSender sender, @Nullable Map<String, Object> replacements) {

		final Matcher matcher = BRACKET_PLACEHOLDER_PATTERN.matcher(message);

		while (matcher.find()) {
			final String variableKey = matcher.group();

			// Find the variable key without []
			final Variable variable = Variable.findVariable(variableKey.substring(1, variableKey.length() - 1));

			if (variable != null && variable.getType() == Variable.Type.FORMAT) {
				final SimpleComponent component = variable.build(sender, SimpleComponent.empty(), replacements);

				// We do not support interact chat elements in format variables,
				// so we just flatten the variable. Use formatting or chat variables instead.
				String plain = component.getPlainMessage();

				// And we remove the white prefix that is by default added in every component
				if (plain.startsWith(ChatColor.COLOR_CHAR + "f" + ChatColor.COLOR_CHAR + "f"))
					plain = plain.substring(4);

				message = message.replace(variableKey, plain);
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
				final boolean emptyColorless = Common.stripColors(value).isEmpty();
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

		if (console != null) {

			// Replace custom expansions
			for (final SimpleExpansion expansion : customExpansions) {
				final String value = expansion.replacePlaceholders(console, variable);

				if (value != null)
					return value;
			}

			// Replace custom variables
			final Function<CommandSender, String> customReplacer = customVariables.get(variable);

			if (customReplacer != null)
				return customReplacer.apply(console);
		}

		switch (variable) {
			case "server_name":
				return Remain.getServerName();
			case "nms_version":
				return MinecraftVersion.getServerVersion();
			case "timestamp":
				return SimpleSettings.TIMESTAMP_FORMAT.format(System.currentTimeMillis());
			case "timestamp_short":
				return TimeUtil.getFormattedDateShort();
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
			case "player_name":
				return player == null ? Common.resolveSenderName(console) : player.getName();
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
				return SimplePlugin.getInstance().getMainCommand() != null ? SimplePlugin.getInstance().getMainCommand().getLabel() : "noMainCommandLabel";
			case "sender_is_player":
				return player != null ? "true" : "false";
			case "sender_is_discord":
				return console instanceof DiscordSender ? "true" : "false";
			case "sender_is_console":
				return console instanceof ConsoleCommandSender ? "true" : "false";

			case "plugin_prefix":
				return SimpleSettings.PLUGIN_PREFIX;
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