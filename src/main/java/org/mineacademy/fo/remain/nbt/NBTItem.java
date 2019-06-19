package org.mineacademy.fo.remain.nbt;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.constants.FoConstants;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Represents an item's NBT tag
 */
@Setter
@Getter
public final class NBTItem extends NBTCompound {

	/**
	 * The associated item stack
	 */
	private ItemStack item;

	/**
	 * Access an items's NBT tag
	 */
	public NBTItem(@NonNull ItemStack item) {
		super(null, null);
		this.item = item.clone();
	}

	@Override
	protected Object getCompound() {
		return !NBTEntity.COMPATIBLE ? null : NBTReflectionUtil.getItemRootNBTTagCompound(NBTReflectionUtil.getNMSItemStack(item));
	}

	@Override
	protected void setCompound(Object tag) {
		if (NBTEntity.COMPATIBLE)
			item = NBTReflectionUtil.getBukkitItemStack(NBTReflectionUtil.setNBTTag(tag, NBTReflectionUtil.getNMSItemStack(item)));
	}

	// -----------------------------------------------------------------------------
	// Some convenience methods
	// -----------------------------------------------------------------------------

	/**
	 * Attempts to find a potion effect at the {@link FoConstants.NBT#TAG} tag
	 * by "potion" key
	 *
	 * @param item
	 * @return the effect, or null if not found
	 */
	public static PotionEffectType readPotion(ItemStack item) {
		return readPotion(item, "potion");
	}

	/**
	 * Attempts to find a potion effect at the {@link FoConstants.NBT#TAG} tag
	 * by the given key
	 *
	 * @param item
	 * @param key
	 * @return
	 */
	public static PotionEffectType readPotion(ItemStack item, String key) {
		final String name = NBTItem.readString(item, key);

		return name != null ? PotionEffectType.getByName(name) : null;
	}

	/**
	 * Attempts to find an entity type at {@link FoConstants.NBT#TAG} tag
	 * at the "entity" key
	 *
	 * @param item
	 * @return
	 */
	public static EntityType readEntity(ItemStack item) {
		return readEntity(item, "entity");
	}

	/**
	 * Attempts to find an entity type at {@link FoConstants.NBT#TAG}
	 * at the given key
	 *
	 * @param item
	 * @param key
	 * @return
	 */
	public static EntityType readEntity(ItemStack item, String key) {
		final String type = NBTItem.readString(item, key);

		return type != null && !type.isEmpty() ? EntityType.valueOf(type) : null;
	}

	/**
	 * Attempts to write a potion effect at {@link FoConstants.NBT#TAG}
	 * with the "potion" key
	 *
	 * @param item
	 * @param potion
	 * @return
	 */
	public static ItemStack writePotion(ItemStack item, @NonNull PotionEffectType potion) {
		return writePotion(item, potion, "potion");
	}

	/**
	 * Attempts to write a potion effect at {@link FoConstants.NBT#TAG}
	 * with the given key
	 *
	 * @param item
	 * @param potion
	 * @param key
	 * @return
	 */
	public static ItemStack writePotion(ItemStack item, @NonNull PotionEffectType potion, String key) {
		return NBTItem.setString(item, key, potion.getName());
	}

	/**
	 * Attempts to write an entity type at {@link FoConstants.NBT#TAG}
	 * with the "entity" key
	 *
	 * @param item
	 * @param type
	 * @return
	 */
	public static ItemStack writeEntity(ItemStack item, @NonNull EntityType type) {
		return writeEntity(item, type, "entity");
	}

	/**
	 * Attempts to write an entity type at {@link FoConstants.NBT#TAG}
	 * with the given key
	 *
	 * @param item
	 * @param type
	 * @param key
	 * @return
	 */
	public static ItemStack writeEntity(ItemStack item, @NonNull EntityType type, String key) {
		return NBTItem.setString(item, key, type.toString());
	}

	// -----------------------------------------------------------------------------
	// Quickly writing tags
	// -----------------------------------------------------------------------------

	/**
	 * A shortcut for setting a tag with key-value pair on an item
	 *
	 * The tag will be given from {@link FoConstants.NBT#TAG}
	 *
	 * @param item
	 * @param key
	 * @param value
	 * @return
	 */
	public static ItemStack setString(ItemStack item, String key, String value) {
		return setString(item, FoConstants.NBT.TAG, key, value);
	}

	/**
	 * A shortcut for setting a tag with key-value pair on an item
	 *
	 * @param item
	 * @param compoundTag
	 * @param key
	 * @param value
	 * @return
	 */
	public static ItemStack setString(ItemStack item, String compoundTag, String key, String value) {
		Valid.checkNotNull(item, "Setting NBT tag got null item");

		final NBTItem nbt = new NBTItem(item);
		final NBTCompound tag = nbt.addCompound(compoundTag);

		tag.setString(key, value);
		return nbt.getItem();
	}

	// -----------------------------------------------------------------------------
	// Quickly reading tags
	// -----------------------------------------------------------------------------

	/**
	 * A shortcut from reading a certain key from item
	 * from its tag from {@link FoConstants.NBT#TAG}
	 *
	 * @param item
	 * @param key
	 * @return
	 */
	public static String readString(ItemStack item, String key) {
		return readString(item, FoConstants.NBT.TAG, key);
	}

	/**
	 * A shortcut from reading a certain key from an item's given compound tag
	 *
	 * @param item
	 * @param compoundTag
	 * @param key
	 * @return
	 */
	public static String readString(ItemStack item, String compoundTag, String key) {
		Valid.checkNotNull(item, "Reading NBT tag got null item");

		final NBTItem nbt = new NBTItem(item);
		final String name = nbt.hasKey(compoundTag) ? nbt.getCompound(compoundTag).getString(key) : null;

		return name;
	}
}
