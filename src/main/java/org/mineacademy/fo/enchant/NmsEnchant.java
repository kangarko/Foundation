package org.mineacademy.fo.enchant;

import org.bukkit.enchantments.Enchantment;

/**
 * Represents a bridge between Foundation and the server when registering custom enchantments.
 */
public interface NmsEnchant {

	/**
	 * Register the enchantment into the server.
	 *
	 * NB: Some versions such as 1.20+ require you to unfreeze the registry on plugin load first!
	 * (Make sure you freeze it at the bottom of onEnable after you are done).
	 *
	 * NB: We automatically call this method in Foundation for all classes extending {@link SimpleEnchantment}
	 */
	void register();

	/**
	 * Return a Bukkit's Enchantment class for this custom enchantment.
	 *
	 * @return
	 */
	Enchantment toBukkit();
}
