package org.mineacademy.fo.remain.nbt;

import org.bukkit.block.Block;

/**
 * Helper class to store NBT data to Block Locations. Use getData() to get the
 * NBT instance. Important notes:
 *
 * - Non BlockEntities can not have NBT data. This stores the data to the chunk
 * instead!
 *
 * - The data is really just on the location. If the block gets
 * broken/changed/explodes/moved etc, the data is still on that location!
 *
 * @author tr7zw
 *
 */
public class NBTBlock {

	private final Block block;
	private final NBTChunk nbtChunk;

	public NBTBlock(Block block) {
		this.block = block;
		if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R3))
			throw new NbtApiException("NBTBlock is only working for 1.16.4+!");
		this.nbtChunk = new NBTChunk(block.getChunk());
	}

	public NBTCompound getData() {
		return this.nbtChunk.getPersistentDataContainer().getOrCreateCompound("blocks")
				.getOrCreateCompound(this.block.getX() + "_" + this.block.getY() + "_" + this.block.getZ());
	}

}
