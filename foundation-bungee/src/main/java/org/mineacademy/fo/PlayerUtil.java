package org.mineacademy.fo;

import java.util.function.Function;

import org.mineacademy.fo.model.SimpleComponent;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Utility class for player-related methods.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerUtil {

	/**
	 * Set the check if the player is vanished.
	 */
	@Setter
	private static Function<ProxiedPlayer, Boolean> isVanished = player -> false;

	/**
	 * Kick the player from the server with the given reason.
	 *
	 * @param player
	 * @param reason
	 */
	public static void kick(ProxiedPlayer player, String reason) {
		player.disconnect(SimpleComponent.fromMini(reason).toLegacy());
	}

	/**
	 * Checks if the player is vanished.
	 *
	 * @param player
	 * @return
	 */
	public static boolean isVanished(ProxiedPlayer player) {
		return isVanished.apply(player);
	}
}
