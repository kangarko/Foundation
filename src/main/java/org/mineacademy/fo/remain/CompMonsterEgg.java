package org.mineacademy.fo.remain;

import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
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
	private static final String TAG = SimplePlugin.getInstance().getName() + "_NbtTag";

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
	public static ItemStack makeEgg(@NonNull EntityType type, final int count) {
		CompMaterial material = CompEntityType.getSpawnEgg(type);

		if (material == null && MinecraftVersion.atLeast(V.v1_13))
			material = CompMaterial.SHEEP_SPAWN_EGG;

		ItemStack itemStack = new ItemStack(material != null ? material.getMaterial() : Material.valueOf("MONSTER_EGG"), count);

		// For older MC
		if (itemStack.getType().toString().equals("MONSTER_EGG"))
			itemStack = setEntity(itemStack, type);

		return itemStack;
	}

	/**
	 * Detect an {@link EntityType} from an {@link ItemStack}
	 *
	 * @param item
	 * @return the entity type, or unknown or error if not found
	 */
	public static EntityType getEntity(@NonNull final ItemStack item) {
		Valid.checkBoolean(CompMaterial.isMonsterEgg(item.getType()), "Item must be a monster egg not " + item);
		EntityType type = null;

		if (MinecraftVersion.atLeast(V.v1_13))
			type = CompEntityType.fromSpawnEggMaterial(CompMaterial.fromItem(item));

		if (type == null && Remain.hasSpawnEggMeta())
			type = getTypeByMeta(item);

		if (type == null && MinecraftVersion.olderThan(V.v1_13))
			type = getTypeByData(item);

		if (type == null)
			type = getTypeByNbt(item);

		return type != null ? type : CompEntityType.UNKNOWN;
	}

	private static EntityType getTypeByMeta(final ItemStack item) {
		final ItemMeta meta = item.getItemMeta();

		return item.hasItemMeta() && meta instanceof SpawnEggMeta ? ((SpawnEggMeta) meta).getSpawnedType() : null;
	}

	private static EntityType getTypeByData(final ItemStack item) {
		EntityType type = readEntity0(item);

		if (type == null) {
			if (item.getDurability() != 0)
				type = EntityType.fromId(item.getDurability());

			if (type == null && item.getData().getData() != 0)
				type = EntityType.fromId(item.getData().getData());
		}

		return type;
	}

	private static EntityType readEntity0(final ItemStack item) {
		Valid.checkNotNull(item, "Reading entity got null item");

		final NBTItem nbt = new NBTItem(item);
		final String type = nbt.hasKey(TAG) ? nbt.getCompound(TAG).getString("entity") : null;

		return type != null && !type.isEmpty() ? CompEntityType.fromName(type) : null;
	}

	private static EntityType getTypeByNbt(@NonNull final ItemStack item) {
		try {
			final Class<?> classNMSItemstack = ReflectionUtil.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
			final Object stack = Remain.asNMSCopy(item);
			final Object tagCompound = classNMSItemstack.getMethod("getTag").invoke(stack);

			if (tagCompound == null)
				return null;

			Valid.checkNotNull(tagCompound, "Spawn egg lacks tag compound: " + item);

			final Method tagGetCompound = tagCompound.getClass().getMethod("getCompound", String.class);
			final Object entityTag = tagGetCompound.invoke(tagCompound, "EntityTag");

			final Method tagGetString = entityTag.getClass().getMethod("getString", String.class);
			String idString = (String) tagGetString.invoke(entityTag, "id");

			if (MinecraftVersion.atLeast(V.v1_11) && idString.startsWith("minecraft:"))
				idString = idString.split("minecraft:")[1];

			return CompEntityType.fromName(idString);

		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	/**
	 * Insert metadata to an existing monster itemstack.
	 *
	 * If the entity type does not have a monster egg, it's set to sheep spawn egg.
	 *
	 * @param item
	 * @param type
	 * @return the itemstack
	 */
	public static ItemStack setEntity(@NonNull ItemStack item, final EntityType type) {
		Valid.checkBoolean(CompMaterial.isMonsterEgg(item.getType()), "Item must be a monster egg not " + item);

		if (MinecraftVersion.atLeast(V.v1_13)) {
			final CompMaterial material = CompEntityType.getSpawnEgg(type);
			item.setType(material == null ? CompMaterial.SHEEP_SPAWN_EGG.getMaterial() : material.getMaterial());

			return item;
		}

		if (Remain.hasSpawnEggMeta())
			item = setTypeByMeta(item, type);

		else
			item = setTypeByData(item, type);

		return item;
	}

	private static ItemStack setTypeByMeta(final ItemStack item, final EntityType type) {
		final SpawnEggMeta meta = (SpawnEggMeta) item.getItemMeta();

		meta.setSpawnedType(type);
		item.setItemMeta(meta);

		return item;
	}

	private static ItemStack setTypeByData(final ItemStack item, final EntityType type) {
		final Integer id = CompEntityType.getId(type);

		if (id != null) {
			item.setDurability(id.shortValue());
			item.getData().setData(id.byteValue());
		}

		return writeEntity0(item, type);
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