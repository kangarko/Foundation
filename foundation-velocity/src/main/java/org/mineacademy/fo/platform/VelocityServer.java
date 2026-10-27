package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mineacademy.fo.debug.Debugger;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
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
		int count = 0;

		for (final Player player : this.server.getPlayersConnected()) {
			final Optional<ServerConnection> connection = player.getCurrentServer();

			if (player.isActive() && connection.isPresent() && connection.get().getServer() == this.server)
				count++;
		}

		return count;
	}

	@Override
	public Set<UUID> getPlayerUniqueIds() {
		return this.server.getPlayersConnected().stream().map(Player::getUniqueId).collect(Collectors.toSet());
	}

	@Override
	public boolean isEmpty() {
		return this.getPlayerCount() == 0;
	}

	@Override
	public boolean sendData(final String channel, final byte[] byteArray) {
		final boolean sent = this.server.sendPluginMessage(new LegacyChannelIdentifier(channel), byteArray);

		if (!sent)
			Debugger.debug("proxy", "Dropping " + channel + " data for " + this.getName() + " because no player has finished joining it");

		return sent;
	}

	@Override
	public String toString() {
		return "VelocityServer{name=" + this.getName() + ",address=" + this.getAddress() + "}";
	}
}
