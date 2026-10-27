package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.mineacademy.fo.debug.Debugger;

import lombok.NonNull;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

final class BungeeServer extends FoundationServer {

	private static final Map<UUID, Server> joinedConnections = new ConcurrentHashMap<>();

	/**
	 * The server we are wrapping
	 */
	private final ServerInfo server;

	/**
	 * Create a new server instance
	 *
	 * @param server
	 */
	BungeeServer(@NonNull final ServerInfo server) {
		this.server = server;
	}

	@Override
	public InetSocketAddress getAddress() {
		return this.server.getAddress();
	}

	@Override
	public String getName() {
		return this.server.getName();
	}

	@Override
	public int getPlayerCount() {
		int count = 0;

		for (final ProxiedPlayer player : this.server.getPlayers())
			if (this.findJoinedConnection(player) != null)
				count++;

		return count;
	}

	@Override
	public Set<UUID> getPlayerUniqueIds() {
		return this.server.getPlayers().stream().map(ProxiedPlayer::getUniqueId).collect(Collectors.toSet());
	}

	@Override
	public boolean isEmpty() {
		return this.getPlayerCount() == 0;
	}

	@Override
	public boolean sendData(final String channel, final byte[] byteArray) {
		for (final ProxiedPlayer player : this.server.getPlayers()) {
			final Server connection = this.findJoinedConnection(player);

			if (connection != null) {
				connection.sendData(channel, byteArray);

				return true;
			}
		}

		Debugger.debug("proxy", "Dropping " + channel + " data for " + this.getName() + " because no player has finished joining it");

		return false;
	}

	private Server findJoinedConnection(final ProxiedPlayer player) {
		final Server connection = player.getServer();

		if (connection == null || !connection.isConnected() || connection != joinedConnections.get(player.getUniqueId()) || !connection.getInfo().getName().equals(this.getName()))
			return null;

		return connection;
	}

	@Override
	public String toString() {
		return "BungeeServer{name=" + this.getName() + ",address=" + this.getAddress() + "}";
	}

	static void markJoined(final ProxiedPlayer player, final Server connection) {
		if (!player.isConnected())
			return;

		joinedConnections.put(player.getUniqueId(), connection);
	}

	static void forgetJoined(final ProxiedPlayer player) {
		joinedConnections.remove(player.getUniqueId());
	}
}
