package org.mineacademy.fo.remain.nbt;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;

/**
 * NBT class to access vanilla tags from TileEntities. TileEntities don't
 * support custom tags. Use the NBTInjector for custom tags. Changes will be
 * instantly applied to the Tile, use the merge method to do many things at
 * once.
 *
 * @author tr7zw
 *
 */
public class NBTTileEntity extends NBTCompound {

	private final BlockState tile;

	/**
	 * @param tile BlockState from any TileEntity
	 */
	public NBTTileEntity(BlockState tile) {
		super(null, null);
		if (tile == null || (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_8_R3) && !tile.isPlaced())) {
			throw new NullPointerException("Tile can't be null/not placed!");
		}
		this.tile = tile;
	}

	@Override
	public Object getCompound() {
		if (!Bukkit.isPrimaryThread())
			throw new NbtApiException("BlockEntity NBT needs to be accessed sync!");
		return NBTReflectionUtil.getTileEntityNBTTagCompound(tile);
	}

	@Override
	protected void setCompound(Object compound) {
		if (!Bukkit.isPrimaryThread())
			throw new NbtApiException("BlockEntity NBT needs to be accessed sync!");
		NBTReflectionUtil.setTileEntityNBTTagCompound(tile, compound);
	}

	/**
	 * Gets the NBTCompound used by spigots PersistentDataAPI. This method is only
	 * available for 1.14+!
	 *
	 * @return NBTCompound containing the data of the PersistentDataAPI
	 */
	public NBTCompound getPersistentDataContainer() {
		Valid.checkBoolean(org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_14), "getPersistentDataContainer() requires MC 1.14+");

		if (hasTag("PublicBukkitValues")) {
			return getCompound("PublicBukkitValues");
		} else {
			NBTContainer container = new NBTContainer();
			container.addCompound("PublicBukkitValues").setString("__nbtapi",
					"Marker to make the PersistentDataContainer have content");
			mergeCompound(container);
			return getCompound("PublicBukkitValues");
		}
	}

}
