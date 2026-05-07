package org.mineacademy.fo.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.region.RegionTracker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Triggered when a player enters a {@link DiskRegion}.
 *
 * Fired by {@link RegionTracker} on its 1Hz tick and on player join.
 */
@Getter
@RequiredArgsConstructor
public final class RegionEnterEvent extends SimpleEvent {

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
