package org.mineacademy.fo.model;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A store of how much health should each individual food give.
 *
 * The player has 20 health parts so that 10 health points will give him
 * half of the total amount of his health shown on the bottom left bar.
 *
 * The food must be the same name as {@link CompMaterial}
 */
@RequiredArgsConstructor
public enum Food {
	APPLE(2),
	BAKED_POTATO(5),
	BEEF(4),
	BEETROOT(1),
	BEETROOT_SOUP(6),
	BREAD(5),
	CAKE(2),
	CARROT(3),
	CHICKEN(6),
	CHORUS_FRUIT(4),
	COD(3),
	COOKED_BEEF(8),
	COOKED_CHICKEN(6),
	COOKED_COD(5),
	COOKED_MUTTON(6),
	COOKED_PORKCHOP(8),
	COOKED_RABBIT(5),
	COOKED_SALMON(6),
	COOKIE(2),
	DRIED_KELP(1),
	ENCHANTED_GOLDEN_APPLE(4),
	GLOW_BERRIES(3),
	GOLDEN_APPLE(4),
	GOLDEN_CARROT(6),
	HONEY_BOTLE(4),
	MELON_SLICE(2),
	MUSHROOM_STEW(6),
	MUTTON(2),
	POISONOUS_POTATO(2),
	PORKCHOP(4),
	POTATO(1),
	PUFFERFISH(1),
	PUMPKIN_PIE(8),
	RABBIT(3),
	RABBIT_STEW(10),
	ROTTEN_FLESH(4),
	SALMON(2),
	SPIDER_EYE(2),
	SUSPICIOUS_STEW(6),
	SWEET_BERRIES(2),
	TROPICAL_FISH(1);

	/**
	 * How much health to give from this food?
	 */
	@Getter
	private final int healthPoints;

	/**
	 * Attempts to find the food item for the particular item stack
	 *
	 * @param item
	 * @return the item, or null if none
	 */
	public static Food getFood(ItemStack item) {
		final CompMaterial material = CompMaterial.fromMaterial(item.getType());

		return ReflectionUtil.lookupEnumSilent(Food.class, material.toString());
	}
}