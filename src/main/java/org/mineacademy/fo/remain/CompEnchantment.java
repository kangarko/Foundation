package org.mineacademy.fo.remain;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;

/**
 * Provides a wrapper for all enchantments in Minecraft.
 *
 * Some enchants might be null on old Minecraft versions.
 */
public final class CompEnchantment {

	/**
	 * Provides protection against environmental damage
	 */
	public static final Enchantment PROTECTION_ENVIRONMENTAL = find(0, "PROTECTION_ENVIRONMENTAL", "protection");

	/**
	 * Provides protection against fire damage
	 */
	public static final Enchantment PROTECTION_FIRE = find(1, "PROTECTION_FIRE", "fire_protection");

	/**
	 * Provides protection against fall damage
	 */
	public static final Enchantment PROTECTION_FALL = find(2, "PROTECTION_FALL", "feather_falling");

	/**
	 * Provides protection against explosive damage
	 */
	public static final Enchantment PROTECTION_EXPLOSIONS = find(3, "PROTECTION_EXPLOSIONS", "blast_protection");

	/**
	 * Provides protection against projectile damage
	 */
	public static final Enchantment PROTECTION_PROJECTILE = find(4, "PROTECTION_PROJECTILE", "projectile_protection");

	/**
	 * Decreases the rate of air loss whilst underwater
	 */
	public static final Enchantment OXYGEN = find(5, "OXYGEN", "respiration");

	/**
	 * Increases the speed at which a player may mine underwater
	 */
	public static final Enchantment WATER_WORKER = find(6, "WATER_WORKER", "aqua_affinity");

	/**
	 * Damages the attacker
	 */
	@Nullable
	public static final Enchantment THORNS = find(7, "THORNS", "thorns");

	/**
	 * Increases walking speed while in water
	 */
	@Nullable
	public static final Enchantment DEPTH_STRIDER = find(8, "DEPTH_STRIDER", "depth_strider");

	/**
	 * Freezes any still water adjacent to ice / frost which player is walking on
	 */
	@Nullable
	public static final Enchantment FROST_WALKER = find(9, "FROST_WALKER", "frost_walker");

	/**
	 * Item cannot be removed
	 */
	@Nullable
	public static final Enchantment BINDING_CURSE = find(10, "BINDING_CURSE", "binding_curse");

	/**
	 * Increases damage against all targets
	 */
	public static final Enchantment DAMAGE_ALL = find(16, "DAMAGE_ALL", "sharpness");

	/**
	 * Increases damage against undead targets
	 */
	public static final Enchantment DAMAGE_UNDEAD = find(17, "DAMAGE_UNDEAD", "smite");

	/**
	 * Increases damage against arthropod targets
	 */
	public static final Enchantment DAMAGE_ARTHROPODS = find(18, "DAMAGE_ARTHROPODS", "bane_of_arthropods");

	/**
	 * All damage to other targets will knock them back when hit
	 */
	public static final Enchantment KNOCKBACK = find(19, "KNOCKBACK", "knockback");

	/**
	 * When attacking a target, has a chance to set them on fire
	 */
	public static final Enchantment FIRE_ASPECT = find(20, "FIRE_ASPECT", "fire_aspect");

	/**
	 * Provides a chance of gaining extra loot when killing monsters
	 */
	public static final Enchantment LOOT_BONUS_MOBS = find(21, "LOOT_BONUS_MOBS", "looting");

	/**
	 * Increases damage against targets when using a sweep attack
	 */
	@Nullable
	public static final Enchantment SWEEPING_EDGE = find(22, "SWEEPING_EDGE", "sweeping");

	/**
	 * Increases the rate at which you mine/dig
	 */
	public static final Enchantment DIG_SPEED = find(32, "DIG_SPEED", "efficiency");

	/**
	 * Allows blocks to drop themselves instead of fragments (for example,
	 * stone instead of cobblestone)
	 */
	public static final Enchantment SILK_TOUCH = find(33, "SILK_TOUCH", "silk_touch");

	/**
	 * Decreases the rate at which a tool looses durability
	 */
	public static final Enchantment DURABILITY = find(34, "DURABILITY", "unbreaking");

	/**
	 * Provides a chance of gaining extra loot when destroying blocks
	 */
	public static final Enchantment LOOT_BONUS_BLOCKS = find(35, "LOOT_BONUS_BLOCKS", "fortune");

	/**
	 * Provides extra damage when shooting arrows from bows
	 */
	public static final Enchantment ARROW_DAMAGE = find(48, "ARROW_DAMAGE", "power");

	/**
	 * Provides a knockback when an entity is hit by an arrow from a bow
	 */
	public static final Enchantment ARROW_KNOCKBACK = find(49, "ARROW_KNOCKBACK", "punch");

	/**
	 * Sets entities on fire when hit by arrows shot from a bow
	 */
	public static final Enchantment ARROW_FIRE = find(50, "ARROW_FIRE", "flame");

	/**
	 * Provides infinite arrows when shooting a bow
	 */
	public static final Enchantment ARROW_INFINITE = find(51, "ARROW_INFINITE", "infinity");

	/**
	 * Decreases odds of catching worthless junk
	 */
	@Nullable
	public static final Enchantment LUCK = find(61, "LUCK", "luck_of_the_sea");

	/**
	 * Increases rate of fish biting your hook
	 */
	@Nullable
	public static final Enchantment LURE = find(62, "LURE", "lure");

	/**
	 * Causes a thrown trident to return to the player who threw it
	 */
	@Nullable
	public static final Enchantment LOYALTY = find(-1, "LOYALTY", "loyalty");

	/**
	 * Deals more damage to mobs that live in the ocean
	 */
	@Nullable
	public static final Enchantment IMPALING = find(-1, "IMPALING", "impaling");

	/**
	 * When it is rainy, launches the player in the direction their trident is thrown
	 */
	@Nullable
	public static final Enchantment RIPTIDE = find(-1, "RIPTIDE", "riptide");

	/**
	 * Strikes lightning when a mob is hit with a trident if conditions are
	 * stormy
	 */
	@Nullable
	public static final Enchantment CHANNELING = find(-1, "CHANNELING", "channeling");

	/**
	 * Shoot multiple arrows from crossbows
	 */
	@Nullable
	public static final Enchantment MULTISHOT = find(-1, "MULTISHOT", "multishot");

	/**
	 * Charges crossbows quickly
	 */
	@Nullable
	public static final Enchantment QUICK_CHARGE = find(-1, "QUICK_CHARGE", "quick_charge");

	/**
	 * Crossbow projectiles pierce entities
	 */
	@Nullable
	public static final Enchantment PIERCING = find(-1, "PIERCING", "piercing");

	/**
	 * Allows mending the item using experience orbs
	 */
	@Nullable
	public static final Enchantment MENDING = find(70, "MENDING", "mending");

	/**
	 * Item disappears instead of dropping
	 */
	@Nullable
	public static final Enchantment VANISHING_CURSE = find(71, "VANISHING_CURSE", "vanishing_curse");

	/**
	 * Walk quicker on soul blocks
	 */
	@Nullable
	public static final Enchantment SOUL_SPEED = find(-1, "SOUL_SPEED", "soul_speed");

	/**
	 * Walk quicker while sneaking
	 */
	@Nullable
	public static final Enchantment SWIFT_SNEAK = find(-1, "SWIFT_SNEAK", "swift_sneak");

	/*
	 * Find the enchantment by ID or name
	 */
	private static Enchantment find(int id, String oldName, String key) {
		Enchantment enchantment;

		try {
			enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key));

		} catch (final NoClassDefFoundError | NoSuchMethodError ex) {
			enchantment = Enchantment.getByName(oldName);

			if (enchantment == null && MinecraftVersion.olderThan(V.v1_13)) {
				final Method getById = ReflectionUtil.getMethod(Enchantment.class, "getById", int.class);

				enchantment = ReflectionUtil.invokeStatic(getById, id);
			}

		}

		return enchantment;
	}
}
