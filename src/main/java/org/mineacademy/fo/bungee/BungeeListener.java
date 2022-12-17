package org.mineacademy.fo.bungee;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.bungee.message.IncomingMessage;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a BungeeCord listener using a bungee channel
 * on which you can listen to receiving messages
 * <p>
 * This class is also a Listener for Bukkit events for your convenience
 */
@Getter
public abstract class BungeeListener implements Listener, PluginMessageListener {

	/**
	 * The channel
	 */
	private final String channel;

	/**
	 * The actions
	 */
	private final BungeeMessageType[] actions;

	/**
	 * Create a new bungee suite with the given params
	 *
	 * @param channel
	 * @param listener
	 * @param actions
	 */
	protected BungeeListener(@NonNull String channel, Class<? extends BungeeMessageType> actionEnum) {
		this.channel = channel;
		this.actions = toAction(actionEnum);
	}

	/*
	 * Get values() from the action enum
	 */
	private static BungeeMessageType[] toAction(@NonNull Class<? extends BungeeMessageType> actionEnum) {
		Valid.checkBoolean(actionEnum != BungeeMessageType.class, "When creating BungeeListener put your own class that extend BungeeMessageType there, not BungeeMessageType class itself!");
		Valid.checkBoolean(actionEnum.isEnum(), "BungeeListener expects BungeeMessageType to be an enum, given: " + actionEnum);

		try {
			return (BungeeMessageType[]) actionEnum.getMethod("values").invoke(null);

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Unable to get values() of " + actionEnum + ", ensure it is an enum!");

			return null;
		}
	}

	/**
	 * Handle the received message automatically if it matches our tag
	 */
	@Override
	public final void onPluginMessageReceived(String channelName, Player player, byte[] data) {

		// Cauldron/Thermos is unsupported for Bungee
		if (!Bukkit.getName().contains("Cauldron")) {
			final IncomingMessage message = new IncomingMessage(this, data);

			this.onMessageReceived(player, message);
		}
	}

	/**
	 * Called automatically when you receive a plugin message from Bungeecord,
	 * see https://spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel
	 *
	 * @param player
	 * @param message
	 */
	public abstract void onMessageReceived(Player player, IncomingMessage message);
}
