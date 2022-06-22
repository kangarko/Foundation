package org.mineacademy.fo.remain.nbt;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;

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
		if (tile == null || (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_8_R3) && !tile.isPlaced()))
			throw new NullPointerException("Tile can't be null/not placed!");
		this.tile = tile;
	}

	@Override
	public Object getCompound() {
		if (!Bukkit.isPrimaryThread())
			throw new FoException("BlockEntity NBT needs to be accessed sync!");
		return NBTReflectionUtil.getTileEntityNBTTagCompound(this.tile);
	}

	@Override
	protected void setCompound(Object compound) {
		if (!Bukkit.isPrimaryThread())
			throw new FoException("BlockEntity NBT needs to be accessed sync!");
		NBTReflectionUtil.setTileEntityNBTTagCompound(this.tile, compound);
	}

	/**
	 * Gets the NBTCompound used by spigots PersistentDataAPI. This method is only
	 * available for 1.14+!
	 *
	 * @return NBTCompound containing the data of the PersistentDataAPI
	 */
	public NBTCompound getPersistentDataContainer() {
		Valid.checkBoolean(org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_14), "NBTTileEntity#getPersistentDataContainer requires Minecraft 1.14+");

		if (this.hasKey("PublicBukkitValues"))
			return this.getCompound("PublicBukkitValues");
		else {
			final NBTContainer container = new NBTContainer();
			container.addCompound("PublicBukkitValues").setString("__nbtapi",
					"Marker to make the PersistentDataContainer have content");
			this.mergeCompound(container);
			return this.getCompound("PublicBukkitValues");
		}
	}

}
