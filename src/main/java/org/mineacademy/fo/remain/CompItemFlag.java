package org.mineacademy.fo.remain;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * A compatibility wrapper for {@link ItemFlag}
 */
public enum CompItemFlag {

	/**
	 * Setting to show/hide enchants
	 */
	HIDE_ENCHANTS,

	/**
	 * Setting to show/hide Attributes like Damage
	 */
	HIDE_ATTRIBUTES,

	/**
	 * Setting to show/hide the unbreakable State
	 */
	HIDE_UNBREAKABLE,

	/**
	 * Setting to show/hide what the ItemStack can break/destroy
	 */
	HIDE_DESTROYS,

	/**
	 * Setting to show/hide where this ItemStack can be build/placed on
	 */
	HIDE_PLACED_ON,

	/**
	 * Setting to show/hide potion effects on this ItemStack
	 */
	HIDE_POTION_EFFECTS;

	/**
	 * Tries to apply this item flag to the given item, fails silently
	 *
	 * @param item
	 */
	public final void applyTo(ItemStack item) {
		try {
			final ItemMeta meta = item.getItemMeta();
			final ItemFlag bukkitFlag = ItemFlag.valueOf(this.toString());

			meta.addItemFlags(bukkitFlag);

			item.setItemMeta(meta);

		} catch (final Throwable t) {
			// Unsupported MC version
		}
	}
}