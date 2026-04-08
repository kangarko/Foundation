package org.mineacademy.fo.remain.nbt;

import java.util.function.BiConsumer;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public interface ReadWriteItemNBT extends ReadWriteNBT, ReadableItemNBT {

	/**
	 * True, if the item has any tags now known for this item type.
	 * 
	 * @return true when custom tags are present
	 */
	boolean hasCustomNbtData();

	/**
	 * Remove all custom (non-vanilla) NBT tags from the NBTItem.
	 */
	void clearCustomNBT();

	/**
	 * Gives save access to the {@link ItemMeta} of the internal {@link ItemStack}.
	 * Supported operations while inside this scope: - any get/set method of
	 * {@link ItemMeta} - any getter on {@link NBTItem}
	 * 
	 * All changes made to the {@link NBTItem} during this scope will be reverted at
	 * the end.
	 * 
	 * @param handler
	 */
	void modifyMeta(BiConsumer<ReadableNBT, ItemMeta> handler);

	/**
	 * Gives save access to the {@link ItemMeta} of the internal {@link ItemStack}.
	 * Supported operations while inside this scope: - any get/set method of
	 * {@link ItemMeta} - any getter on {@link NBTItem}
	 * 
	 * All changes made to the {@link NBTItem} during this scope will be reverted at
	 * the end.
	 * 
	 * @param handler
	 */
	<T extends ItemMeta> void modifyMeta(Class<T> type, BiConsumer<ReadableNBT, T> handler);

}
