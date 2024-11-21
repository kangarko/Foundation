package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

final class BungeeServer implements FoundationServer {

	/**
	 * The server we are wrapping
	 */
	private final ServerInfo server;

	/**
	 * Create a new server instance
	 *
	 * @param server
	 */
	BungeeServer(@NonNull ServerInfo server) {
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
	public List<FoundationPlayer> getPlayers() {
		return this.server.getPlayers().stream()
				.filter(ProxiedPlayer::isConnected)
				.map(BungeePlayer::new)
				.collect(Collectors.toList());
	}

	@Override
	public boolean isEmpty() {
		return this.server.getPlayers().isEmpty();
	}

	@Override
	public void sendData(String channel, byte[] byteArray) {
		this.server.sendData(channel, byteArray);
	}
}
