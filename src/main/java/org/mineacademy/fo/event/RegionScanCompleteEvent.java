package org.mineacademy.fo.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.mineacademy.fo.model.OfflineRegionScanner;

/**
 * Triggered when {@link OfflineRegionScanner} finishes scanning all offline regions on your disk
 */
@Getter
@RequiredArgsConstructor
public final class RegionScanCompleteEvent extends SimpleEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The world this scanner operated in
	 */
	private final World world;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}