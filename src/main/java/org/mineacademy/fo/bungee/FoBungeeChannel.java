package org.mineacademy.fo.bungee;

import lombok.Getter;

/**
 * Properitary implementation of the names of channels used when
 * we communicate with Bungeecord with our plugins
 */
public enum FoBungeeChannel implements BungeeChannel {

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
	FoBungeeChannel(String channel) {
		this.name = channel;
	}
}
