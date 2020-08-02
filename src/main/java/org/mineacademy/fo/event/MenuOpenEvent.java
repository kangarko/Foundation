package org.mineacademy.fo.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.InventoryDrawer;

/**
 * Triggered when a menu is opened for a player
 */
@Getter
@RequiredArgsConstructor
public final class MenuOpenEvent extends SimpleEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The menu. Use {@link #getDrawer()} to edit how menu items will look like.
	 */
	private final Menu menu;

	/**
	 * The drawer that contains prepared items to render for the player
	 * Use this to edit how the menu will look like
	 */
	private final InventoryDrawer drawer;

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