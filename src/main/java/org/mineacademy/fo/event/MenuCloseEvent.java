package org.mineacademy.fo.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.mineacademy.fo.menu.Menu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Triggered when a menu is closed from a player at the very end of the pipeline.
 */
@Getter
@RequiredArgsConstructor
public final class MenuCloseEvent extends SimpleEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The menu. Use {@link #getDrawer()} to edit how menu items will look like.
	 */
	private final Menu menu;

	/**
	 * The inventory as it looked like when closing
	 */
	private final Inventory inventory;

	/**
	 * The player
	 */
	private final Player player;

	/**
	 * Should we prevent to display this menu?
	 */
	@Setter
	private boolean cancelled;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}