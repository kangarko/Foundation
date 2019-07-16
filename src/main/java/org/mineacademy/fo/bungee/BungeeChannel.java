package org.mineacademy.fo.bungee;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the names of channels used when communicating with Bungeecord
 *
 * MAKE SURE TO INCLUDE "plugin:" in your channel names
 */
public interface BungeeChannel {

	/**
	 * The default bungeecord channel, see
	 * https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/
	 */
	BungeeChannel DEFAULT_CHANNEL = new WrapperBungeeChannel("BungeeCord");

	/**
	 * Represents the name of the channel recognized by both
	 * Bungeecords and Bukkit/Spigot servers
	 *
	 * @return
	 */
	String getName();

	/**
	 * Implementation for BungeeChannel for some default channels
	 */
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class WrapperBungeeChannel implements BungeeChannel {
		private final String name;
	}
}
