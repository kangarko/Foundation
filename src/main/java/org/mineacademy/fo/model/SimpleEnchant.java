package org.mineacademy.fo.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.enchantments.Enchantment;

/**
 * A simple wrapper for enchants
 */
@Getter
@RequiredArgsConstructor
public final class SimpleEnchant {

	/**
	 * The enchantment
	 */
	private final Enchantment enchant;

	/**
	 * The level
	 */
	private final int level;

	/**
	 * Creates a new enchantment of level 1
	 *
	 * @param enchant the enchantment
	 */
	public SimpleEnchant(Enchantment enchant) {
		this(enchant, 1);
	}
}