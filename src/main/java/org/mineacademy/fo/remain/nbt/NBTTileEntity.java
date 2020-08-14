package org.mineacademy.fo.remain.nbt;

import org.bukkit.block.BlockState;

/**
 * NBT class to access vanilla tags from TileEntities. TileEntities don't
 * support custom tags. Use the NBTInjector for custom tags. Changes will be
 * instantly applied to the Tile, use the merge method to do many things at
 * once.
 *
 * @author tr7zw
 */
public class NBTTileEntity extends NBTCompound {

	private final BlockState tile;

	/**
	 * @param tile BlockState from any TileEntity
	 */
	public NBTTileEntity(final BlockState tile) {
		super(null, null);
		if (tile == null || !tile.isPlaced())
			throw new NullPointerException("Tile can't be null/not placed!");
		this.tile = tile;
	}

	@Override
	public Object getCompound() {
		return NBTReflectionUtil.getTileEntityNBTTagCompound(tile);
	}

	@Override
	protected void setCompound(final Object compound) {
		NBTReflectionUtil.setTileEntityNBTTagCompound(tile, compound);
	}
}
