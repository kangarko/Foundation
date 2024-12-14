package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.Remain;

import com.velocitypowered.api.proxy.Player;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Common extends CommonCore {

	/**
	 * Sends a message to the player
	 *
	 * @param player
	 * @param messages
	 */
	public static void tell(@NonNull Player player, String... messages) {
		final FoundationPlayer audience = Platform.toPlayer(player);

		for (final String message : messages)
			audience.sendMessage(SimpleComponent.fromMiniAmpersand(message));
	}

	/**
	 * Sends a message to the audience. Supports {plugin_prefix} and {player} variable.
	 * Supports \<actionbar\>, \<toast\>, \<title\>, \<bossbar\> and \<center\>.
	 * Properly sends the message to the player if he is conversing with the server.
	 *
	 * @param player
	 * @param message
	 */
	public static void tell(@NonNull final Player player, SimpleComponent message) {
		Platform.toPlayer(player).sendMessage(message);
	}

	/**
	 * Convenience method for getting a list of player names
	 *
	 * @param ignoreVanished
	 * @return
	 */
	public static List<String> getPlayerNames(boolean ignoreVanished) {
		final List<String> found = new ArrayList<>();

		for (final Player online : Remain.getOnlinePlayers(ignoreVanished))
			found.add(online.getUsername());

		return found;
	}

	/**
	 * Convenience method for getting a list of server names
	 *
	 * @return
	 */
	public static List<String> getServerNames() {
		return new ArrayList<>(convertList(Remain.getServers(), server -> server.getServerInfo().getName()));
	}
}
