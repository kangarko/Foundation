package org.mineacademy.fo.remain;

import org.bukkit.entity.Villager;
import org.mineacademy.fo.ReflectionUtil;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Compatible enum for villager types.
 */
@RequiredArgsConstructor
public enum CompVillagerType {

	DESERT("DESERT"),
	JUNGLE("JUNGLE"),
	PLAINS("PLAINS"),
	SAVANNA("SAVANNA"),
	SNOW("SNOW"),
	SWAMP("SWAMP"),
	TAIGA("TAIGA");

	@Getter
	private final String enumName;

	/**
	 * Return the villager profession in Bukkit enum
	 *
	 * @return
	 */
	public Villager.Type toBukkit() {
		return ReflectionUtil.lookupEnum(Villager.Type.class, this.enumName);
	}

	/**
	 * Apply this profession to the given Villager
	 *
	 * @param villager
	 */
	public void apply(Villager villager) {
		villager.setVillagerType(this.toBukkit());
	}

	/**
	 * Converts the name into the profession
	 *
	 * @param name
	 * @return
	 */
	public static Villager.Type convertNameToBukkit(String name) {
		for (final CompVillagerType type : values())
			if (type.getEnumName().equalsIgnoreCase(name))
				return type.toBukkit();

		return ReflectionUtil.lookupEnum(Villager.Type.class, name);
	}
}