package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import lombok.NonNull;

final class VelocityServer implements FoundationServer {

	/**
	 * The server we are wrapping
	 */
	private final RegisteredServer server;

	/**
	 * Create a new server
	 *
	 * @param server
	 */
	VelocityServer(@NonNull RegisteredServer server) {
		this.server = server;
	}

	@Override
	public InetSocketAddress getAddress() {
		return this.server.getServerInfo().getAddress();
	}

	@Override
	public String getName() {
		return this.server.getServerInfo().getName();
	}

	@Override
	public List<FoundationPlayer> getPlayers() {
		return this.server.getPlayersConnected().stream()
				.filter(Player::isActive)
				.map(VelocityPlayer::new)
				.collect(Collectors.toList());
	}

	@Override
	public boolean isEmpty() {
		return this.server.getPlayersConnected().isEmpty();
	}

	@Override
	public void sendData(String channel, byte[] byteArray) {
		this.server.sendPluginMessage(new LegacyChannelIdentifier(channel), byteArray);
	}
}
