package org.mineacademy.fo.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.region.RegionTracker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Triggered when a player leaves a {@link DiskRegion}.
 *
 * Fired by {@link RegionTracker} when the per-player tick observes a region exit.
 * Not fired on player quit; listeners that need quit cleanup must register their own
 * {@link org.bukkit.event.player.PlayerQuitEvent} handler.
 */
@Getter
@RequiredArgsConstructor
public final class RegionLeaveEvent extends SimpleEvent {

	private static final HandlerList handlers = new HandlerList();

	private final Player player;
	private final DiskRegion region;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
