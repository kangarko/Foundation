package org.mineacademy.fo.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.mineacademy.fo.menu.Menu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Triggered when a menu is opened for a player
 */
@Getter
@RequiredArgsConstructor
public final class MenuOpenEvent extends SimpleEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The menu
	 */
	private final Menu menu;

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