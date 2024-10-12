package org.mineacademy.fo.platform;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;
import org.mineacademy.fo.proxy.message.IncomingMessage;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Distributes received plugin message across all {@link ProxyListener} classes
 *
 * @deprecated internal use only
 */
@Deprecated
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FoundationPluginMessageListener implements PluginMessageListener {

	@Getter
	private static final FoundationPluginMessageListener instance = new FoundationPluginMessageListener();

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] data) {
		synchronized (SimplePlugin.getInstance()) {

			// Check if the message is for a server (ignore client messages)
			if (!channel.equals(ProxyListener.DEFAULT_CHANNEL))
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

			for (final ProxyListener listener : ProxyListener.getRegisteredlisteners())
				if (channelName.equals(listener.getChannel())) {

					final UUID senderUid = UUID.fromString(input.readUTF());
					final String serverName = input.readUTF();
					final String actionName = input.readUTF();

					final ProxyMessage message = ProxyMessage.getByName(listener, actionName);

					if (message == null) {
						new NullPointerException("Unknown plugin action '" + actionName + "'. IF YOU UPDATED THE PLUGIN BY RELOADING, stop your entire network, update all servers and start again.").printStackTrace();

					} else {
						final IncomingMessage incomingMessage = new IncomingMessage(listener, senderUid, serverName, message, data, input, stream);

						listener.setData(data);
						listener.onMessageReceived(Platform.toPlayer(player), incomingMessage);
					}

					break;
				}
		}
	}
}