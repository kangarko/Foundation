package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.List;

import org.bukkit.Bukkit;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BukkitServer implements FoundationServer {

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
	public List<FoundationPlayer> getPlayers() {
		return Platform.getOnlinePlayers();
	}

	@Override
	public boolean isEmpty() {
		return Remain.getOnlinePlayers().isEmpty();
	}

	@Override
	public void sendData(final String channel, final byte[] byteArray) {
		Platform.sendPluginMessage(null, channel, byteArray);
	}
}
