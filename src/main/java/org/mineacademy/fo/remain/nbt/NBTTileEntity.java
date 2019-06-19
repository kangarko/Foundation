package org.mineacademy.fo.remain.nbt;

import org.bukkit.block.BlockState;

/**
 * Represents a tile entity's NBT tag
 */
public class NBTTileEntity extends NBTCompound {

	/**
	 * The tile entity
	 */
	private final BlockState tile;

	/**
	 * Access a tile entity's NBT tag
	 */
	public NBTTileEntity(BlockState tile) {
		super(null, null);

		this.tile = tile;
	}

	@Override
	protected Object getCompound() {
		return NBTReflectionUtil.getTileEntityNBTTagCompound(tile);
	}

	@Override
	protected void setCompound(Object tag) {
		NBTReflectionUtil.setTileEntityNBTTagCompound(tile, tag);
	}

}
