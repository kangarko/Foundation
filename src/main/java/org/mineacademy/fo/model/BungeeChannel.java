package org.mineacademy.fo.model;

import lombok.Getter;

/**
 * Represents the names of channels used when communicating with Bungeecord
 */
public enum BungeeChannel {

	/**
	 * The default bungeecord channel, see
	 * https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/
	 */
	BUNGEECORD("BungeeCord"),

	/**
	 * The channel for ChatControl
	 */
	CHATCONTROL("plugin:chatcontrol");

	/**
	 * The name of the channel
	 */
	@Getter
	private String name;

	/**
	 * Constructs a new bungee channel name
	 *
	 * @param channel
	 */
	BungeeChannel(String channel) {
		this.name = channel;
	}
}
