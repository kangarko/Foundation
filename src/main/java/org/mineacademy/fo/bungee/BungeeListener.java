package org.mineacademy.fo.bungee;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.bungee.message.IncomingMessage;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a BungeeCord listener using a bungee channel
 * on which you can listen to receiving messages
 * <p>
 * This class is also a Listener for Bukkit events for your convenience
 */
@Getter
public abstract class BungeeListener implements Listener {

	/**
	 * Holds all registered listeners, all using "BungeeCord" channel and actually writing
	 * the channel name as UTF in to the byte array to avoid "Unknown custom packed identifier: plugin:chcred"
	 * bug.
	 */
	private static final List<BungeeListener> registeredListeners = new ArrayList<>();

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
		this.actions = toActions(actionEnum);
	}

	private static BungeeMessageType[] toActions(@NonNull Class<? extends BungeeMessageType> actionEnum) {
		Valid.checkBoolean(actionEnum.isEnum(), "BungeeListener expects BungeeMessageType to be an enum, given: " + actionEnum);

		try {
			return (BungeeMessageType[]) actionEnum.getMethod("values").invoke(null);

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Unable to get values() of " + actionEnum + ", ensure it is an enum!");

			return null;
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

	/**
	 * @deprecated internal use only
	 *
	 * @param listener
	 */
	@Deprecated
	public static void addRegisteredListener(BungeeListener listener) {
		registeredListeners.add(listener);
	}

	/**
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void clearRegisteredListeners() {
		registeredListeners.clear();
	}

	/**
	 * A helper to read data once for all sub-channels and distribute the message.
	 */
	public static final class CommonBungeeListener implements PluginMessageListener {

		/**
		 * Handle the received message automatically if it matches our tag
		 */
		@Override
		public void onPluginMessageReceived(String channelName, Player player, byte[] data) {

			// Cauldron/Thermos is unsupported for Bungee
			if (Bukkit.getName().contains("Cauldron"))
				return;

			if (!channelName.equals("BungeeCord"))
				return;

			final ByteArrayDataInput is = ByteStreams.newDataInput(new ByteArrayInputStream(data));
			final String ourChannelName;

			try {
				ourChannelName = is.readUTF();

			} catch (Exception ex) {
				// Foundation uses the BungeeCord channel and so do other plugins,
				// we have to determine if our channel is set to check it.
				return;
			}

			for (BungeeListener listener : registeredListeners)
				if (listener.getChannel().equals(ourChannelName)) {
					final IncomingMessage message = new IncomingMessage(data);

					listener.onMessageReceived(player, message);
				}
		}
	}
}
