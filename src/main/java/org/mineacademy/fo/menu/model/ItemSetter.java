package org.mineacademy.fo.menu.model;

import org.bukkit.inventory.ItemStack;

/**
 * Utility interface to set items at a certain slot
 */
public interface ItemSetter {

	/**
	 * Set the item at the certain slot
	 *
	 * @param slot the slot
	 * @param item the item
	 */
	void setItem(int slot, ItemStack item);
}
