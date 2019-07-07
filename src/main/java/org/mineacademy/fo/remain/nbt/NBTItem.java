package org.mineacademy.fo.remain.nbt;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompMaterial;

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
		Valid.checkNotNull(bukkitItem, "Bukkit item cannot be null!");

		if (CompMaterial.isAir(bukkitItem.getType()))
			return null;

		final Object nmsItem = WrapperMethod.ITEMSTACK_NMSCOPY.run(null, bukkitItem);

		return NBTReflectionUtil.getItemRootNBTTagCompound(nmsItem);
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
}
