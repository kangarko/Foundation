package org.mineacademy.fo.visual;

import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * A utility class for displaying glowing block corners
 */
@UtilityClass
public class BlockVisualizer {

	/**
	 * Stores a map of currently visualized blocks
	 */
	private final StrictMap<Location, Object /*old MC compatibility*/> visualizedBlocks = new StrictMap<>();

	/**
	 * Starts visualizing the block at the given location
	 *
	 * @param block
	 * @param mask
	 * @param blockName
	 */
	public void visualize(@NonNull final Block block, final CompMaterial mask, final String blockName) {
		Valid.checkBoolean(!isVisualized(block), "Block at " + block.getLocation() + " already visualized");
		final Location location = block.getLocation();

		final FallingBlock falling = spawnFallingBlock(location, mask, blockName);

		// Also send the block change packet to barrier (fixes lightning glitches)
		for (final Player player : block.getWorld().getPlayers())
			Remain.sendBlockChange(2, player, location, MinecraftVersion.olderThan(V.v1_9) ? mask : CompMaterial.BARRIER);

		visualizedBlocks.put(location, falling == null ? false : falling);
	}

	/*
	 * Spawns a customized falling block at the given location
	 */
	private FallingBlock spawnFallingBlock(final Location location, final CompMaterial mask, final String blockName) {
		if (MinecraftVersion.olderThan(V.v1_9))
			return null;

		final FallingBlock falling = Remain.spawnFallingBlock(location.clone().add(0.5, 0, 0.5), mask.getMaterial());

		falling.setDropItem(false);
		falling.setVelocity(new Vector(0, 0, 0));

		Remain.setCustomName(falling, blockName);

		CompProperty.GLOWING.apply(falling, true);
		CompProperty.GRAVITY.apply(falling, false);

		return falling;
	}

	/**
	 * Stops visualizing the block at the given location
	 *
	 * @param block
	 */
	public void stopVisualizing(@NonNull final Block block) {
		Valid.checkBoolean(isVisualized(block), "Block at " + block.getLocation() + " not visualized");

		final Object fallingBlock = visualizedBlocks.remove(block.getLocation());

		// Mark the entity for removal on the next tick
		if (fallingBlock instanceof FallingBlock)
			((FallingBlock) fallingBlock).remove();

		// Then restore the client's block back to normal
		for (final Player player : block.getWorld().getPlayers())
			Remain.sendBlockChange(1, player, block);
	}

	/**
	 * Stop all blocks from being visualized
	 */
	public void stopAll() {
		for (final Location location : new HashSet<>(visualizedBlocks.keySet())) {
			final Block block = location.getBlock();

			if (isVisualized(block))
				stopVisualizing(block);
		}
	}

	/**
	 * Return true if the given block is currently being visualized
	 *
	 * @param block
	 * @return
	 */
	public boolean isVisualized(@NonNull final Block block) {
		return visualizedBlocks.containsKey(block.getLocation());
	}
}
