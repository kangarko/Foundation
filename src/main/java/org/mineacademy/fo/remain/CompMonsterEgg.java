package org.mineacademy.fo.remain;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.nbt.NBTCompound;
import org.mineacademy.fo.remain.nbt.NBTItem;

import lombok.NonNull;

/**
 * Utility class for manipulating Monster Eggs
 */
public final class CompMonsterEgg {

	/**
	 * Our universal tag we use to mark our eggs
	 */
	private static final String TAG = SimplePlugin.getNamed() + "_NbtTag";

	/**
	 * Suppress errors when entity type cannot be detected from an item stack and
	 * return unknown entity or otherwise.
	 */
	public static boolean acceptUnsafeEggs = false;

	// Prevent new instance, always call static methods
	private CompMonsterEgg() {
	}

	/**
	 * Makes a monster egg of the specified type.
	 *
	 * @param type
	 * @return the finished monster egg
	 */
	public static ItemStack makeEgg(final EntityType type) {
		return makeEgg(type, 1);
	}

	/**
	 * Makes a monster egg of a certain count.
	 *
	 * @param type
	 * @param count
	 * @return the finished egg
	 */
	public static ItemStack makeEgg(final EntityType type, final int count) {
		Valid.checkNotNull(type, "Entity type cannot be null!");
		ItemStack itemStack = new ItemStack(CompMaterial.makeMonsterEgg(type).getMaterial(), count);

		// For older MC
		if (itemStack.getType().toString().equals("MONSTER_EGG"))
			itemStack = setEntity(itemStack, type);

		return itemStack;
	}

	/**
	 * Detect an {@link EntityType} from an {@link ItemStack}
	 *
	 * @param item
	 * @return the entity type, or unknown or error if not found, see
	 * {@link #acceptUnsafeEggs}
	 */
	public static EntityType getEntity(@NonNull final ItemStack item) {
		Valid.checkBoolean(CompMaterial.isMonsterEgg(item.getType()), "Item must be a monster egg not " + item);
		EntityType type = null;

		if (MinecraftVersion.atLeast(V.v1_13))
			type = getTypeFromMaterial(item);

		else if (Remain.hasItemMeta() && Remain.hasSpawnEggMeta())
			type = getTypeByMeta(item);

		else
			type = getTypeByData(item);

		if (type == null)
			type = getTypeByNbt(item);

		if (type == null && acceptUnsafeEggs)
			type = EntityType.UNKNOWN;

		Valid.checkNotNull(type, "Could not detect monster type from " + item + ")");
		return type;
	}

	private static EntityType getTypeFromMaterial(final ItemStack item) {
		final String name = item.getType().toString().replace("_SPAWN_EGG", "");
		EntityType type = null;

		try {

			// Try to translate directly
			type = EntityType.valueOf(name);

		} catch (final IllegalArgumentException ex) {

			// Special cases e.g. zombie_pigman is pig_zombie
			for (final EntityType all : EntityType.values())
				if (all.getName() != null && all.getName().equalsIgnoreCase(name))
					type = all;
		}

		Valid.checkNotNull(type, "Unable to find EntityType from Material." + item.getType());
		return type;
	}

	@SuppressWarnings("removal")
	private static EntityType getTypeByMeta(final ItemStack item) {
		final ItemMeta m = item.getItemMeta();

		return item.hasItemMeta() && m instanceof SpawnEggMeta ? ((SpawnEggMeta) m).getSpawnedType() : null;
	}

	private static EntityType getTypeByData(final ItemStack item) {
		EntityType type = readEntity0(item);

		if (type == null) {
			if (item.getDurability() != 0)
				type = DataMap.getEntity(item.getDurability());

			if (type == null && item.getData().getData() != 0)
				type = DataMap.getEntity(item.getData().getData());
		}

		return type;
	}

	private static EntityType readEntity0(final ItemStack item) {
		Valid.checkNotNull(item, "Reading entity got null item");

		final NBTItem nbt = new NBTItem(item);
		final String type = nbt.hasKey(TAG) ? nbt.getCompound(TAG).getString("entity") : null;

		return type != null && !type.isEmpty() ? EntityType.valueOf(type) : null;
	}

	private static EntityType getTypeByNbt(@NonNull final ItemStack item) {
		try {
			final Class<?> NMSItemStackClass = ReflectionUtil.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
			final Class<?> craftItemStackClass = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
			final Method asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
			final Object stack = asNMSCopyMethod.invoke(null, item);
			final Object tagCompound = NMSItemStackClass.getMethod("getTag").invoke(stack);

			if (tagCompound == null && acceptUnsafeEggs)
				return null;

			Valid.checkNotNull(tagCompound, "Spawn egg lacks tag compound: " + item);

			final Method tagGetCompound = tagCompound.getClass().getMethod("getCompound", String.class);
			final Object entityTag = tagGetCompound.invoke(tagCompound, "EntityTag");

			final Method tagGetString = entityTag.getClass().getMethod("getString", String.class);
			String idString = (String) tagGetString.invoke(entityTag, "id");

			if (MinecraftVersion.atLeast(V.v1_11) && idString.startsWith("minecraft:"))
				idString = idString.split("minecraft:")[1];

			final EntityType type = EntityType.fromName(idString);
			//Valid.requireNonNull(type, "Unable to parse mob egg from string: " + idString);

			return type;
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	/**
	 * Insert metadata to an existing monster itemstack.
	 *
	 * @param item
	 * @param type
	 * @return the itemstack
	 */
	public static ItemStack setEntity(ItemStack item, final EntityType type) {
		Valid.checkNotNull(item, "Item == null");
		Valid.checkBoolean(type.isSpawnable(), "EntityType." + type + " cannot be spawned and set into a monster egg!");

		if (MinecraftVersion.atLeast(V.v1_13)) {
			item.setType(CompMaterial.makeMonsterEgg(type).getMaterial());

			return item;
		}

		Valid.checkBoolean(CompMaterial.isMonsterEgg(item.getType()), "Item must be a monster egg not " + item);

		if (Remain.hasSpawnEggMeta())
			item = setTypeByMeta(item, type);

		else
			item = setTypeByData(item, type);

		return item;
	}

	@SuppressWarnings("removal")
	private static ItemStack setTypeByMeta(final ItemStack item, final EntityType type) {
		final SpawnEggMeta m = (SpawnEggMeta) item.getItemMeta();

		m.setSpawnedType(type);
		item.setItemMeta(m);

		return item;
	}

	private static ItemStack setTypeByData(final ItemStack item, final EntityType type) {
		final Number data = DataMap.getData(type);

		if (data.intValue() != -1) {

			item.setDurability(data.shortValue());
			item.getData().setData(data.byteValue());

			return writeEntity0(item, type);

		} else if (!acceptUnsafeEggs)
			throw new FoException("Could not set monster egg to " + type);

		return item;
	}

	private static ItemStack writeEntity0(final ItemStack item, final EntityType type) {
		Valid.checkNotNull(item, "setting nbt got null item");
		Valid.checkNotNull(type, "setting nbt got null entity");

		final NBTItem nbt = new NBTItem(item);
		final NBTCompound tag = nbt.addCompound(TAG);

		tag.setString("entity", type.toString());
		return nbt.getItem();
	}
}

/**
 * Wrapper for mapping entity ID to its respective type
 */
final class DataMap {

	private static final Map<Integer, String> map = new HashMap<>();

	static EntityType getEntity(final int data) {
		final String name = map.get(data);

		return name != null ? ReflectionUtil.lookupEnumSilent(EntityType.class, name.toUpperCase()) : null;
	}

	static int getData(final EntityType type) {
		final Integer data = getKeyFromValue(type.toString());

		return data != null ? data : -1;
	}

	private static Integer getKeyFromValue(final String value) {
		for (final Entry<Integer, String> e : map.entrySet())
			if (e.getValue().equals(value))
				return e.getKey();

		return null;
	}

	static {
		map.put(1, "DROPPED_ITEM");
		map.put(2, "EXPERIENCE_ORB");
		map.put(3, "AREA_EFFECT_CLOUD");
		map.put(4, "ELDER_GUARDIAN");
		map.put(5, "WITHER_SKELETON");
		map.put(6, "STRAY");
		map.put(7, "EGG");
		map.put(8, "LEASH_HITCH");
		map.put(9, "PAINTING");
		map.put(10, "ARROW");
		map.put(11, "SNOWBALL");
		map.put(12, "FIREBALL");
		map.put(13, "SMALL_FIREBALL");
		map.put(14, "ENDER_PEARL");
		map.put(15, "ENDER_SIGNAL");
		map.put(16, "SPLASH_POTION");
		map.put(17, "THROWN_EXP_BOTTLE");
		map.put(18, "ITEM_FRAME");
		map.put(19, "WITHER_SKULL");
		map.put(20, "PRIMED_TNT");
		map.put(21, "FALLING_BLOCK");
		map.put(22, "FIREWORK");
		map.put(23, "HUSK");
		map.put(24, "SPECTRAL_ARROW");
		map.put(25, "SHULKER_BULLET");
		map.put(26, "DRAGON_FIREBALL");
		map.put(27, "ZOMBIE_VILLAGER");
		map.put(28, "SKELETON_HORSE");
		map.put(29, "ZOMBIE_HORSE");
		map.put(30, "ARMOR_STAND");
		map.put(31, "DONKEY");
		map.put(32, "MULE");
		map.put(33, "EVOKER_FANGS");
		map.put(34, "EVOKER");
		map.put(35, "VEX");
		map.put(36, "VINDICATOR");
		map.put(37, "ILLUSIONER");
		map.put(40, "MINECART_COMMAND");
		map.put(41, "BOAT");
		map.put(42, "MINECART");
		map.put(43, "MINECART_CHEST");
		map.put(44, "MINECART_FURNACE");
		map.put(45, "MINECART_TNT");
		map.put(46, "MINECART_HOPPER");
		map.put(47, "MINECART_MOB_SPAWNER");
		map.put(50, "CREEPER");
		map.put(51, "SKELETON");
		map.put(52, "SPIDER");
		map.put(53, "GIANT");
		map.put(54, "ZOMBIE");
		map.put(55, "SLIME");
		map.put(56, "GHAST");
		map.put(57, "PIG_ZOMBIE");
		map.put(58, "ENDERMAN");
		map.put(59, "CAVE_SPIDER");
		map.put(60, "SILVERFISH");
		map.put(61, "BLAZE");
		map.put(62, "MAGMA_CUBE");
		map.put(63, "ENDER_DRAGON");
		map.put(64, "WITHER");
		map.put(65, "BAT");
		map.put(66, "WITCH");
		map.put(67, "ENDERMITE");
		map.put(68, "GUARDIAN");
		map.put(69, "SHULKER");
		map.put(90, "PIG");
		map.put(91, "SHEEP");
		map.put(92, "COW");
		map.put(93, "CHICKEN");
		map.put(94, "SQUID");
		map.put(95, "WOLF");
		map.put(96, "MUSHROOM_COW");
		map.put(97, "SNOWMAN");
		map.put(98, "OCELOT");
		map.put(99, "IRON_GOLEM");
		map.put(100, "HORSE");
		map.put(101, "RABBIT");
		map.put(102, "POLAR_BEAR");
		map.put(103, "LLAMA");
		map.put(104, "LLAMA_SPIT");
		map.put(105, "PARROT");
		map.put(120, "VILLAGER");
		//map.put(200, "ENDER_CRYSTAL");
		//map.put(-1, "LINGERING_POTION");
		//map.put(-1, "FISHING_HOOK");
		//map.put(-1, "LIGHTNING");
		//map.put(-1, "WEATHER");
		//map.put(-1, "PLAYER");
		//map.put(-1, "COMPLEX_PART");
		//map.put(-1, "TIPPED_ARROW");
		//map.put(-1, "UNKNOWN");
	}
}