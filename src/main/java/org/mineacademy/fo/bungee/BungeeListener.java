package org.mineacademy.fo.bungee;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.bungee.message.IncomingMessage;
import org.mineacademy.fo.debug.Debugger;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
	 * Holds registered bungee listeners
	 */
	private static final Set<BungeeListener> registeredListeners = new HashSet<>();

	/**
	 * The channel
	 */
	@Getter
	private final String channel;

	/**
	 * The actions
	 */
	@Getter
	private final BungeeMessageType[] actions;

	/**
	 * Temporary variable for reading data
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private byte[] data;

	/**
	 * Create a new bungee suite with the given params
	 *
	 * @param channel
	 * @param actionEnum
	 */
	protected BungeeListener(@NonNull String channel, Class<? extends BungeeMessageType> actionEnum) {
		this.channel = channel;
		this.actions = toActions(actionEnum);

		for (final BungeeListener listener : registeredListeners)
			if (listener.getChannel().equals(this.getChannel()))
				return;

		registeredListeners.add(this);
	}

	private static BungeeMessageType[] toActions(@NonNull Class<? extends BungeeMessageType> actionEnum) {
		Valid.checkBoolean(actionEnum != BungeeMessageType.class, "When creating BungeeListener put your own class that extend BungeeMessageType there, not BungeeMessageType class itself!");
		Valid.checkBoolean(actionEnum.isEnum(), "BungeeListener expects BungeeMessageType to be an enum, given: " + actionEnum);

		try {
			return (BungeeMessageType[]) actionEnum.getMethod("values").invoke(null);

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Unable to get values() of " + actionEnum + ", ensure it is an enum or has 'public static T[] values() method'!");

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

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BungeeListener && ((BungeeListener) obj).getChannel().equals(this.getChannel());
	}

	/**
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void clearRegisteredListeners() {
		registeredListeners.clear();
	}

	/**
	 * Distributes received plugin message across all {@link BungeeListener} classes
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class BungeeListenerImpl implements PluginMessageListener {

		@Getter
		private static final BungeeListenerImpl instance = new BungeeListenerImpl();

		@Override
		public void onPluginMessageReceived(String channelName, Player player, byte[] data) {

			// Check if the message is for a server (ignore client messages)
			//if (!channelName.equals("BungeeCord"))
			//	return;

			for (final BungeeListener listener : registeredListeners)
				if (channelName.equals(listener.getChannel())) {

					// Read the plugin message
					final ByteArrayInputStream stream = new ByteArrayInputStream(data);
					ByteArrayDataInput input;

					try {
						input = ByteStreams.newDataInput(stream);

					} catch (final Throwable t) {
						input = ByteStreams.newDataInput(data);
					}

					input.readUTF(); // unused channel name
					final UUID senderUid = UUID.fromString(input.readUTF());
					final String serverName = input.readUTF();
					final String actionName = input.readUTF();

					final BungeeMessageType action = BungeeMessageType.getByName(listener, actionName);
					Valid.checkNotNull(action, "Unknown plugin action '" + actionName + "'. IF YOU UPDATED THE PLUGIN BY RELOADING, stop your entire network, ensure all servers were updated and start it again.");

					final IncomingMessage message = new IncomingMessage(listener, senderUid, serverName, action, data, input, stream);

					listener.data = data;

					Debugger.debug("bungee-all", "Channel " + channelName + " received " + message.getAction() + " message from " + message.getServerName() + " server.");
					listener.onMessageReceived(player, message);

					break;
				}
		}
	}
}
