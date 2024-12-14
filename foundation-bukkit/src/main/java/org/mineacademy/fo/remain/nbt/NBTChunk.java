package org.mineacademy.fo.remain.nbt;

import org.bukkit.Chunk;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ValidCore;

public class NBTChunk {

	private final Chunk chunk;

	public NBTChunk(final Chunk chunk) {
		this.chunk = chunk;
	}

	/**
	 * Gets the NBTCompound used by spigots PersistentDataAPI. This method is only
	 * available for 1.16.4+!
	 *
	 * @return NBTCompound containing the data of the PersistentDataAPI
	 */
	public NBTCompound getPersistentDataContainer() {
		ValidCore.checkBoolean(org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_14), "NBTChunk#getPersistentDataContainer requires Minecraft 1.14+");

		return new NBTPersistentDataContainer(this.chunk.getPersistentDataContainer());
	}

}
