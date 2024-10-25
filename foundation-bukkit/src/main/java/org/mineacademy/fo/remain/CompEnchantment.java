package org.mineacademy.fo.remain;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;

/**
 * Provides a wrapper for all enchantments in Minecraft.
 *
 * Some enchants might be null on old Minecraft versions.
 */
public final class CompEnchantment {

	/*
	 * A helper to convert enchant to string.
	 */
	private static final Function<Enchantment, String> TO_STRING = type -> {
		try {
			return type.getKey().getKey().replace("minecraft:", "");

		} catch (final NoSuchMethodError err) {
			return type.getName();
		}
	};

	/**
	 * Store all items by name.
	 */
	private static final Map<String, Enchantment> byName = new TreeMap<>(Comparator.comparing(name -> name, String.CASE_INSENSITIVE_ORDER));

	/**
	 * Store all item names.
	 */
	private static final Set<String> names = new TreeSet<>(Comparator.comparing(name -> name, String.CASE_INSENSITIVE_ORDER));

	/**
	 * Store all items by type.
	 */
	private static final Set<Enchantment> byType = new TreeSet<>(Comparator.comparing(TO_STRING, String.CASE_INSENSITIVE_ORDER));

	/**
	 * Holds the formatted name for each enchant i.e. "Sharpness" for DAMAGE_ALL etc.
	 */
	private static final Map<Enchantment, String> loreName = new TreeMap<>(Comparator.comparing(TO_STRING, String.CASE_INSENSITIVE_ORDER));

	/**
	 * Provides protection against environmental damage
	 */
	public static final Enchantment PROTECTION_ENVIRONMENTAL = register(0, "PROTECTION_ENVIRONMENTAL", "protection");

	/**
	 * Provides protection against fire damage
	 */
	public static final Enchantment PROTECTION_FIRE = register(1, "PROTECTION_FIRE", "fire_protection");

	/**
	 * Provides protection against fall damage
	 */
	public static final Enchantment PROTECTION_FALL = register(2, "PROTECTION_FALL", "feather_falling");

	/**
	 * Provides protection against explosive damage
	 */
	public static final Enchantment PROTECTION_EXPLOSIONS = register(3, "PROTECTION_EXPLOSIONS", "blast_protection");

	/**
	 * Provides protection against projectile damage
	 */
	public static final Enchantment PROTECTION_PROJECTILE = register(4, "PROTECTION_PROJECTILE", "projectile_protection");

	/**
	 * Decreases the rate of air loss whilst underwater
	 */
	public static final Enchantment OXYGEN = register(5, "OXYGEN", "respiration");

	/**
	 * Increases the speed at which a player may mine underwater
	 */
	public static final Enchantment WATER_WORKER = register(6, "WATER_WORKER", "aqua_affinity");

	/**
	 * Damages the attacker
	 */

	public static final Enchantment THORNS = register(7, "THORNS", "thorns");

	/**
	 * Increases walking speed while in water
	 */

	public static final Enchantment DEPTH_STRIDER = register(8, "DEPTH_STRIDER", "depth_strider");

	/**
	 * Freezes any still water adjacent to ice / frost which player is walking on
	 */

	public static final Enchantment FROST_WALKER = register(9, "FROST_WALKER", "frost_walker");

	/**
	 * Item cannot be removed
	 */

	public static final Enchantment BINDING_CURSE = register(10, "BINDING_CURSE", "binding_curse");

	/**
	 * Increases damage against all targets
	 */
	public static final Enchantment DAMAGE_ALL = register(16, "DAMAGE_ALL", "sharpness");

	/**
	 * Increases damage against undead targets
	 */
	public static final Enchantment DAMAGE_UNDEAD = register(17, "DAMAGE_UNDEAD", "smite");

	/**
	 * Increases damage against arthropod targets
	 */
	public static final Enchantment DAMAGE_ARTHROPODS = register(18, "DAMAGE_ARTHROPODS", "bane_of_arthropods");

	/**
	 * All damage to other targets will knock them back when hit
	 */
	public static final Enchantment KNOCKBACK = register(19, "KNOCKBACK", "knockback");

	/**
	 * When attacking a target, has a chance to set them on fire
	 */
	public static final Enchantment FIRE_ASPECT = register(20, "FIRE_ASPECT", "fire_aspect");

	/**
	 * Provides a chance of gaining extra loot when killing monsters
	 */
	public static final Enchantment LOOT_BONUS_MOBS = register(21, "LOOT_BONUS_MOBS", "looting");

	/**
	 * Increases damage against targets when using a sweep attack
	 */

	public static final Enchantment SWEEPING_EDGE = register(22, "SWEEPING", "sweeping_edge");

	/**
	 * Increases the rate at which you mine/dig
	 */
	public static final Enchantment DIG_SPEED = register(32, "DIG_SPEED", "efficiency");

	/**
	 * Allows blocks to drop themselves instead of fragments (for example,
	 * stone instead of cobblestone)
	 */
	public static final Enchantment SILK_TOUCH = register(33, "SILK_TOUCH", "silk_touch");

	/**
	 * Decreases the rate at which a tool looses durability
	 */
	public static final Enchantment DURABILITY = register(34, "DURABILITY", "unbreaking");

	/**
	 * Provides a chance of gaining extra loot when destroying blocks
	 */
	public static final Enchantment LOOT_BONUS_BLOCKS = register(35, "LOOT_BONUS_BLOCKS", "fortune");

	/**
	 * Provides extra damage when shooting arrows from bows
	 */
	public static final Enchantment ARROW_DAMAGE = register(48, "ARROW_DAMAGE", "power");

	/**
	 * Provides a knockback when an entity is hit by an arrow from a bow
	 */
	public static final Enchantment ARROW_KNOCKBACK = register(49, "ARROW_KNOCKBACK", "punch");

	/**
	 * Sets entities on fire when hit by arrows shot from a bow
	 */
	public static final Enchantment ARROW_FIRE = register(50, "ARROW_FIRE", "flame");

	/**
	 * Provides infinite arrows when shooting a bow
	 */
	public static final Enchantment ARROW_INFINITE = register(51, "ARROW_INFINITE", "infinity");

	/**
	 * Decreases odds of catching worthless junk
	 */
	public static final Enchantment LUCK = register(61, "LUCK", "luck_of_the_sea");

	/**
	 * Increases rate of fish biting your hook
	 */
	public static final Enchantment LURE = register(62, "LURE", "lure");

	/**
	 * Causes a thrown trident to return to the player who threw it
	 */
	public static final Enchantment LOYALTY = register(-1, "LOYALTY", "loyalty");

	/**
	 * Deals more damage to mobs that live in the ocean
	 */
	public static final Enchantment IMPALING = register(-1, "IMPALING", "impaling");

	/**
	 * When it is rainy, launches the player in the direction their trident is thrown
	 */
	public static final Enchantment RIPTIDE = register(-1, "RIPTIDE", "riptide");

	/**
	 * Strikes lightning when a mob is hit with a trident if conditions are
	 * stormy
	 */
	public static final Enchantment CHANNELING = register(-1, "CHANNELING", "channeling");

	/**
	 * Shoot multiple arrows from crossbows
	 */
	public static final Enchantment MULTISHOT = register(-1, "MULTISHOT", "multishot");

	/**
	 * Charges crossbows quickly
	 */
	public static final Enchantment QUICK_CHARGE = register(-1, "QUICK_CHARGE", "quick_charge");

	/**
	 * Crossbow projectiles pierce entities
	 */
	public static final Enchantment PIERCING = register(-1, "PIERCING", "piercing");

	/**
	* Increases fall damage of maces
	*/
	public static final Enchantment DENSITY = register(-1, "DENSITIY", "density");

	/**
	 * Reduces armor effectiveness against maces
	 */
	public static final Enchantment BREACH = register(-1, "BREACH", "breach");

	/**
	 * Emits wind burst upon hitting enemy
	 */
	public static final Enchantment WIND_BURST = register(-1, "WIND_BURST", "wind_burst");

	/**
	 * Allows mending the item using experience orbs
	 */
	public static final Enchantment MENDING = register(70, "MENDING", "mending");

	/**
	 * Item disappears instead of dropping
	 */
	public static final Enchantment VANISHING_CURSE = register(71, "VANISHING_CURSE", "vanishing_curse");

	/**
	 * Walk quicker on soul blocks
	 */
	public static final Enchantment SOUL_SPEED = register(-1, "SOUL_SPEED", "soul_speed");

	/**
	 * Walk quicker while sneaking
	 */
	public static final Enchantment SWIFT_SNEAK = register(-1, "SWIFT_SNEAK", "swift_sneak");

	/**
	 * Get the enchant by name
	 *
	 * @param name
	 * @return
	 */
	public static Enchantment getByName(String name) {
		return byName.get(name.replace("minecraft:", "").toUpperCase());
	}

	/**
	 * Return all available enchant effect types
	 *
	 * @return
	 */
	public static Collection<Enchantment> getEnchantments() {
		return byType;
	}

	/**
	 * Return the name as it appears on the item lore or null if not found
	 *
	 * @param type
	 * @return
	 */
	public static String getLoreName(Enchantment type) {
		return loreName.get(type);
	}

	/**
	 * Return all available enchant effect types
	 *
	 * @return
	 */
	public static Collection<String> getEnchantmentNames() {
		return names;
	}

	/*
	 * Find the enchantment by ID or name, returns null if unsupported by server
	 */
	private static Enchantment register(int id, String legacyName, String modernName) {
		Enchantment enchantment = null;

		try {
			enchantment = Enchantment.getByKey(NamespacedKey.minecraft(modernName));

		} catch (final NoClassDefFoundError | NoSuchMethodError ex) {
			enchantment = Enchantment.getByName(legacyName);

			if (enchantment == null && MinecraftVersion.olderThan(V.v1_13)) {
				final Method getById = ReflectionUtil.getMethod(Enchantment.class, "getById", int.class);

				enchantment = ReflectionUtil.invokeStatic(getById, id);
			}
		}

		if (enchantment != null) {
			try {
				byName.put(enchantment.getKey().getKey().toUpperCase(), enchantment);
			} catch (final NoSuchMethodError err) {
			}

			byName.put(modernName.toUpperCase(), enchantment);

			if (legacyName != null)
				byName.put(legacyName, enchantment);

			names.add(modernName.toUpperCase());

			byType.add(enchantment);
			loreName.put(enchantment, ChatUtil.capitalizeFully(modernName));
		}

		return enchantment;
	}

	static {
		for (final Enchantment enchantment : Enchantment.values()) {
			String name;

			try {
				name = enchantment.getKey().getKey().toUpperCase();

			} catch (final NoSuchMethodError err) {
				name = enchantment.getName().toUpperCase();
			}

			byName.put(name, enchantment);
			names.add(name);
			loreName.put(enchantment, ChatUtil.capitalizeFully(name));

			byType.add(enchantment);
		}
	}
}
