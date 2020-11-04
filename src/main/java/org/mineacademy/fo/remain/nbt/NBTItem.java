package org.mineacademy.fo.remain.nbt;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * NBT class to access vanilla/custom tags on ItemStacks. This class doesn't
 * autosave to the Itemstack, use getItem to get the changed ItemStack
 *
 * @author tr7zw
 *
 */
public class NBTItem extends NBTCompound {

	private ItemStack bukkitItem;
	private final boolean directApply;
	private ItemStack originalSrcStack = null;

	/**
	 * Constructor for NBTItems. The ItemStack will be cloned!
	 *
	 * @param item
	 */
	public NBTItem(ItemStack item) {
		this(item, false);
	}

	/**
	 * Constructor for NBTItems. The ItemStack will be cloned! If directApply is true,
	 * all changed will be mapped to the original item. Changes to the NBTItem will overwrite changes done
	 * to the original item in that case.
	 *
	 * @param item
	 * @param directApply
	 */
	public NBTItem(ItemStack item, boolean directApply) {
		super(null, null);
		if (item == null || item.getType() == Material.AIR) {
			throw new NullPointerException("ItemStack can't be null/Air!");
		}
		this.directApply = directApply;
		bukkitItem = item.clone();
		if (directApply) {
			this.originalSrcStack = item;
		}
	}

	@Override
	public Object getCompound() {
		return NBTReflectionUtil.getItemRootNBTTagCompound(WrapperReflection.ITEMSTACK_NMSCOPY.run(null, bukkitItem));
	}

	@Override
	protected void setCompound(Object compound) {
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
	public void applyNBT(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			throw new NullPointerException("ItemStack can't be null/Air!");
		}
		final NBTItem nbti = new NBTItem(new ItemStack(item.getType()));
		nbti.mergeCompound(this);
		item.setItemMeta(nbti.getItem().getItemMeta());
	}

	/**
	 * Merge all NBT tags to the provided ItemStack.
	 *
	 * @param item ItemStack that should get the new NBT data
	 */
	public void mergeNBT(ItemStack item) {
		final NBTItem nbti = new NBTItem(item);
		nbti.mergeCompound(this);
		item.setItemMeta(nbti.getItem().getItemMeta());
	}

	/**
	 * Merge only custom (non-vanilla) NBT tags to the provided ItemStack.
	 *
	 * @param item ItemStack that should get the new NBT data
	 */
	public void mergeCustomNBT(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			throw new NullPointerException("ItemStack can't be null/Air!");
		}
		final ItemMeta meta = item.getItemMeta();
		NBTReflectionUtil.getUnhandledNBTTags(meta).putAll(NBTReflectionUtil.getUnhandledNBTTags(bukkitItem.getItemMeta()));
		item.setItemMeta(meta);
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
		return NBTReflectionUtil.convertNMSItemtoNBTCompound(WrapperReflection.ITEMSTACK_NMSCOPY.run(null, item));
	}

	/**
	 * Helper method to do the inverse to "convertItemtoNBT". Creates an
	 * {@link ItemStack} using the {@link NBTCompound}
	 *
	 * @param comp
	 * @return ItemStack using the {@link NBTCompound}'s data
	 */
	public static ItemStack convertNBTtoItem(NBTCompound comp) {
		return (ItemStack) WrapperReflection.ITEMSTACK_BUKKITMIRROR.run(null,
				NBTReflectionUtil.convertNBTCompoundtoNMSItem(comp));
	}

	@Override
	protected void saveCompound() {
		if (directApply) {
			applyNBT(originalSrcStack);
		}
	}

}
