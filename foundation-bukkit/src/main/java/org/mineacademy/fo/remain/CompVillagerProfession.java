package org.mineacademy.fo.remain;

import org.bukkit.entity.Villager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Compatible enum for villager professions where we convert to the
 * closest approximate (based on my non-native English understanding :))
 * to pre Village And Pillage update.
 */
@RequiredArgsConstructor
public enum CompVillagerProfession {

	NONE("FARMER"),

	/**
	 * Armorer profession. Wears a black apron. Armorers primarily trade for
	 * iron armor, chainmail armor, and sometimes diamond armor.
	 */
	ARMORER("BLACKSMITH"),

	/**
	 * Butcher profession. Wears a white apron. Butchers primarily trade for
	 * raw and cooked food.
	 */
	BUTCHER("BUTCHER"),

	/**
	 * Cartographer profession. Wears a white robe. Cartographers primarily
	 * trade for explorer maps and some paper.
	 */
	CARTOGRAPHER("FARMER"),

	/**
	 * Cleric profession. Wears a purple robe. Clerics primarily trade for
	 * rotten flesh, gold ingot, redstone, lapis, ender pearl, glowstone,
	 * and bottle o' enchanting.
	 */
	CLERIC("FARMER"),

	/**
	 * Farmer profession. Wears a brown robe. Farmers primarily trade for
	 * food-related items.
	 */
	FARMER("FARMER"),

	/**
	 * Fisherman profession. Wears a brown robe. Fisherman primarily trade
	 * for fish, as well as possibly selling string and/or coal.
	 */
	FISHERMAN("FARMER"),

	/**
	 * Fletcher profession. Wears a brown robe. Fletchers primarily trade
	 * for string, bows, and arrows.
	 */
	FLETCHER("PRIEST"),

	/**
	 * Leatherworker profession. Wears a white apron. Leatherworkers
	 * primarily trade for leather, and leather armor, as well as saddles.
	 */
	LEATHERWORKER("BUTCHER"),

	/**
	 * Librarian profession. Wears a white robe. Librarians primarily trade
	 * for paper, books, and enchanted books.
	 */
	LIBRARIAN("LIBRARIAN"),

	/**
	 * Mason profession.
	 */
	MASON("FARMER"),

	/**
	 * Nitwit profession. Wears a green apron, cannot trade. Nitwit
	 * villagers do not do anything. They do not have any trades by default.
	 */
	NITWIT("FARMER"),

	/**
	 * Sheperd profession. Wears a brown robe. Shepherds primarily trade for
	 * wool items, and shears.
	 */
	SHEPHERD("FARMER"),

	/**
	 * Toolsmith profession. Wears a black apron. Tool smiths primarily
	 * trade for iron and diamond tools.
	 */
	TOOLSMITH("BLACKSMITH"),

	/**
	 * Weaponsmith profession. Wears a black apron. Weapon smiths primarily
	 * trade for iron and diamond weapons, sometimes enchanted.
	 */
	WEAPONSMITH("BLACKSMITH");

	@Getter
	private final String legacyName;

	/**
	 * Return the villager profession in Bukkit enum
	 *
	 * @return
	 */
	public Villager.Profession toBukkit() {
		try {
			return Villager.Profession.valueOf(this.name());
		} catch (Throwable t) {
			return Villager.Profession.valueOf(this.legacyName);
		}
	}

	/**
	 * Apply this profession to the given Villager
	 *
	 * @param villager
	 */
	public void apply(Villager villager) {
		villager.setProfession(this.toBukkit());
	}
}