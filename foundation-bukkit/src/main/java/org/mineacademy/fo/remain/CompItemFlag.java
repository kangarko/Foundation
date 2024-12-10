package org.mineacademy.fo.remain;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.ReflectionUtil;

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
	HIDE_POTION_EFFECTS,

	/**
	 * Setting to show/hide dyes from colored leather armor
	 */
	HIDE_DYE,

	/**
	 * Setting to show/hide armor trim from armor
	 */
	HIDE_ARMOR_TRIM,

	/**
	 * Setting to show/hide potion effects, book and firework information, map tooltips, patterns of banners
	 */
	HIDE_ADDITIONAL_TOOLTIP,

	/**
	 * Setting to show/hide stored enchants on an item, such as enchantments
	 * on an enchanted book.
	 */
	HIDE_STORED_ENCHANTS;

	/**
	 * Tries to apply this item flag to the given item, fails silently
	 *
	 * @param item
	 */
	public final void applyTo(ItemStack item) {
		try {
			final ItemMeta meta = item.getItemMeta();
			final ItemFlag bukkitFlag = ReflectionUtil.lookupEnum(ItemFlag.class, this.toString());

			meta.addItemFlags(bukkitFlag);

			item.setItemMeta(meta);

		} catch (final Throwable t) {
			// Unsupported MC version
		}
	}

	/**
	 * Checks if the given item has this item flag
	 * Fails silently and returns false
	 * @param item
	 * @return true if the item has this flag
	 */
	public final boolean has(ItemStack item) {
		try {
			if (!item.hasItemMeta())
				return false;

			final ItemMeta meta = item.getItemMeta();
			if (meta == null)
				return false;

			final ItemFlag bukkitFlag = ReflectionUtil.lookupEnum(ItemFlag.class, this.toString());

			return meta.hasItemFlag(bukkitFlag);

		} catch (final Throwable t) {
			// Unsupported MC version
			return false;
		}
	}
}