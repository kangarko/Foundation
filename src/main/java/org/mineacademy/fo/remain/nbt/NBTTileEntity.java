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
	private final boolean readonly;
	private final Object compound;
	private boolean closed = false;

	/**
	 * @param tile     BlockState from any TileEntity
	 * @param readonly Readonly makes a copy at init, only reading from that copy
	 */
	public NBTTileEntity(BlockState tile, boolean readonly) {
		super(null, null);
		if (tile == null || (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_8_R3) && !tile.isPlaced())) {
			throw new NullPointerException("Tile can't be null/not placed!");
		}
		this.tile = tile;
		this.readonly = readonly;
		if (readonly) {
			this.compound = getCompound();
		} else {
			this.compound = null;
		}
	}

	/**
	 * @param tile BlockState from any TileEntity
	 */
	public NBTTileEntity(BlockState tile) {
		super(null, null);
		if (tile == null || (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_8_R3) && !tile.isPlaced())) {
			throw new NullPointerException("Tile can't be null/not placed!");
		}
		this.readonly = false;
		this.compound = null;
		this.tile = tile;
	}

	@Override
	protected void setClosed() {
		this.closed = true;
	}

	@Override
	protected boolean isClosed() {
		return closed;
	}

	@Override
	protected boolean isReadOnly() {
		return readonly;
	}

	@Override
	public Object getCompound() {
		// this runs before async check, since it's just a copy
		if (readonly && compound != null) {
			return compound;
		}
		if (!Bukkit.isPrimaryThread())
			throw new NbtApiException("BlockEntity NBT needs to be accessed sync!");
		return NBTReflectionUtil.getTileEntityNBTTagCompound(tile);
	}

	@Override
	protected void setCompound(Object compound) {
		if (readonly) {
			throw new NbtApiException("Tried setting data in read only mode!");
		}
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
		Valid.checkBoolean(org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_14), "getPersistentDataContainer needs MC 1.14+");

		if (hasTag("PublicBukkitValues")) {
			return getCompound("PublicBukkitValues");
		} else {
			final NBTContainer container = new NBTContainer();
			container.addCompound("PublicBukkitValues").setString("__nbtapi",
					"Marker to make the PersistentDataContainer have content");
			mergeCompound(container);
			return getCompound("PublicBukkitValues");
		}
	}

}
