package org.mineacademy.fo.model;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * Represents a bungeecords listener using a {@link BungeeChannel} channel
 * on which you can listen to receiving messages
 */
public abstract class BungeeListener implements PluginMessageListener {

	/**
	 * The channel name
	 */
	private final String channelName;

	/**
	 * Create a new bungee listener using {@link SimplePlugin#getDefaultBungeeChannel()}
	 */
	protected BungeeListener() {
		this(SimplePlugin.getInstance().getDefaultBungeeChannel());
	}

	/**
	 * Create a new bungee listener using the given channel
	 *
	 * @param channel
	 */
	protected BungeeListener(BungeeChannel channel) {
		this.channelName = channel.getName();
	}

	/**
	 * Return the bungee channel
	 *
	 * @return
	 */
	public final String getChannel() {
		return channelName;
	}

	/**
	 * Handle the received message automatically if it matches our tag
	 */
	@Override
	public final void onPluginMessageReceived(String tag, Player player, byte[] data) {
		if (tag.equals(channelName))
			onMessageReceived(player, data);
	}

	/**
	 * Called automatically when you receive a plugin message from Bungeecord,
	 * see https://spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel
	 *
	 * @param player
	 * @param data
	 */
	public abstract void onMessageReceived(Player player, byte[] data);
}
