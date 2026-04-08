package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BukkitServer extends FoundationServer {

	@Getter
	private static final BukkitServer instance = new BukkitServer();

	@Override
	public InetSocketAddress getAddress() {
		return new InetSocketAddress(Bukkit.getIp(), Bukkit.getPort());
	}

	@Override
	public String getName() {
		return Platform.getCustomServerName();
	}

	@Override
	public int getPlayerCount() {
		return Remain.getOnlinePlayers().size();
	}

	@Override
	public Set<UUID> getPlayerUniqueIds() {
		return Remain.getOnlinePlayers().stream().map(Player::getUniqueId).collect(Collectors.toSet());
	}

	@Override
	public boolean isEmpty() {
		return Remain.getOnlinePlayers().isEmpty();
	}

	@Override
	public void sendData(final String channel, final byte[] byteArray) {
		Platform.sendPluginMessage(null, channel, byteArray);
	}

	@Override
	public String toString() {
		return "BukkitServer{name=" + this.getName() + ",address=" + this.getAddress() + "}";
	}
}
