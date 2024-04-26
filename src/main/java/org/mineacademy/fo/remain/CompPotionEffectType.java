package org.mineacademy.fo.remain;

import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.ReflectionUtil;

/**
 * Wrapper for 1.20.5 naming changes in PotionEffectType
 */
public final class CompPotionEffectType {

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

	/*
	 * Get the potion effect type by its name
	 */
	private static PotionEffectType find(String legacyName, String modernName) {

		try {
			return ReflectionUtil.getStaticFieldContent(PotionEffectType.class, modernName);

		} catch (final Throwable t) {
			try {
				return ReflectionUtil.getStaticFieldContent(PotionEffectType.class, legacyName);

			} catch (Throwable tt) {
				// Unavailable for this MC version
				return null;
			}
		}
	}

}