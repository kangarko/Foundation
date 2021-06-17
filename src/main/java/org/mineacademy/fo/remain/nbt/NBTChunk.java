package org.mineacademy.fo.remain.nbt;

import org.bukkit.Chunk;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.exception.FoException;

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

		if (org.mineacademy.fo.MinecraftVersion.olderThan(V.v1_16))
			throw new FoException("getPersistentDataContainer requires MC 1.16 or newer");

		return new NBTPersistentDataContainer(chunk.getPersistentDataContainer());
	}

}
