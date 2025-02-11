package org.mineacademy.fo.platform;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;
import org.mineacademy.fo.proxy.message.IncomingMessage;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

/**
 * A listener that forwards incoming messages to the registered listeners
 *
 * @deprecated internal use only
 */
@Deprecated
public final class BungeeListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(final ServerConnectEvent event) {
		final ProxiedPlayer player = event.getPlayer();

		((BungeePlatform) Platform.getPlatform()).registerPlayer(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(final ServerDisconnectEvent event) {
		final ProxiedPlayer player = event.getPlayer();

		Platform.getPlatform().unregisterPlayer(player.getUniqueId());
	}

	/**
	 * Handle the received message automatically if it matches our tag
	 *
	 * @param event
	 */
	@EventHandler
	public void onPluginMessage(final PluginMessageEvent event) {
		synchronized (ProxyListener.DEFAULT_CHANNEL) {
			final Connection sender = event.getSender();
			final byte[] data = event.getData();

			if (event.isCancelled())
				return;

			// Check if the message is for a server (ignore client messages)
			if (!event.getTag().equals("BungeeCord"))
				return;

			// Check if a player is not trying to send us a fake message
			if (!(sender instanceof Server))
				return;

			// Read the plugin message
			final ByteArrayInputStream stream = new ByteArrayInputStream(data);
			ByteArrayDataInput in;

			try {
				in = ByteStreams.newDataInput(stream);

			} catch (final Throwable t) {
				in = ByteStreams.newDataInput(data);
			}

			final String subChannel = in.readUTF();

			boolean handled = false;

			for (final ProxyListener listener : ProxyListener.getRegisteredListeners())
				if (subChannel.equals(listener.getChannel())) {

					final UUID senderUid = UUID.fromString(in.readUTF());
					final String serverName = in.readUTF();
					final String actionName = in.readUTF();

					final ProxyMessage message = ProxyMessage.getByName(listener, actionName);

					if (message == null)
						new NullPointerException("Unknown plugin message '" + actionName + "'. IF YOU UPDATED THE PLUGIN BY RELOADING, stop your entire network, ensure all servers were updated and start it again.").printStackTrace();

					else {
						final IncomingMessage incomingMessage = new IncomingMessage(listener, senderUid, serverName, message, data, in, stream);

						listener.setData(data);
						listener.onMessageReceived(incomingMessage);
					}

					handled = true;
					break;
				}

			if (handled)
				event.setCancelled(true);
		}
	}
}