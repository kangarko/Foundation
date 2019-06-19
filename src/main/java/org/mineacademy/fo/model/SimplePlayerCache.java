package org.mineacademy.fo.model;

import org.bukkit.entity.Player;

/**
 * An attempt to standardize way player cache is handled
 * You need to implement own way of storing/saving it in your plugin
 */
public interface SimplePlayerCache {

	/**
	 * Should be called when the cache does not exist and is being created
	 *
	 * @param player
	 */
	void onCreate(Player player);

	/**
	 * Should be called when the cache is being called (you call the getCacheFor(player) method)
	 *
	 * @param player
	 */
	void onCall(Player player);

	/**
	 * Reset the cache data, i.e. on player exit
	 */
	void reset();
}
