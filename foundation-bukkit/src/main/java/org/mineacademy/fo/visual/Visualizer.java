package org.mineacademy.fo.visual;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * A utility class to help visualize blocks.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Visualizer {

	/**
	 * Stores a map of currently visualized blocks.
	 */
	private static final Map<Location, OwnedVisualizedBlock> visualizedBlocks = new HashMap<>();

	@Getter
	@Setter
	@AllArgsConstructor
	private final static class OwnedVisualizedBlock {

		/**
		 * The block.
		 */
		private Object fallingBlock;

		/**
		 * The UUID of the player who created this visualized block.
		 */
		private final UUID creatorPlayerUid;
	}

	/**
	 * Starts visualizing the block at the given location.
	 *
	 * @param initiator
	 * @param block
	 * @param mask
	 * @param blockName
	 */
	public static void visualize(@NonNull Player initiator, @NonNull final Block block, @NonNull final CompMaterial mask, @NonNull final String blockName) {
		ValidCore.checkBoolean(!isVisualized(block), "Block at " + block.getLocation() + " already visualized");
		final Location location = block.getLocation();

		final FallingBlock falling = spawnFallingBlock(location, mask, blockName);

		// Also send the block change packet to barrier (fixes lightning glitches)
		for (final Player player : block.getWorld().getPlayers())
			Remain.sendBlockChange(2, player, location, MinecraftVersion.olderThan(V.v1_9) ? mask : CompMaterial.BARRIER);

		visualizedBlocks.put(location, new OwnedVisualizedBlock(falling == null ? false : falling, initiator.getUniqueId()));

		// Remove the block after 10 seconds
		Platform.runTask(20 * 10, () -> stopVisualizing(block));
	}

	/*
	 * Spawns a customized falling block at the given location.
	 */
	private static FallingBlock spawnFallingBlock(final Location location, final CompMaterial mask, final String blockName) {
		if (MinecraftVersion.olderThan(V.v1_9))
			return null;

		// Hide the original block for world players
		for (final Player player : location.getWorld().getPlayers())
			Remain.sendBlockChange(0, player, location, CompMaterial.AIR);

		return handleFallingSpawn(location, mask, blockName);
	}

	private static FallingBlock handleFallingSpawn(final Location location, final CompMaterial mask, final String blockName) {
		final FallingBlock falling = Remain.spawnFallingBlock(location.clone().add(0.5, 0, 0.5), mask.getMaterial());

		falling.setDropItem(false);
		falling.setVelocity(new Vector(0, 0, 0));

		Remain.setCustomName(falling, blockName);

		CompProperty.GLOWING.apply(falling, true);
		CompProperty.GRAVITY.apply(falling, false);

		try {
			falling.setPersistent(true);
		} catch (final NoSuchMethodError ex) {
			// Ignore
		}

		try {
			falling.setNoPhysics(true);
		} catch (final NoSuchMethodError ex) {
			// Ignore
		}

		return falling;
	}

	/**
	 * Stops visualizing the block at the given location.
	 *
	 * @param block
	 */
	public static void stopVisualizing(@NonNull final Block block) {
		if (isVisualized(block)) {
			final OwnedVisualizedBlock owned = visualizedBlocks.remove(block.getLocation());
			final Object fallingBlock = owned.getFallingBlock();

			// Mark the entity for removal on the next tick
			if (fallingBlock instanceof FallingBlock)
				((FallingBlock) fallingBlock).remove();

			// Then restore the client's block back to normal
			for (final Player player : block.getWorld().getPlayers())
				Remain.sendBlockChange(1, player, block);
		}
	}

	/**
	 * Stops visualizing all blocks for the given player.
	 *
	 * @param player
	 */
	public static void stopVisualizing(@NonNull final Player player) {
		final UUID playerUid = player.getUniqueId();

		for (final Iterator<Map.Entry<Location, OwnedVisualizedBlock>> iterator = visualizedBlocks.entrySet().iterator(); iterator.hasNext();) {
			final Map.Entry<Location, OwnedVisualizedBlock> entry = iterator.next();
			final OwnedVisualizedBlock owned = entry.getValue();

			if (owned.getCreatorPlayerUid().equals(playerUid)) {
				final Block block = entry.getKey().getBlock();

				// Mark the entity for removal on the next tick
				if (owned.getFallingBlock() instanceof FallingBlock)
					((FallingBlock) owned.getFallingBlock()).remove();

				// Then restore the client's block back to normal
				for (final Player worldPlayer : entry.getKey().getWorld().getPlayers())
					Remain.sendBlockChange(1, worldPlayer, block);

				iterator.remove();
			}
		}
	}

	/**
	 * Return true if the given block is currently being visualized.
	 *
	 * @param block
	 * @return
	 */
	public static boolean isVisualized(@NonNull final Block block) {
		return visualizedBlocks.containsKey(block.getLocation());
	}
}
