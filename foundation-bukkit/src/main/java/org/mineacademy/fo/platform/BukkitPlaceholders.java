package org.mineacademy.fo.platform;

import org.bukkit.entity.Player;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleExpansion;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Expands the functionality of {@link Variables} to include Bukkit-specific variables,
 * and also hooks into PlaceholderAPI.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BukkitPlaceholders extends SimpleExpansion {

	@Getter
	private final static BukkitPlaceholders instance = new BukkitPlaceholders();

	@Override
	protected String onReplace(final FoundationPlayer audience, final String identifier) {
		final Player player = audience != null && audience.isPlayer() ? audience.getPlayer() : null;
		final String fallbackName = audience != null ? audience.getName() : "Unknown";

		if (player != null && !player.isOnline())
			return null;

		if ("player_tab_name".equals(identifier))
			return player == null ? fallbackName : player.getPlayerListName();

		else if ("player_display_name".equals(identifier))
			return player == null ? fallbackName : player.getDisplayName();

		else if ("player_nick".equals(identifier))
			return player == null ? fallbackName : CommonCore.getOrDefault(HookManager.getNickOrNullColored(player), fallbackName);

		else if ("player_prefix".equals(identifier))
			return player == null ? "" : HookManager.getPlayerPrefix(player);

		else if ("player_suffix".equals(identifier))
			return player == null ? "" : HookManager.getPlayerSuffix(player);

		else if ("player_group".equals(identifier))
			return player == null ? "" : HookManager.getPlayerPermissionGroup(player);

		else if ("player_primary_group".equals(identifier))
			return player == null ? "" : HookManager.getPlayerPrimaryGroup(player);

		else if ("player_vanished".equals(identifier))
			return player == null ? "false" : String.valueOf(PlayerUtil.isVanished(player));

		else if ("player_town".equals(identifier))
			return player == null ? "" : HookManager.getTownName(player);

		else if ("player_nation".equals(identifier))
			return player == null ? "" : HookManager.getNation(player);

		else if ("player_faction".equals(identifier))
			return player == null ? "" : HookManager.getFaction(player);

		else if ("player_world".equals(identifier))
			return player == null ? "" : HookManager.getWorldAlias(player.getWorld());

		else if ("player_gamemode".equals(identifier))
			return player == null ? "" : ChatUtil.capitalize(player.getGameMode().name().toLowerCase());

		else if ("player_ping".equals(identifier))
			return player == null ? "" : String.valueOf(Remain.getPing(player));

		else if ("player_health".equals(identifier))
			return player == null ? "" : String.valueOf(Remain.getHealth(player));

		else if ("player_health_colorized".equals(identifier))
			return player == null ? "" : formatHealth(player);

		else if ("player_location".equals(identifier))
			return player == null ? "" : SerializeUtil.serializeLocation(player.getLocation());

		else if ("player_x".equals(identifier))
			return player == null ? "" : String.valueOf(player.getLocation().getBlockX());

		else if ("player_y".equals(identifier))
			return player == null ? "" : String.valueOf(player.getLocation().getBlockY());

		else if ("player_z".equals(identifier))
			return player == null ? "" : String.valueOf(player.getLocation().getBlockZ());

		else if ("nms_version".equals(identifier) || "server_nms_version".equals(identifier))
			return Remain.getNmsVersion();

		else
			return null;
	}

	/*
	 * Formats the {health} variable
	 */
	private static String formatHealth(final Player player) {
		final int health = Remain.getHealth(player);

		return (health > 10 ? CompChatColor.DARK_GREEN : health > 5 ? CompChatColor.GOLD : CompChatColor.RED) + "" + health + CompChatColor.RESET;
	}

	@Override
	public int getPriority() {
		return 9;
	}
}
