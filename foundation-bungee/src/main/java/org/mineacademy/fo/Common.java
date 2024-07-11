package org.mineacademy.fo;

import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.md_5.bungee.api.CommandSender;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Common extends CommonCore {

	/**
	 * Sends a message to the player
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tell(@NonNull CommandSender sender, String... messages) {
		final FoundationPlayer audience = Platform.toPlayer(sender);

		for (final String message : messages)
			audience.sendMessage(SimpleComponent.fromMini(message));
	}

	/**
	 * Sends a message to the audience. Supports {plugin_prefix} and {player} variable.
	 * Supports \<actionbar\>, \<toast\>, \<title\>, \<bossbar\> and \<center\>.
	 * Properly sends the message to the player if he is conversing with the server.
	 *
	 * @param sender
	 * @param message
	 */
	public static void tell(@NonNull final CommandSender sender, SimpleComponent message) {
		Platform.toPlayer(sender).sendMessage(message);
	}
}
