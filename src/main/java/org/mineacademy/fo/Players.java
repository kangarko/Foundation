package org.mineacademy.fo;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Players {

	public Optional<Player> find(@NonNull final String name) {
		return Optional.ofNullable(Bukkit.getPlayer(name));
	}

	public Optional<Player> find(@NonNull final UUID uuid) {
		return Optional.ofNullable(Bukkit.getPlayer(uuid));
	}

	public Optional<OfflinePlayer> findOffline(@NonNull final String name) {
		// Can be null. @NonNull is not correct here
		return Optional.ofNullable(Bukkit.getOfflinePlayer(name));
	}

	public Optional<OfflinePlayer> findOffline(@NonNull final UUID uuid) {
		// Can be null. @NonNull is not correct here
		return Optional.ofNullable(Bukkit.getOfflinePlayer(uuid));
	}
}
