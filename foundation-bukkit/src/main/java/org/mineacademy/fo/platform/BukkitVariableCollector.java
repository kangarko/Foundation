package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;

import org.bukkit.entity.Player;
import org.mineacademy.fo.GeoAPI;
import org.mineacademy.fo.GeoAPI.GeoResponse;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.remain.Remain;

/**
 * Expands the functionality of {@link Variables} to include Bukkit-specific variables,
 * and also hooks into PlaceholderAPI.
 */
final class BukkitVariableCollector implements Variables.Collector {

	@Override
	public SimpleComponent replaceVariable(String pluginIdentifier, String params, String variable, FoundationPlayer audience) {
		final Player player = audience != null && audience.isPlayer() ? ((BukkitPlayer) audience).getPlayer() : null;

		// Replace PlaceholderAPI
		if (!pluginIdentifier.isEmpty()) {
			final Object placeholderExpansion = HookManager.getPlaceholderAPIHooks().get(pluginIdentifier);

			if (placeholderExpansion != null) {
				final String value = HookManager.getPlaceholderAPIValue(placeholderExpansion, pluginIdentifier, player, params);

				if (value != null)
					return SimpleComponent.fromSection(value);
			}
		}

		if ("server_version".equals(variable))
			return SimpleComponent.fromPlain(MinecraftVersion.getFullVersion());

		else if ("nms_version".equals(variable))
			return SimpleComponent.fromPlain(Remain.getNmsVersion());

		else if ("player".equals(variable) || "player_name".equals(variable))
			return SimpleComponent.fromPlain(audience == null ? "" : audience.getName());

		else if ("player_uuid".equals(variable))
			return SimpleComponent.fromPlain(player == null ? "" : player.getUniqueId().toString());

		else if ("town".equals(variable))
			return SimpleComponent.fromSection(player == null ? "" : HookManager.getTownName(player));

		else if ("nation".equals(variable))
			return SimpleComponent.fromSection(player == null ? "" : HookManager.getNation(player));

		else if ("faction".equals(variable))
			return SimpleComponent.fromSection(player == null ? "" : HookManager.getFaction(player));

		else if ("world".equals(variable))
			return SimpleComponent.fromSection(player == null ? "" : HookManager.getWorldAlias(player.getWorld()));

		else if ("health".equals(variable))
			return SimpleComponent.fromSection(player == null ? "" : formatHealth(player));

		else if ("location".equals(variable))
			return SimpleComponent.fromPlain(player == null ? "" : SerializeUtil.serializeLoc(player.getLocation()));

		else if ("x".equals(variable))
			return SimpleComponent.fromPlain(player == null ? "" : String.valueOf(player.getLocation().getBlockX()));

		else if ("y".equals(variable))
			return SimpleComponent.fromPlain(player == null ? "" : String.valueOf(player.getLocation().getBlockY()));

		else if ("z".equals(variable))
			return SimpleComponent.fromPlain(player == null ? "" : String.valueOf(player.getLocation().getBlockZ()));

		else if ("tab_name".equals(variable))
			return SimpleComponent.fromSection(player == null ? audience.getName() : player.getPlayerListName());

		else if ("display_name".equals(variable))
			return SimpleComponent.fromSection(player == null ? audience.getName() : player.getDisplayName());

		else if ("player_nick".equals(variable) || "nick".equals(variable))
			return SimpleComponent.fromSection(player == null ? audience.getName() : HookManager.getNickColored(player));

		else if ("player_prefix".equals(variable))
			return SimpleComponent.fromSection(player == null ? "" : HookManager.getPlayerPrefix(player));

		else if ("player_suffix".equals(variable))
			return SimpleComponent.fromSection(player == null ? "" : HookManager.getPlayerSuffix(player));

		else if ("player_group".equals(variable))
			return SimpleComponent.fromSection(player == null ? "" : HookManager.getPlayerPermissionGroup(player));

		else if ("player_primary_group".equals(variable))
			return SimpleComponent.fromSection(player == null ? "" : HookManager.getPlayerPrimaryGroup(player));

		else if ("player_ip".equals(variable))
			return SimpleComponent.fromPlain(player == null ? "" : formatIp(player));

		else if ("player_vanished".equals(variable))
			return SimpleComponent.fromPlain(player == null ? "false" : String.valueOf(PlayerUtil.isVanished(player)));

		else if ("country_code".equals(variable) || "country_name".equals(variable) || "region_name".equals(variable) || "isp".equals(variable)) {
			final InetSocketAddress ip = audience == null ? null : audience.getAddress();

			if (ip == null)
				return SimpleComponent.fromPlain("");

			final GeoResponse geoResponse = GeoAPI.getCountry(ip);

			if (geoResponse == null)
				return SimpleComponent.fromPlain("");

			else if ("country_code".equals(variable))
				return SimpleComponent.fromPlain(geoResponse.getCountryCode());

			else if ("country_name".equals(variable))
				return SimpleComponent.fromPlain(geoResponse.getCountryName());

			else if ("region_name".equals(variable))
				return SimpleComponent.fromPlain(geoResponse.getRegionName());

			else if ("isp".equals(variable))
				return SimpleComponent.fromPlain(geoResponse.getIsp());
		}

		return null;
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
