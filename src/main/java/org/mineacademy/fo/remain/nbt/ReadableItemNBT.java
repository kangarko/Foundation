package org.mineacademy.fo.remain.nbt;

public interface ReadableItemNBT extends ReadableNBT {

	/**
	 * Returns true if the item has NBT data.
	 * 
	 * @return Does the ItemStack have a NBTCompound.
	 */
	public boolean hasNBTData();

}
