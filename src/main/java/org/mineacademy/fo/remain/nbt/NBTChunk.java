package org.mineacademy.fo.remain.nbt;

import org.bukkit.Chunk;

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
		return new NBTPersistentDataContainer(this.chunk.getPersistentDataContainer());
	}

}
