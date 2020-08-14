package org.mineacademy.fo.remain.nbt;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * NBT class to access vanilla/custom tags on ItemStacks. This class doesn't
 * autosave to the Itemstack, use getItem to get the changed ItemStack
 *
 * @author tr7zw
 */
public class NBTItem extends NBTCompound {

	private ItemStack bukkitItem;

	/**
	 * Constructor for NBTItems. The ItemStack will be cloned!
	 *
	 * @param item
	 */
	public NBTItem(final ItemStack item) {
		super(null, null);
		if (item == null)
			throw new NullPointerException("ItemStack can't be null!");

		bukkitItem = item.clone();
	}

	@Override
	public Object getCompound() {
		return NBTReflectionUtil.getItemRootNBTTagCompound(WrapperReflection.ITEMSTACK_NMSCOPY.run(null, bukkitItem));
	}

	@Override
	protected void setCompound(final Object compound) {
		final Object stack = WrapperReflection.ITEMSTACK_NMSCOPY.run(null, bukkitItem);
		WrapperReflection.ITEMSTACK_SET_TAG.run(stack, compound);
		bukkitItem = (ItemStack) WrapperReflection.ITEMSTACK_BUKKITMIRROR.run(null, stack);
	}

	/**
	 * Apply stored NBT tags to the provided ItemStack.
	 * <p>
	 * Note: This will completely override current item's {@link ItemMeta}.
	 * If you still want to keep the original item's NBT tags, see
	 * {@link #mergeNBT(ItemStack)} and {@link #mergeCustomNBT(ItemStack)}.
	 *
	 * @param item ItemStack that should get the new NBT data
	 */
	public void applyNBT(final ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			throw new NullPointerException("ItemStack can't be null/Air!");
		final NBTItem nbti = new NBTItem(new ItemStack(item.getType()));
		nbti.mergeCompound(this);
		item.setItemMeta(nbti.getItem().getItemMeta());
	}

	/**
	 * Remove all custom (non-vanilla) NBT tags from the NBTItem.
	 */
	public void clearCustomNBT() {
		final ItemMeta meta = bukkitItem.getItemMeta();
		NBTReflectionUtil.getUnhandledNBTTags(meta).clear();
		bukkitItem.setItemMeta(meta);
	}

	/**
	 * @return The modified ItemStack
	 */
	public ItemStack getItem() {
		return bukkitItem;
	}

	protected void setItem(final ItemStack item) {
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
	static NBTContainer convertItemtoNBT(final ItemStack item) {
		return NBTReflectionUtil.convertNMSItemtoNBTCompound(WrapperReflection.ITEMSTACK_NMSCOPY.run(null, item));
	}

	/**
	 * Helper method to do the inverse to "convertItemtoNBT". Creates an
	 * {@link ItemStack} using the {@link NBTCompound}
	 *
	 * @param comp
	 * @return ItemStack using the {@link NBTCompound}'s data
	 */
	static ItemStack convertNBTtoItem(final NBTCompound comp) {
		return (ItemStack) WrapperReflection.ITEMSTACK_BUKKITMIRROR.run(null, NBTReflectionUtil.convertNBTCompoundtoNMSItem(comp));
	}

}
