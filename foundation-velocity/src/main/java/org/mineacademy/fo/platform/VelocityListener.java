package org.mineacademy.fo.platform;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;
import org.mineacademy.fo.proxy.message.IncomingMessage;
import org.mineacademy.fo.remain.Remain;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.UuidUtils;

/**
 * A listener for handling incoming plugin messages
 *
 * @deprecated internal use only
 */
@Deprecated
public final class VelocityListener {

	/**
	 * Handle the received message automatically if it matches our tag
	 *
	 * @param event
	 */
	@Subscribe
	public void onPluginMessage(PluginMessageEvent event) {
		synchronized (ProxyListener.DEFAULT_CHANNEL) {
			final ChannelMessageSource sender = event.getSource();
			final byte[] data = event.getData();

			if (event.getResult() == ForwardResult.handled())
				return;

			if (!event.getIdentifier().getId().equals(ProxyListener.DEFAULT_CHANNEL) && !event.getIdentifier().getId().equals("bungeecord:main"))
				return;

			// Check if a player is not trying to send us a fake message
			if (!(sender instanceof ServerConnection))
				return;

			final ServerConnection connection = (ServerConnection) event.getSource();
			final ByteArrayInputStream stream = new ByteArrayInputStream(data);
			final ByteArrayDataInput in = ByteStreams.newDataInput(stream);

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

			// Credits: https://github.com/VelocityPowered/BungeeQuack/blob/master/src/main/java/com/velocitypowered/bungeequack/BungeeQuack.java
			// The reason for this ugly patch is that the above listener is ignored completely when velocity handles bungee commands :/
			//
			// https://github.com/kangarko/ChatControl-Red/issues/2673
			final ProxyServer proxy = VelocityPlugin.getServer();
			final ByteArrayDataOutput out = ByteStreams.newDataOutput();
			boolean found = true;

			if (subChannel.equals("ForwardToPlayer")) {
				proxy.getPlayer(in.readUTF())
						.ifPresent(player -> player.sendPluginMessage(event.getIdentifier(), prepareForwardMessage(in)));

			} else if (subChannel.equals("Forward")) {
				final String target = in.readUTF();
				final byte[] toForward = prepareForwardMessage(in);

				if (target.equals("ALL")) {
					for (final RegisteredServer other : Remain.getServers())
						other.sendPluginMessage(event.getIdentifier(), toForward);

				} else
					proxy.getServer(target).ifPresent(conn -> conn.sendPluginMessage(event.getIdentifier(), toForward));

			} else if (subChannel.equals("Connect")) {
				final Optional<RegisteredServer> info = proxy.getServer(in.readUTF());
				info.ifPresent(serverInfo -> connection.getPlayer().createConnectionRequest(serverInfo).fireAndForget());

			} else if (subChannel.equals("ConnectOther"))
				proxy.getPlayer(in.readUTF()).ifPresent(otherPlayer -> {
					final Optional<RegisteredServer> info = proxy.getServer(in.readUTF());
					info.ifPresent(serverInfo -> otherPlayer.createConnectionRequest(serverInfo).fireAndForget());
				});

			else if (subChannel.equals("IP")) {
				out.writeUTF("IP");
				out.writeUTF(connection.getPlayer().getRemoteAddress().getHostString());
				out.writeInt(connection.getPlayer().getRemoteAddress().getPort());

			} else if (subChannel.equals("PlayerCount")) {
				final String target = in.readUTF();

				if (target.equals("ALL")) {
					out.writeUTF("PlayerCount");
					out.writeUTF("ALL");
					out.writeInt(proxy.getPlayerCount());
				} else
					proxy.getServer(target).ifPresent(rs -> {
						final int playersOnServer = rs.getPlayersConnected().size();
						out.writeUTF("PlayerCount");
						out.writeUTF(rs.getServerInfo().getName());
						out.writeInt(playersOnServer);
					});

			} else if (subChannel.equals("PlayerList")) {
				final String target = in.readUTF();

				if (target.equals("ALL")) {
					out.writeUTF("PlayerList");
					out.writeUTF("ALL");
					out.writeUTF(Remain.getOnlinePlayers(true).stream().map(Player::getUsername).collect(Collectors.joining(", ")));

				} else
					proxy.getServer(target).ifPresent(info -> {
						final String playersOnServer = info.getPlayersConnected().stream().map(Player::getUsername).collect(Collectors.joining(", "));
						out.writeUTF("PlayerList");
						out.writeUTF(info.getServerInfo().getName());
						out.writeUTF(playersOnServer);
					});

			} else if (subChannel.equals("GetServers")) {
				out.writeUTF("GetServers");
				out.writeUTF(Remain.getServers().stream().map(s -> s.getServerInfo().getName()).collect(Collectors.joining(", ")));

			} else if (subChannel.equals("Message")) {
				final String target = in.readUTF();
				final String message = in.readUTF();

				if (target.equals("ALL"))
					for (final Player player : Remain.getOnlinePlayers(false))
						Common.tell(player, message);

				else
					proxy.getPlayer(target).ifPresent(player -> {
						Common.tell(player, message);
					});

			} else if (subChannel.equals("GetServer")) {
				out.writeUTF("GetServer");
				out.writeUTF(connection.getServerInfo().getName());

			} else if (subChannel.equals("UUID")) {
				out.writeUTF("UUID");
				out.writeUTF(UuidUtils.toUndashed(connection.getPlayer().getUniqueId()));

			} else if (subChannel.equals("UUIDOther"))
				proxy.getPlayer(in.readUTF()).ifPresent(player -> {
					out.writeUTF("UUIDOther");
					out.writeUTF(player.getUsername());
					out.writeUTF(UuidUtils.toUndashed(player.getUniqueId()));
				});

			else if (subChannel.equals("ServerIP"))
				proxy.getServer(in.readUTF()).ifPresent(info -> {
					out.writeUTF("ServerIP");
					out.writeUTF(info.getServerInfo().getName());
					out.writeUTF(info.getServerInfo().getAddress().getHostString());
					out.writeShort(info.getServerInfo().getAddress().getPort());
				});

			else if (subChannel.equals("KickPlayer"))
				proxy.getPlayer(in.readUTF()).ifPresent(player -> {
					final String kickReason = in.readUTF();

					player.disconnect(SimpleComponent.fromSection(kickReason).toAdventure(null));
				});

			else
				found = false;

			if (found) {
				final byte[] outData = out.toByteArray();

				if (outData.length > 0)
					connection.sendPluginMessage(event.getIdentifier(), outData);

				handled = true;
			}

			if (handled)
				event.setResult(PluginMessageEvent.ForwardResult.handled());
		}
	}

	// Credits: https://github.com/VelocityPowered/BungeeQuack/blob/master/src/main/java/com/velocitypowered/bungeequack/BungeeQuack.java
	private byte[] prepareForwardMessage(ByteArrayDataInput in) {
		final String channel = in.readUTF();
		final short messageLength = in.readShort();
		final byte[] message = new byte[messageLength];
		in.readFully(message);

		final ByteArrayDataOutput forwarded = ByteStreams.newDataOutput();
		forwarded.writeUTF(channel);
		forwarded.writeShort(messageLength);
		forwarded.write(message);
		return forwarded.toByteArray();
	}
}