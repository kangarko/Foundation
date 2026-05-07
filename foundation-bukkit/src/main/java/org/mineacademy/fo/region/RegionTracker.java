package org.mineacademy.fo.region;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.mineacademy.fo.event.RegionEnterEvent;
import org.mineacademy.fo.event.RegionLeaveEvent;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Polls online players once per second and fires {@link RegionEnterEvent} and
 * {@link RegionLeaveEvent} as players cross {@link DiskRegion} borders.
 *
 * Folia-aware: per-player work runs on the player's region thread via
 * {@link Remain#runEntityTask(org.bukkit.entity.Entity, int, Runnable)}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegionTracker implements Listener {

	private static final RegionTracker instance = new RegionTracker();

	private final Map<UUID, Set<String>> playerRegions = new ConcurrentHashMap<>();

	/**
	 * Listener registration happens immediately so player join/quit are tracked
	 * from the moment of the call. The initial silent seed and the polling tick
	 * are deferred to give {@link DiskRegion#loadRegions()} time to populate.
	 */
	public static void start() {
		Platform.registerEvents(instance);

		Platform.runTask(() -> {
			for (final Player online : Remain.getOnlinePlayers())
				Remain.runEntityTask(online, 0, () -> instance.recordRegions(online));

			Platform.runTaskTimer(20, 20, instance::tick);
		});
	}

	/**
	 * Returns an immutable snapshot of the region file names the given player is currently inside.
	 * Empty set when the player is in no tracked region or the tracker is not running.
	 *
	 * @param player
	 * @return
	 */
	public static Set<String> getRegionsFor(final Player player) {
		final Set<String> tracked = instance.playerRegions.get(player.getUniqueId());

		return tracked == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(tracked));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		Remain.runEntityTask(player, 0, () -> this.tickPlayer(player));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(final PlayerQuitEvent event) {
		this.playerRegions.remove(event.getPlayer().getUniqueId());
	}

	private void tick() {
		for (final Player online : Remain.getOnlinePlayers())
			Remain.runEntityTask(online, 0, () -> this.tickPlayer(online));
	}

	private void tickPlayer(final Player player) {
		if (!player.isOnline())
			return;

		final UUID uuid = player.getUniqueId();
		final Set<String> previous = this.playerRegions.getOrDefault(uuid, Collections.emptySet());
		final Set<String> current = this.recordRegions(player);

		for (final String name : current)
			if (!previous.contains(name)) {
				final DiskRegion region = DiskRegion.findRegion(name);

				if (region != null)
					Platform.callEvent(new RegionEnterEvent(player, region));
			}

		for (final String name : previous)
			if (!current.contains(name)) {
				final DiskRegion region = DiskRegion.findRegion(name);

				if (region != null)
					Platform.callEvent(new RegionLeaveEvent(player, region));
			}
	}

	private Set<String> recordRegions(final Player player) {
		final Set<String> current = new LinkedHashSet<>();

		for (final DiskRegion region : DiskRegion.findRegions(player.getLocation()))
			current.add(region.getFileName());

		this.playerRegions.put(player.getUniqueId(), current);

		return current;
	}
}
