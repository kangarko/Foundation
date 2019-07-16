package org.mineacademy.fo.bungee;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * Represents a bungeecords listener using a {@link BungeeChannel} channel
 * on which you can listen to receiving messages
 *
 * This class is also a Listener for Bukkit events for your convenience
 */
public abstract class BungeeListener implements Listener, PluginMessageListener {

	/**
	 * The channel name
	 */
	private final BungeeChannel channelName;

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
		this.channelName = channel;

		Common.registerEvents(this);
	}

	/**
	 * Return the bungee channel
	 *
	 * @return
	 */
	public final BungeeChannel getChannel() {
		return channelName;
	}

	/**
	 * Handle the received message automatically if it matches our tag
	 */
	@Override
	public final void onPluginMessageReceived(String tag, Player player, byte[] data) {
		if (tag.equals(channelName.getName()))
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
