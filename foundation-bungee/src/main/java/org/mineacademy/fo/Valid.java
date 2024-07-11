package org.mineacademy.fo;

import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.CommandSender;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Valid extends ValidCore {

	/**
	 * Check if the player has the given permission, if false we send him a no permissions
	 * message and return false, otherwise no message is sent and we return true
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	public static boolean checkPermission(final CommandSender sender, final String permission) {
		if (!sender.hasPermission(permission)) {
			Common.tell(sender, Lang.componentVars("no-permission", "permission", permission));

			return false;
		}

		return true;
	}
}
