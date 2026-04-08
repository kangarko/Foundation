package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.NonNull;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

final class BungeeServer extends FoundationServer {

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
		return this.server.getPlayers().size();
	}

	@Override
	public Set<UUID> getPlayerUniqueIds() {
		return this.server.getPlayers().stream().map(ProxiedPlayer::getUniqueId).collect(Collectors.toSet());
	}

	@Override
	public boolean isEmpty() {
		return this.server.getPlayers().isEmpty();
	}

	@Override
	public void sendData(final String channel, final byte[] byteArray) {
		this.server.sendData(channel, byteArray);
	}

	@Override
	public String toString() {
		return "BungeeServer{name=" + this.getName() + ",address=" + this.getAddress() + "}";
	}
}
