package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import lombok.NonNull;

final class VelocityServer extends FoundationServer {

	/**
	 * The server we are wrapping
	 */
	private final RegisteredServer server;

	/**
	 * Create a new server
	 *
	 * @param server
	 */
	VelocityServer(@NonNull final RegisteredServer server) {
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
	public int getPlayerCount() {
		return this.server.getPlayersConnected().size();
	}

	@Override
	public Set<UUID> getPlayerUniqueIds() {
		return this.server.getPlayersConnected().stream().map(Player::getUniqueId).collect(Collectors.toSet());
	}

	@Override
	public boolean isEmpty() {
		return this.server.getPlayersConnected().isEmpty();
	}

	@Override
	public void sendData(final String channel, final byte[] byteArray) {
		this.server.sendPluginMessage(new LegacyChannelIdentifier(channel), byteArray);
	}

	@Override
	public String toString() {
		return "VelocityServer{name=" + this.getName() + ",address=" + this.getAddress() + "}";
	}
}
