package org.mineacademy.fo.enchant;

public enum SimpleEnchantmentRarity {

	COMMON(10),
	UNCOMMON(5),
	RARE(2),
	VERY_RARE(1);

	private final int weight;

	SimpleEnchantmentRarity(int weight) {
		this.weight = weight;
	}

	/**
	 * Gets the weight for the rarity.
	 *
	 * @return the weight
	 */
	public int getWeight() {
		return weight;
	}
}
