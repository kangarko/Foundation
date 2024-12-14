package org.mineacademy.fo;

import java.util.function.Function;

import org.mineacademy.fo.model.SimpleComponent;

import com.velocitypowered.api.proxy.Player;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Utility class for player-related methods.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerUtil {

	/**
	 * Set the check if the player is vanished.
	 */
	@Setter
	private static Function<Player, Boolean> isVanished = player -> false;

	/**
	 * Kick the player from the server with the given reason.
	 *
	 * @param player
	 * @param reason
	 */
	public static void kick(final Player player, final String reason) {
		player.disconnect(SimpleComponent.fromMiniAmpersand(reason).toAdventure(null));
	}

	/**
	 * Checks if the player is vanished.
	 *
	 * @param player
	 * @return
	 */
	public static boolean isVanished(final Player player) {
		return isVanished.apply(player);
	}
}
