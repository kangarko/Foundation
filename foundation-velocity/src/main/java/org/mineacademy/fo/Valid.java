package org.mineacademy.fo;

import org.mineacademy.fo.settings.Lang;

import com.velocitypowered.api.proxy.Player;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Valid extends ValidCore {

	/**
	 * Check if the player has the given permission, if false we send him a no permissions
	 * message and return false, otherwise no message is sent and we return true
	 *
	 * @param player
	 * @param permission
	 * @return
	 */
	public static boolean checkPermission(final Player player, final String permission) {
		if (!player.hasPermission(permission)) {
			Common.tell(player, Lang.componentVars("no-permission", "permission", permission));

			return false;
		}

		return true;
	}
}
