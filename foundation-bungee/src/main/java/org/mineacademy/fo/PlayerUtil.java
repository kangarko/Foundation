package org.mineacademy.fo;

import java.util.function.Function;

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
	 * Checks if the player is vanished.
	 *
	 * @param player
	 * @return
	 */
	public static boolean isVanished(ProxiedPlayer player) {
		return isVanished.apply(player);
	}
}
