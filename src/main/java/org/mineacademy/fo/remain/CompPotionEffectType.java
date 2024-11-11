package org.mineacademy.fo.remain;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.ReflectionUtil;

/**
 * Wrapper for 1.20.5 naming changes in PotionEffectType
 */
public final class CompPotionEffectType {

	/*
	 * A helper to convert potion to string.
	 */
	private static final Function<PotionEffectType, String> TO_STRING = type -> {
		try {
			return type.getKey().getKey().replace("minecraft:", "");

		} catch (final NoSuchMethodError err) {
			return type.getName();
		}
	};

	/**
	 * Store all items by name.
	 */
	private static final Map<String, PotionEffectType> byName = new TreeMap<>(Comparator.comparing(name -> name, String.CASE_INSENSITIVE_ORDER));

	/**
	 * Store all item names.
	 */
	private static final Set<String> names = new TreeSet<>(Comparator.comparing(name -> name, String.CASE_INSENSITIVE_ORDER));

	/**
	 * Store all items by type.
	 */
	private static final Set<PotionEffectType> byType = new TreeSet<>(Comparator.comparing(TO_STRING, String.CASE_INSENSITIVE_ORDER));

	/**
	 * Holds the formatted name for each potion i.e. "Mining Fatigue" for SLOW_DIGGING etc.
	 */
	private static final Map<PotionEffectType, String> loreName = new TreeMap<>(Comparator.comparing(TO_STRING, String.CASE_INSENSITIVE_ORDER));

	/**
	 * Increases movement speed.
	 */
	public static final PotionEffectType SPEED = find("SPEED", "SPEED");

	/**
	 * Decreases movement speed.
	 */
	public static final PotionEffectType SLOW = find("SLOW", "SLOWNESS");

	/**
	 * Increases dig speed.
	 */
	public static final PotionEffectType FAST_DIGGING = find("FAST_DIGGING", "HASTE");

	/**
	 * Decreases dig speed.
	 */
	public static final PotionEffectType SLOW_DIGGING = find("SLOW_DIGGING", "MINING_FATIGUE");

	/**
	 * Increases damage dealt.
	 */
	public static final PotionEffectType INCREASE_DAMAGE = find("INCREASE_DAMAGE", "STRENGTH");

	/**
	 * Heals an entity.
	 */
	public static final PotionEffectType HEAL = find("HEAL", "INSTANT_HEALTH");

	/**
	 * Hurts an entity.
	 */
	public static final PotionEffectType HARM = find("HARM", "INSTANT_DAMAGE");

	/**
	 * Increases jump height.
	 */
	public static final PotionEffectType JUMP = find("JUMP", "JUMP_BOOST");

	/**
	 * Warps vision on the client.
	 */
	public static final PotionEffectType CONFUSION = find("CONFUSION", "NAUSEA");

	/**
	 * Regenerates health.
	 */
	public static final PotionEffectType REGENERATION = find("REGENERATION", "REGENERATION");

	/**
	 * Decreases damage dealt to an entity.
	 */
	public static final PotionEffectType DAMAGE_RESISTANCE = find("DAMAGE_RESISTANCE", "RESISTANCE");

	/**
	 * Stops fire damage.
	 */
	public static final PotionEffectType FIRE_RESISTANCE = find("FIRE_RESISTANCE", "FIRE_RESISTANCE");

	/**
	 * Allows breathing underwater.
	 */
	public static final PotionEffectType WATER_BREATHING = find("WATER_BREATHING", "WATER_BREATHING");

	/**
	 * Grants invisibility.
	 */
	public static final PotionEffectType INVISIBILITY = find("INVISIBILITY", "INVISIBILITY");

	/**
	 * Blinds an entity.
	 */
	public static final PotionEffectType BLINDNESS = find("BLINDNESS", "BLINDNESS");

	/**
	 * Allows an entity to see in the dark.
	 */
	public static final PotionEffectType NIGHT_VISION = find("NIGHT_VISION", "NIGHT_VISION");

	/**
	 * Increases hunger.
	 */
	public static final PotionEffectType HUNGER = find("HUNGER", "HUNGER");

	/**
	 * Decreases damage dealt by an entity.
	 */
	public static final PotionEffectType WEAKNESS = find("WEAKNESS", "WEAKNESS");

	/**
	 * Deals damage to an entity over time.
	 */
	public static final PotionEffectType POISON = find("POISON", "POISON");

	/**
	 * Deals damage to an entity over time and gives the health to the
	 * shooter.
	 */
	public static final PotionEffectType WITHER = find("WITHER", "WITHER");

	/**
	 * Increases the maximum health of an entity.
	 */
	public static final PotionEffectType HEALTH_BOOST = find("HEALTH_BOOST", "HEALTH_BOOST");

	/**
	 * Increases the maximum health of an entity with health that cannot be
	 * regenerated, but is refilled every 30 seconds.
	 */
	public static final PotionEffectType ABSORPTION = find("ABSORPTION", "ABSORPTION");

	/**
	 * Increases the food level of an entity each tick.
	 */
	public static final PotionEffectType SATURATION = find("SATURATION", "SATURATION");

	/**
	 * Outlines the entity so that it can be seen from afar.
	 */
	public static final PotionEffectType GLOWING = find("GLOWING", "GLOWING");

	/**
	 * Causes the entity to float into the air.
	 */
	public static final PotionEffectType LEVITATION = find("LEVITATION", "LEVITATION");

	/**
	 * Loot table luck.
	 */
	public static final PotionEffectType LUCK = find("LUCK", "LUCK");

	/**
	 * Loot table unluck.
	 */
	public static final PotionEffectType UNLUCK = find("UNLUCK", "UNLUCK");

	/**
	 * Slows entity fall rate.
	 */
	public static final PotionEffectType SLOW_FALLING = find("SLOW_FALLING", "SLOW_FALLING");

	/**
	 * Effects granted by a nearby conduit. Includes enhanced underwater abilities.
	 */
	public static final PotionEffectType CONDUIT_POWER = find("CONDUIT_POWER", "CONDUIT_POWER");

	/**
	 * Increses underwater movement speed.<br>
	 * Squee'ek uh'k kk'kkkk squeek eee'eek.
	 */
	public static final PotionEffectType DOLPHINS_GRACE = find("DOLPHINS_GRACE", "DOLPHINS_GRACE");

	/**
	 * Triggers a raid when the player enters a village.<br>
	 * oof.
	 */
	public static final PotionEffectType BAD_OMEN = find("BAD_OMEN", "BAD_OMEN");

	/**
	 * Reduces the cost of villager trades.<br>
	 * \o/.
	 */
	public static final PotionEffectType HERO_OF_THE_VILLAGE = find("HERO_OF_THE_VILLAGE", "HERO_OF_THE_VILLAGE");

	/**
	 * Causes the player's vision to dim occasionally.
	 */
	public static final PotionEffectType DARKNESS = find("DARKNESS", "DARKNESS");

	/**
	 * Causes trial spawners to become ominous.
	 */
	public static final PotionEffectType TRIAL_OMEN = find("TRIAL_OMEN");

	/**
	 * Triggers a raid when a player enters a village.
	 */
	public static final PotionEffectType RAID_OMEN = find("RAID_OMEN");

	/**
	 * Emits a wind burst upon death.
	 */
	public static final PotionEffectType WIND_CHARGED = find("WIND_CHARGED");

	/**
	 * Creates cobwebs upon death.
	 */
	public static final PotionEffectType WEAVING = find("WEAVING");

	/**
	 * Causes slimes to spawn upon death.
	 */
	public static final PotionEffectType OOZING = find("OOZING");

	/**
	 * Chance of spawning silverfish when hurt.
	 */
	public static final PotionEffectType INFESTED = find("INFESTED");

	/**
	 * Get the potion by name
	 *
	 * @param name
	 * @return
	 */
	@Nullable
	public static PotionEffectType getByName(String name) {
		return byName.get(name.replace("minecraft:", "").toUpperCase());
	}

	/**
	 * Return all available potion effect types
	 *
	 * @return
	 */
	public static Collection<PotionEffectType> getPotions() {
		return byType;
	}

	/**
	 * Return the name as it appears on the item lore
	 *
	 * @param type
	 * @return
	 */
	@Nullable
	public static String getLoreName(PotionEffectType type) {
		return loreName.get(type);
	}

	/**
	 * Return all available potion effect types
	 *
	 * @return
	 */
	public static Collection<String> getPotionNames() {
		return names;
	}

	/*
	 * Get the potion effect type by its name
	 */
	private static PotionEffectType find(String modernName) {
		return find(null, modernName);
	}

	/*
	 * Get the potion effect type by its name
	 */
	private static PotionEffectType find(String legacyName, String modernName) {
		PotionEffectType type = null;

		try {
			type = ReflectionUtil.getStaticFieldContent(PotionEffectType.class, modernName);

		} catch (final Throwable t) {

			if (legacyName != null)
				try {
					type = ReflectionUtil.getStaticFieldContent(PotionEffectType.class, legacyName);

				} catch (final Throwable tt) {
				}
		}

		if (type != null) {
			try {
				byName.put(type.getKey().getKey().toUpperCase(), type);
			} catch (final NoSuchMethodError err) {
			}

			byName.put(modernName, type);

			if (legacyName != null)
				byName.put(legacyName, type);

			names.add(modernName);

			byType.add(type);
			loreName.put(type, ChatUtil.capitalizeFully(modernName));
		}

		return type;
	}

	static {
		for (final PotionEffectType type : ReflectionUtil.getEnumValues(PotionEffectType.class)) {
			if (type == null)
				continue; // wtf 1.8.8

			String name;

			try {
				name = type.getKey().getKey().toUpperCase();

			} catch (final NoSuchMethodError err) {
				name = type.getName().toUpperCase();
			}

			byName.put(name, type);
			names.add(name);
			loreName.put(type, ChatUtil.capitalizeFully(name));

			byType.add(type);
		}
	}
}