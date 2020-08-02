package org.mineacademy.fo.remain.nbt;

import org.bukkit.block.BlockState;
import org.mineacademy.fo.remain.nbt.annotations.AvaliableSince;
import org.mineacademy.fo.remain.nbt.utils.MinecraftVersion;

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
		if (tile == null || !tile.isPlaced()) throw new NullPointerException("Tile can't be null/not placed!");
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

	/**
	 * Gets the NBTCompound used by spigots PersistentDataAPI. This method is only
	 * available for 1.14+!
	 *
	 * @return NBTCompound containing the data of the PersistentDataAPI
	 */
	@AvaliableSince(version = MinecraftVersion.MC1_14_R1)
	private NBTCompound getPersistentDataContainer() {
//        FAUtil.check(this::getPersistentDataContainer, CheckUtil::isAvaliable);
		if (hasKey("PublicBukkitValues")) return getCompound("PublicBukkitValues");
		else {
			final NBTContainer container = new NBTContainer();
			container.addCompound("PublicBukkitValues").setString("__nbtapi",
				"Marker to make the PersistentDataContainer have content");
			mergeCompound(container);
			return getCompound("PublicBukkitValues");
		}
	}

}
