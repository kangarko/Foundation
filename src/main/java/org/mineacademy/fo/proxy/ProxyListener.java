package org.mineacademy.fo.proxy;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.proxy.message.IncomingMessage;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Represents a proxy listener using on which you can listen to receiving messages
 * with Foundation format.
 *
 * This class is also a Listener for Bukkit events for your convenience
 */
@Getter
public abstract class ProxyListener implements Listener {

	/**
	 * The default channel
	 */
	public static final String DEFAULT_CHANNEL = "BungeeCord";

	/**
	 * Holds registered listeners
	 */
	private static final Set<ProxyListener> registeredListeners = new HashSet<>();

	/**
	 * The channel
	 */
	@Getter
	private final String channel;

	/**
	 * The actions
	 */
	@Getter
	private final ProxyMessage[] actions;

	/**
	 * Temporary variable for reading data
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private byte[] data;

	/**
	 * Create a new listener with the given params
	 *
	 * @param channel
	 * @param actionEnum
	 */
	protected ProxyListener(@NonNull String channel, Class<? extends ProxyMessage> actionEnum) {
		this.channel = channel;
		this.actions = toActions(actionEnum);

		for (final ProxyListener listener : registeredListeners)
			if (listener.getChannel().equals(this.getChannel()))
				return;

		registeredListeners.add(this);
	}

	private static ProxyMessage[] toActions(@NonNull Class<? extends ProxyMessage> actionEnum) {
		Valid.checkBoolean(actionEnum != ProxyMessage.class, "When creating a new proxy listener put your own class that extend ProxyMessage there, not ProxyMessage class itself!");
		Valid.checkBoolean(actionEnum.isEnum(), "Proxy listener expects ProxyMessage to be an enum, given: " + actionEnum);

		try {
			return (ProxyMessage[]) actionEnum.getMethod("values").invoke(null);

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Unable to get values() of " + actionEnum + ", ensure it is an enum or has 'public static T[] values() method'!");

			return null;
		}
	}

	/**
	 * Called automatically when you receive a plugin message from proxy,
	 * see https://spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel
	 *
	 * @param player
	 * @param message
	 */
	public abstract void onMessageReceived(Player player, IncomingMessage message);

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ProxyListener && ((ProxyListener) obj).getChannel().equals(this.getChannel());
	}

	/**
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void clearRegisteredListeners() {
		registeredListeners.clear();
	}

	/**
	 * Distributes received plugin message across all {@link ProxyListener} classes
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class ProxyListenerImpl implements PluginMessageListener {

		@Getter
		private static final ProxyListenerImpl instance = new ProxyListenerImpl();

		@Override
		public void onPluginMessageReceived(String channel, Player player, byte[] data) {
			synchronized (SimplePlugin.getInstance()) {

				// Check if the message is for a server (ignore client messages)
				if (!channel.equals(DEFAULT_CHANNEL))
					return;

				// Read the plugin message
				final ByteArrayInputStream stream = new ByteArrayInputStream(data);
				ByteArrayDataInput input;

				try {
					input = ByteStreams.newDataInput(stream);

				} catch (final Throwable t) {
					input = ByteStreams.newDataInput(data);
				}

				final String channelName = input.readUTF();

				for (final ProxyListener listener : registeredListeners)
					if (channelName.equals(listener.getChannel())) {

						final UUID senderUid = UUID.fromString(input.readUTF());
						final String serverName = input.readUTF();
						final String actionName = input.readUTF();

						final ProxyMessage message = ProxyMessage.getByName(listener, actionName);
						Valid.checkNotNull(message, "Unknown plugin action '" + actionName + "'. IF YOU UPDATED THE PLUGIN BY RELOADING, stop your entire network, update all servers and start again.");

						final IncomingMessage incomingMessage = new IncomingMessage(listener, senderUid, serverName, message, data, input, stream);

						listener.data = data;
						listener.onMessageReceived(player, incomingMessage);

						break;
					}
			}
		}
	}
}
