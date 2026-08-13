package org.mineacademy.fo.remain.nbt;

import org.bukkit.Chunk;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;

/**
 * Helper class to store NBT data to {@link Chunk}'s PDC (persistent data
 * container).
 *
 * @deprecated use methods in {@link NBT} class to read/modify chunk's nbt
 */
@Deprecated
public class NBTChunk {

	private final Chunk chunk;

	public NBTChunk(Chunk chunk) {
		this.chunk = chunk;
	}

	/**
	 * Gets the NBTCompound used by spigots PersistentDataAPI. This method is only
	 * available for 1.16.4+!
	 *
	 * @return NBTCompound containing the data of the PersistentDataAPI
	 */
	public NBTCompound getPersistentDataContainer() {
		Valid.checkBoolean(org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_16), "PersistentDataContainer is only available for 1.16.4+!");

		return new NBTPersistentDataContainer(this.chunk.getPersistentDataContainer());
	}

}
