package org.mineacademy.fo.bungee;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.bungee.message.IncomingMessage;
import org.mineacademy.fo.debug.Debugger;

import lombok.Getter;

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
	private final BungeeAction[] actions;

	/**
	 * Create a new bungee suite with the given params
	 *
	 * @param channel
	 * @param listener
	 * @param actions
	 */
	protected BungeeListener(String channel, Class<? extends BungeeAction> actionEnum) {
		Valid.checkNotNull(channel, "Channel cannot be null!");

		this.channel = channel;

		final BungeeAction[] actions = toActions(actionEnum);
		Valid.checkNotNull(actions, "Actions cannot be null!");

		this.actions = actions;
	}

	private static BungeeAction[] toActions(Class<? extends BungeeAction> actionEnum) {
		Valid.checkNotNull(actionEnum);
		Valid.checkBoolean(actionEnum.isEnum(), "Enum expected, given: " + actionEnum);

		try {
			return (BungeeAction[]) actionEnum.getMethod("values").invoke(null);

		} catch (final ReflectiveOperationException ex) {
			Common.log("Unable to get values() of " + actionEnum + ", ensure it is an enum!");
			ex.printStackTrace();

			return null;
		}
	}

	/**
	 * Handle the received message automatically if it matches our tag
	 */
	@Override
	public final void onPluginMessageReceived(String channelName, Player player, byte[] data) {

		// Cauldron/Thermos is unsupported for bungee
		if (Bukkit.getName().contains("Cauldron"))
			return;

		final IncomingMessage message = new IncomingMessage(data);

		Debugger.debug("bungee", "Channel " + message.getChannel() + " received " + message.getAction() + " message from " + message.getServerName() + " server.");
		onMessageReceived(player, message);
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
