package org.mineacademy.fo.remain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.mineacademy.fo.Valid;

import lombok.NonNull;

/**
 * Stores compatible entity type names to mitigate enum changes in new Minecraft versions.
 */
public final class CompEntityType {

	/**
	 * The comparator for sorting entity types by their name
	 */
	private static final Comparator<EntityType> COMPARATOR = new Comparator<EntityType>() {
		@Override
		public int compare(EntityType first, EntityType last) {
			return first.name().compareTo(last.name());
		}
	};

	/**
	 * A set of all available entity types on this server version.
	 */
	private static final Set<EntityType> AVAILABLE = new TreeSet<>(COMPARATOR);

	/**
	 * A map of entity types by their spawn egg material.
	 */
	private static final Map<CompMaterial, EntityType> SPAWN_EGG_TO_ENTITY = new HashMap<>();

	/**
	 * A map of entity types by their spawn egg material.
	 */
	private static final Map<EntityType, CompMaterial> ENTITY_TO_SPAWN_EGG = new HashMap<>();

	/**
	 * A map of entity types by their name.
	 */
	private static final Map<String, EntityType> BY_NAME = new HashMap<>();

	/**
	 * A map of entity types by their id. Note that many new entities have the id set to -1
	 * and cannot be stored.
	 */
	private static final Map<Integer, EntityType> BY_ID = new HashMap<>();
	private static final Map<EntityType, Integer> ID_TO_ENTITY = new HashMap<>();

	// ------------------------------------------------------------------------------------------------------------
	// Bukkit fields, can be null
	// ------------------------------------------------------------------------------------------------------------

	public static final EntityType DROPPED_ITEM = find(1, null, "ITEM", "DROPPED_ITEM");
	public static final EntityType EXPERIENCE_ORB = find(2, null, "EXPERIENCE_ORB");
	public static final EntityType AREA_EFFECT_CLOUD = find(3, null, "AREA_EFFECT_CLOUD");
	public static final EntityType ELDER_GUARDIAN = find(4, CompMaterial.ELDER_GUARDIAN_SPAWN_EGG, "ELDER_GUARDIAN");
	public static final EntityType WITHER_SKELETON = find(5, CompMaterial.WITHER_SKELETON_SPAWN_EGG, "WITHER_SKELETON");
	public static final EntityType STRAY = find(6, CompMaterial.STRAY_SPAWN_EGG, "STRAY");
	public static final EntityType EGG = find(7, null, "EGG");
	public static final EntityType LEASH_KNOT = find(8, null, "LEASH_KNOT", "LEASH_HITCH");
	public static final EntityType PAINTING = find(9, null, "PAINTING");
	public static final EntityType ARROW = find(10, null, "ARROW");
	public static final EntityType SNOWBALL = find(11, null, "SNOWBALL");
	public static final EntityType FIREBALL = find(12, null, "FIREBALL");
	public static final EntityType SMALL_FIREBALL = find(13, null, "SMALL_FIREBALL");
	public static final EntityType ENDER_PEARL = find(14, null, "ENDER_PEARL");
	public static final EntityType EYE_OF_ENDER = find(15, null, "EYE_OF_ENDER", "ENDER_SIGNAL");
	public static final EntityType POTION = find(16, null, "POTION", "SPLASH_POTION");
	public static final EntityType EXPERIENCE_BOTTLE = find(17, null, "EXPERIENCE_BOTTLE", "THROWN_EXP_BOTTLE");
	public static final EntityType ITEM_FRAME = find(18, null, "ITEM_FRAME");
	public static final EntityType WITHER_SKULL = find(19, null, "WITHER_SKULL");
	public static final EntityType TNT = find(20, null, "TNT", "PRIMED_TNT");
	public static final EntityType FALLING_BLOCK = find(21, null, "FALLING_BLOCK");
	public static final EntityType FIREWORK_ROCKET = find(22, null, "FIREWORK_ROCKET", "FIREWORK");
	public static final EntityType HUSK = find(23, CompMaterial.HUSK_SPAWN_EGG, "HUSK");
	public static final EntityType SPECTRAL_ARROW = find(24, null, "SPECTRAL_ARROW");
	public static final EntityType SHULKER_BULLET = find(25, null, "SHULKER_BULLET");
	public static final EntityType DRAGON_FIREBALL = find(26, null, "DRAGON_FIREBALL");
	public static final EntityType ZOMBIE_VILLAGER = find(27, CompMaterial.ZOMBIE_VILLAGER_SPAWN_EGG, "ZOMBIE_VILLAGER");
	public static final EntityType SKELETON_HORSE = find(28, CompMaterial.SKELETON_HORSE_SPAWN_EGG, "SKELETON_HORSE");
	public static final EntityType ZOMBIE_HORSE = find(29, CompMaterial.ZOMBIE_HORSE_SPAWN_EGG, "ZOMBIE_HORSE");
	public static final EntityType ARMOR_STAND = find(30, null, "ARMOR_STAND");
	public static final EntityType DONKEY = find(31, CompMaterial.DONKEY_SPAWN_EGG, "DONKEY");
	public static final EntityType MULE = find(32, CompMaterial.MULE_SPAWN_EGG, "MULE");
	public static final EntityType EVOKER_FANGS = find(33, null, "EVOKER_FANGS");
	public static final EntityType EVOKER = find(34, CompMaterial.EVOKER_SPAWN_EGG, "EVOKER");
	public static final EntityType VEX = find(35, CompMaterial.VEX_SPAWN_EGG, "VEX");
	public static final EntityType VINDICATOR = find(36, CompMaterial.VINDICATOR_SPAWN_EGG, "VINDICATOR");
	public static final EntityType ILLUSIONER = find(37, null, "ILLUSIONER");
	public static final EntityType COMMAND_BLOCK_MINECART = find(40, null, "COMMAND_BLOCK_MINECART", "MINECART_COMMAND");
	public static final EntityType MINECART = find(42, null, "MINECART");
	public static final EntityType CHEST_MINECART = find(43, null, "CHEST_MINECART", "MINECART_CHEST");
	public static final EntityType FURNACE_MINECART = find(44, null, "FURNACE_MINECART", "MINECART_FURNACE");
	public static final EntityType TNT_MINECART = find(45, null, "TNT_MINECART", "MINECART_TNT");
	public static final EntityType HOPPER_MINECART = find(46, null, "HOPPER_MINECART", "MINECART_HOPPER");
	public static final EntityType SPAWNER_MINECART = find(47, null, "SPAWNER_MINECART", "MINECART_MOB_SPAWNER");
	public static final EntityType CREEPER = find(50, CompMaterial.CREEPER_SPAWN_EGG, "CREEPER");
	public static final EntityType SKELETON = find(51, CompMaterial.SKELETON_SPAWN_EGG, "SKELETON");
	public static final EntityType SPIDER = find(52, CompMaterial.SPIDER_SPAWN_EGG, "SPIDER");
	public static final EntityType GIANT = find(53, null, "GIANT");
	public static final EntityType ZOMBIE = find(54, CompMaterial.ZOMBIE_SPAWN_EGG, "ZOMBIE");
	public static final EntityType SLIME = find(55, CompMaterial.SLIME_SPAWN_EGG, "SLIME");
	public static final EntityType GHAST = find(56, CompMaterial.GHAST_SPAWN_EGG, "GHAST");
	public static final EntityType ZOMBIFIED_PIGLIN = find(57, CompMaterial.ZOMBIFIED_PIGLIN_SPAWN_EGG, "ZOMBIFIED_PIGLIN", "PIG_ZOMBIE");
	public static final EntityType ENDERMAN = find(58, CompMaterial.ENDERMAN_SPAWN_EGG, "ENDERMAN");
	public static final EntityType CAVE_SPIDER = find(59, CompMaterial.CAVE_SPIDER_SPAWN_EGG, "CAVE_SPIDER");
	public static final EntityType SILVERFISH = find(60, CompMaterial.SILVERFISH_SPAWN_EGG, "SILVERFISH");
	public static final EntityType BLAZE = find(61, CompMaterial.BLAZE_SPAWN_EGG, "BLAZE");
	public static final EntityType MAGMA_CUBE = find(62, CompMaterial.MAGMA_CUBE_SPAWN_EGG, "MAGMA_CUBE");
	public static final EntityType ENDER_DRAGON = find(63, CompMaterial.ENDER_DRAGON_SPAWN_EGG, "ENDER_DRAGON");
	public static final EntityType WITHER = find(64, CompMaterial.WITHER_SPAWN_EGG, "WITHER");
	public static final EntityType BAT = find(65, CompMaterial.BAT_SPAWN_EGG, "BAT");
	public static final EntityType WITCH = find(66, CompMaterial.WITCH_SPAWN_EGG, "WITCH");
	public static final EntityType ENDERMITE = find(67, CompMaterial.ENDERMITE_SPAWN_EGG, "ENDERMITE");
	public static final EntityType GUARDIAN = find(68, CompMaterial.GUARDIAN_SPAWN_EGG, "GUARDIAN");
	public static final EntityType SHULKER = find(69, CompMaterial.SHULKER_SPAWN_EGG, "SHULKER");
	public static final EntityType PIG = find(90, CompMaterial.PIG_SPAWN_EGG, "PIG");
	public static final EntityType SHEEP = find(91, CompMaterial.SHEEP_SPAWN_EGG, "SHEEP");
	public static final EntityType COW = find(92, CompMaterial.COW_SPAWN_EGG, "COW");
	public static final EntityType CHICKEN = find(93, CompMaterial.CHICKEN_SPAWN_EGG, "CHICKEN");
	public static final EntityType SQUID = find(94, CompMaterial.SQUID_SPAWN_EGG, "SQUID");
	public static final EntityType WOLF = find(95, CompMaterial.WOLF_SPAWN_EGG, "WOLF");
	public static final EntityType MOOSHROOM = find(96, CompMaterial.MOOSHROOM_SPAWN_EGG, "MOOSHROOM", "MUSHROOM_COW");
	public static final EntityType SNOW_GOLEM = find(97, CompMaterial.SNOW_GOLEM_SPAWN_EGG, "SNOW_GOLEM", "SNOWMAN");
	public static final EntityType OCELOT = find(98, CompMaterial.OCELOT_SPAWN_EGG, "OCELOT");
	public static final EntityType IRON_GOLEM = find(99, CompMaterial.IRON_GOLEM_SPAWN_EGG, "IRON_GOLEM");
	public static final EntityType HORSE = find(100, CompMaterial.HORSE_SPAWN_EGG, "HORSE");
	public static final EntityType RABBIT = find(101, CompMaterial.RABBIT_SPAWN_EGG, "RABBIT");
	public static final EntityType POLAR_BEAR = find(102, CompMaterial.POLAR_BEAR_SPAWN_EGG, "POLAR_BEAR");
	public static final EntityType LLAMA = find(103, CompMaterial.LLAMA_SPAWN_EGG, "LLAMA");
	public static final EntityType LLAMA_SPIT = find(104, null, "LLAMA_SPIT");
	public static final EntityType PARROT = find(105, CompMaterial.PARROT_SPAWN_EGG, "PARROT");
	public static final EntityType VILLAGER = find(120, CompMaterial.VILLAGER_SPAWN_EGG, "VILLAGER");
	public static final EntityType END_CRYSTAL = find(200, null, "END_CRYSTAL", "ENDER_CRYSTAL");
	public static final EntityType TURTLE = find(-1, CompMaterial.TURTLE_SPAWN_EGG, "TURTLE");
	public static final EntityType PHANTOM = find(-1, CompMaterial.PHANTOM_SPAWN_EGG, "PHANTOM");
	public static final EntityType TRIDENT = find(-1, null, "TRIDENT");
	public static final EntityType COD = find(-1, CompMaterial.COD_SPAWN_EGG, "COD");
	public static final EntityType SALMON = find(-1, CompMaterial.SALMON_SPAWN_EGG, "SALMON");
	public static final EntityType PUFFERFISH = find(-1, CompMaterial.PUFFERFISH_SPAWN_EGG, "PUFFERFISH");
	public static final EntityType TROPICAL_FISH = find(-1, CompMaterial.TROPICAL_FISH_SPAWN_EGG, "TROPICAL_FISH");
	public static final EntityType DROWNED = find(-1, CompMaterial.DROWNED_SPAWN_EGG, "DROWNED");
	public static final EntityType DOLPHIN = find(-1, CompMaterial.DOLPHIN_SPAWN_EGG, "DOLPHIN");
	public static final EntityType CAT = find(-1, CompMaterial.CAT_SPAWN_EGG, "CAT");
	public static final EntityType PANDA = find(-1, CompMaterial.PANDA_SPAWN_EGG, "PANDA");
	public static final EntityType PILLAGER = find(-1, CompMaterial.PILLAGER_SPAWN_EGG, "PILLAGER");
	public static final EntityType RAVAGER = find(-1, CompMaterial.RAVAGER_SPAWN_EGG, "RAVAGER");
	public static final EntityType TRADER_LLAMA = find(-1, CompMaterial.TRADER_LLAMA_SPAWN_EGG, "TRADER_LLAMA");
	public static final EntityType WANDERING_TRADER = find(-1, CompMaterial.WANDERING_TRADER_SPAWN_EGG, "WANDERING_TRADER");
	public static final EntityType FOX = find(-1, CompMaterial.FOX_SPAWN_EGG, "FOX");
	public static final EntityType BEE = find(-1, CompMaterial.BEE_SPAWN_EGG, "BEE");
	public static final EntityType HOGLIN = find(-1, CompMaterial.HOGLIN_SPAWN_EGG, "HOGLIN");
	public static final EntityType PIGLIN = find(-1, CompMaterial.PIGLIN_SPAWN_EGG, "PIGLIN");
	public static final EntityType STRIDER = find(-1, CompMaterial.STRIDER_SPAWN_EGG, "STRIDER");
	public static final EntityType ZOGLIN = find(-1, CompMaterial.ZOGLIN_SPAWN_EGG, "ZOGLIN");
	public static final EntityType PIGLIN_BRUTE = find(-1, CompMaterial.PIGLIN_BRUTE_SPAWN_EGG, "PIGLIN_BRUTE");
	public static final EntityType AXOLOTL = find(-1, CompMaterial.AXOLOTL_SPAWN_EGG, "AXOLOTL");
	public static final EntityType GLOW_ITEM_FRAME = find(-1, null, "GLOW_ITEM_FRAME");
	public static final EntityType GLOW_SQUID = find(-1, CompMaterial.GLOW_SQUID_SPAWN_EGG, "GLOW_SQUID");
	public static final EntityType GOAT = find(-1, CompMaterial.GOAT_SPAWN_EGG, "GOAT");
	public static final EntityType MARKER = find(-1, null, "MARKER");
	public static final EntityType ALLAY = find(-1, CompMaterial.ALLAY_SPAWN_EGG, "ALLAY");
	public static final EntityType FROG = find(-1, CompMaterial.FROG_SPAWN_EGG, "FROG");
	public static final EntityType TADPOLE = find(-1, CompMaterial.TADPOLE_SPAWN_EGG, "TADPOLE");
	public static final EntityType WARDEN = find(-1, CompMaterial.WARDEN_SPAWN_EGG, "WARDEN");
	public static final EntityType CAMEL = find(-1, CompMaterial.CAMEL_SPAWN_EGG, "CAMEL");
	public static final EntityType BLOCK_DISPLAY = find(-1, null, "BLOCK_DISPLAY");
	public static final EntityType INTERACTION = find(-1, null, "INTERACTION");
	public static final EntityType ITEM_DISPLAY = find(-1, null, "ITEM_DISPLAY");
	public static final EntityType SNIFFER = find(-1, CompMaterial.SNIFFER_SPAWN_EGG, "SNIFFER");
	public static final EntityType TEXT_DISPLAY = find(-1, null, "TEXT_DISPLAY");
	public static final EntityType BREEZE = find(-1, CompMaterial.BREEZE_SPAWN_EGG, "BREEZE");
	public static final EntityType WIND_CHARGE = find(-1, null, "WIND_CHARGE");
	public static final EntityType BREEZE_WIND_CHARGE = find(-1, null, "BREEZE_WIND_CHARGE");
	public static final EntityType ARMADILLO = find(-1, CompMaterial.ARMADILLO_SPAWN_EGG, "ARMADILLO");
	public static final EntityType BOGGED = find(-1, CompMaterial.BOGGED_SPAWN_EGG, "BOGGED");
	public static final EntityType OMINOUS_ITEM_SPAWNER = find(-1, null, "OMINOUS_ITEM_SPAWNER");
	public static final EntityType ACACIA_BOAT = find(-1, null, "ACACIA_BOAT");
	public static final EntityType ACACIA_CHEST_BOAT = find(-1, null, "ACACIA_CHEST_BOAT");
	public static final EntityType BAMBOO_RAFT = find(-1, null, "BAMBOO_RAFT");
	public static final EntityType BAMBOO_CHEST_RAFT = find(-1, null, "BAMBOO_CHEST_RAFT");
	public static final EntityType BIRCH_BOAT = find(-1, null, "BIRCH_BOAT");
	public static final EntityType BIRCH_CHEST_BOAT = find(-1, null, "BIRCH_CHEST_BOAT");
	public static final EntityType CHERRY_BOAT = find(-1, null, "CHERRY_BOAT");
	public static final EntityType CHERRY_CHEST_BOAT = find(-1, null, "CHERRY_CHEST_BOAT");
	public static final EntityType DARK_OAK_BOAT = find(-1, null, "DARK_OAK_BOAT");
	public static final EntityType DARK_OAK_CHEST_BOAT = find(-1, null, "DARK_OAK_CHEST_BOAT");
	public static final EntityType JUNGLE_BOAT = find(-1, null, "JUNGLE_BOAT");
	public static final EntityType JUNGLE_CHEST_BOAT = find(-1, null, "JUNGLE_CHEST_BOAT");
	public static final EntityType MANGROVE_BOAT = find(-1, null, "MANGROVE_BOAT");
	public static final EntityType MANGROVE_CHEST_BOAT = find(-1, null, "MANGROVE_CHEST_BOAT");
	public static final EntityType OAK_BOAT = find(41, null, "OAK_BOAT", "BOAT");
	public static final EntityType OAK_CHEST_BOAT = find(-1, null, "OAK_CHEST_BOAT");
	public static final EntityType PALE_OAK_BOAT = find(-1, null, "PALE_OAK_BOAT");
	public static final EntityType PALE_OAK_CHEST_BOAT = find(-1, null, "PALE_OAK_CHEST_BOAT");
	public static final EntityType SPRUCE_BOAT = find(-1, null, "SPRUCE_BOAT");
	public static final EntityType SPRUCE_CHEST_BOAT = find(-1, null, "SPRUCE_CHEST_BOAT");
	public static final EntityType CREAKING = find(-1, CompMaterial.CREAKING_SPAWN_EGG, "CREAKING");
	public static final EntityType CREAKING_TRANSIENT = find(-1, null, "CREAKING_TRANSIENT");
	public static final EntityType FISHING_BOBBER = find(-1, null, "FISHING_BOBBER", "FISHING_HOOK");
	public static final EntityType LIGHTNING_BOLT = find(-1, null, "LIGHTNING_BOLT", "LIGHTNING");
	/** @deprecated removed */
	@Deprecated
	public static final EntityType WEATHER = find(-1, null, "WEATHER");
	public static final EntityType PLAYER = find(-1, null, "PLAYER");
	/** @deprecated removed */
	@Deprecated
	public static final EntityType COMPLEX_PART = find(-1, null, "COMPLEX_PART");
	/** @deprecated broken */
	@Deprecated
	public static final EntityType UNKNOWN = find(-1, null, "UNKNOWN");

	/*
	 * Find the entity type by the given id and bukkit field names.
	 */
	private static EntityType find(int id, CompMaterial spawnEggMaterial, @NonNull String... bukkitFieldNames) {
		EntityType type = null;
		boolean enabledByFeature = false;

		for (final String bukkitFieldName : bukkitFieldNames) {
			try {
				type = EntityType.valueOf(bukkitFieldName);

			} catch (final IllegalArgumentException ex) {
				continue;
			}

			try {
				for (final World other : Bukkit.getWorlds())
					if (type.isEnabledByFeature(other)) {
						enabledByFeature = true;

						break;
					}

			} catch (final IllegalArgumentException | NoSuchMethodError err) {
				enabledByFeature = true;
			}
		}

		if (type != null && enabledByFeature) {

			// Add all name aliases
			for (final String bukkitFieldName : bukkitFieldNames) {
				Valid.checkBoolean(!BY_NAME.containsKey(bukkitFieldName), "Duplicate entity type name: " + bukkitFieldName);

				BY_NAME.put(bukkitFieldName, type);
			}

			if (type.getName() != null)
				BY_NAME.put(type.getName().toUpperCase(), type);

			// Cache available types
			AVAILABLE.add(type);

			// Cache by ID
			if (id != -1) {
				Valid.checkBoolean(!BY_ID.containsKey(id), "Duplicate entity type id: " + id);

				BY_ID.put(id, type);
				ID_TO_ENTITY.put(type, id);
			}

			// Cache by spawn egg
			if (spawnEggMaterial != null && spawnEggMaterial.getMaterial() != null) {
				Valid.checkBoolean(!SPAWN_EGG_TO_ENTITY.containsKey(spawnEggMaterial), "Duplicate spawn egg material: " + spawnEggMaterial);

				// CompMaterial is never null, but legacy versions do not hold all spawn eggs
				if (CompMaterial.isMonsterEgg(spawnEggMaterial.getMaterial())) {
					SPAWN_EGG_TO_ENTITY.put(spawnEggMaterial, type);
					ENTITY_TO_SPAWN_EGG.put(type, spawnEggMaterial);
				}
			}
		}

		return type;
	}

	/**
	 * Return all available entity types for this server version,
	 * includes unspawnable ones.
	 *
	 * @return
	 */
	public static Set<EntityType> getAvailable() {
		return AVAILABLE;
	}

	/**
	 * Return all available entity types, those can are alive
	 * and can be spawned via Bukkit.getWorld().spawnEntity()
	 *
	 * @return
	 */
	public static Set<EntityType> getAvailableSpawnable() {
		final Set<EntityType> types = new TreeSet<>(COMPARATOR);

		for (final EntityType type : AVAILABLE)
			if (type.isAlive() && type.isSpawnable())
				types.add(type);

		return types;
	}

	/**
	 * Return all available entity types that have a spawn egg.
	 * Careful - on old Minecraft version this excludes many spawnable entities.
	 *
	 * @return
	 */
	public static Set<EntityType> getAvailableWithSpawnEgg() {
		final Set<EntityType> types = new TreeSet<>(COMPARATOR);
		types.addAll(SPAWN_EGG_TO_ENTITY.values());

		return types;
	}

	/**
	 * Return the entity type from the given name.
	 * Returns null if not found.
	 *
	 * @param name
	 * @return
	 */
	public static EntityType fromName(@NonNull String name) {
		EntityType type = BY_NAME.get(name.toUpperCase());

		if (type == null)
			type = BY_NAME.get(name.toUpperCase().replace("_", ""));

		return type;
	}

	/**
	 * Return the entity type from the given material.
	 * Returns null if not found.
	 *
	 * @param material
	 * @return
	 */
	public static EntityType fromSpawnEggMaterial(CompMaterial material) {
		return SPAWN_EGG_TO_ENTITY.get(material);
	}

	/**
	 * Return the spawn egg material from the given entity type
	 * or null if not found.
	 *
	 * @param type
	 * @return
	 */
	public static CompMaterial getSpawnEgg(EntityType type) {
		return ENTITY_TO_SPAWN_EGG.get(type);
	}

	/**
	 * Return the id of the given entity type or null if not found.
	 *
	 * @param type
	 * @return
	 */
	public static Integer getId(EntityType type) {
		return ID_TO_ENTITY.get(type);
	}

	/**
	 * Return the entity type from the given id.
	 * Returns null if the id is -1 or not found.
	 *
	 * @param id
	 * @return
	 */
	public static EntityType fromId(int id) {
		Valid.checkBoolean(id != -1, "Cannot get entity type from id -1");

		return BY_ID.get(id);
	}
}