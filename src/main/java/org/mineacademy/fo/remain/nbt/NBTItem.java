package org.mineacademy.fo.remain.nbt;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.constants.FoConstants;

import lombok.NonNull;

/**
 * NBT class to access vanilla/custom tags on ItemStacks. This class doesn't
 * autosave to the Itemstack, use getItem to get the changed ItemStack
 *
 * @author tr7zw
 *
 */
public class NBTItem extends NBTCompound {

	private ItemStack bukkitItem;

	/**
	 * Constructor for NBTItems. The ItemStack will be cloned!
	 *
	 * @param item
	 */
	public NBTItem(ItemStack item) {
		super(null, null);
		if (item == null) {
			throw new NullPointerException("ItemStack can't be null!");
		}
		bukkitItem = item.clone();
	}

	@Override
	public Object getCompound() {
		return NBTReflectionUtil.getItemRootNBTTagCompound(WrapperMethod.ITEMSTACK_NMSCOPY.run(null, bukkitItem));
	}

	@Override
	protected void setCompound(Object compound) {
		final Object stack = WrapperMethod.ITEMSTACK_NMSCOPY.run(null, bukkitItem);
		WrapperMethod.ITEMSTACK_SET_TAG.run(stack, compound);
		bukkitItem = (ItemStack) WrapperMethod.ITEMSTACK_BUKKITMIRROR.run(null, stack);
	}

	/**
	 * @return The modified ItemStack
	 */
	public ItemStack getItem() {
		return bukkitItem;
	}

	protected void setItem(ItemStack item) {
		bukkitItem = item;
	}

	/**
	 * This may return true even when the NBT is empty.
	 *
	 * @return Does the ItemStack have a NBTCompound.
	 */
	public boolean hasNBTData() {
		return getCompound() != null;
	}

	/**
	 * Helper method that converts {@link ItemStack} to {@link NBTContainer} with
	 * all it's data like Material, Damage, Amount and Tags.
	 *
	 * @param item
	 * @return Standalone {@link NBTContainer} with the Item's data
	 */
	public static NBTContainer convertItemtoNBT(ItemStack item) {
		return NBTReflectionUtil.convertNMSItemtoNBTCompound(WrapperMethod.ITEMSTACK_NMSCOPY.run(null, item));
	}

	/**
	 * Helper method to do the inverse to "convertItemtoNBT". Creates an
	 * {@link ItemStack} using the {@link NBTCompound}
	 *
	 * @param comp
	 * @return ItemStack using the {@link NBTCompound}'s data
	 */
	public static ItemStack convertNBTtoItem(NBTCompound comp) {
		return (ItemStack) WrapperMethod.ITEMSTACK_BUKKITMIRROR.run(null,
				NBTReflectionUtil.convertNBTCompoundtoNMSItem(comp));
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
