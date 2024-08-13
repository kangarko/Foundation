package org.mineacademy.fo.plugin;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.GeoAPI;
import org.mineacademy.fo.GeoAPI.GeoResponse;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.Remain;

import net.kyori.adventure.audience.Audience;

public final class BukkitVariableCollector implements Variables.Collector {

	@Override
	public void addVariables(String variable, Audience audience, Map<String, Object> replacements) {
		final Player player = audience instanceof Player ? (Player) audience : null;

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

		// Replace hard variables
		GeoResponse geoResponse = null;

		if (audience instanceof Player && Arrays.asList("country_code", "country_name", "region_name", "isp").contains(variable))
			geoResponse = GeoAPI.getCountry(player.getAddress());

		final String senderName = Platform.resolveSenderName(audience);

		replacements.put("server_version", MinecraftVersion.getFullVersion());
		replacements.put("nms_version", MinecraftVersion.getServerVersion());
		replacements.put("player", senderName);
		replacements.put("player_name", senderName);
		replacements.put("town", player == null ? "" : HookManager.getTownName(player));
		replacements.put("nation", player == null ? "" : HookManager.getNation(player));
		replacements.put("faction", player == null ? "" : HookManager.getFaction(player));
		replacements.put("world", player == null ? "" : HookManager.getWorldAlias(player.getWorld()));
		replacements.put("health", player == null ? "" : formatHealth(player));
		replacements.put("location", player == null ? "" : Common.shortLocation(player.getLocation()));
		replacements.put("x", player == null ? "" : String.valueOf(player.getLocation().getBlockX()));
		replacements.put("y", player == null ? "" : String.valueOf(player.getLocation().getBlockY()));
		replacements.put("z", player == null ? "" : String.valueOf(player.getLocation().getBlockZ()));
		replacements.put("tab_name", player == null ? senderName : player.getPlayerListName());
		replacements.put("display_name", player == null ? senderName : player.getDisplayName());
		replacements.put("player_nick", player == null ? senderName : HookManager.getNickColored(player));
		replacements.put("nick", player == null ? senderName : HookManager.getNickColored(player));
		replacements.put("player_prefix", player == null ? "" : HookManager.getPlayerPrefix(player));
		replacements.put("player_suffix", player == null ? "" : HookManager.getPlayerSuffix(player));
		replacements.put("player_group", player == null ? "" : HookManager.getPlayerPermissionGroup(player));
		replacements.put("player_primary_group", player == null ? "" : HookManager.getPlayerPrimaryGroup(player));
		replacements.put("ip_address", player == null ? "" : formatIp(player));
		replacements.put("player_vanished", player == null ? "false" : String.valueOf(PlayerUtil.isVanished(player)));
		replacements.put("country_code", geoResponse == null ? "" : geoResponse.getCountryCode());
		replacements.put("country_name", geoResponse == null ? "" : geoResponse.getCountryName());
		replacements.put("region_name", geoResponse == null ? "" : geoResponse.getRegionName());
		replacements.put("isp", geoResponse == null ? "" : geoResponse.getIsp());
		replacements.put("sender_is_player", player != null ? "true" : "false");
	}

	/*
	 * Formats the {health} variable
	 */
	private static String formatHealth(Player player) {
		final int health = Remain.getHealth(player);

		return (health > 10 ? CompChatColor.DARK_GREEN : health > 5 ? CompChatColor.GOLD : CompChatColor.RED) + "" + health + CompChatColor.RESET;
	}

	/*
	 * Formats the IP address variable for the player
	 */
	private static String formatIp(Player player) {
		try {
			return player.getAddress().toString().split("\\:")[0];

		} catch (final Throwable t) {
			return player.getAddress() != null ? player.getAddress().toString() : "";
		}
	}
}
