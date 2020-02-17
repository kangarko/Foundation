package org.mineacademy.fo.visualize_old;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * Visualize a single block by either replacing its type with for example
 * Glowstone or invoking setGlowing method in later MC versions
 *
 * @deprecated use classes in the new "visual" package
 */
@Deprecated
public abstract class BlockVisualizer {

	private final Object LOCK = new Object();

	/**
	 * A map of locations and their visualized blocks
	 */
	private final StrictMap<Location, VisualizedBlock> stored = new StrictMap<>();

	/**
	 * The mask that is shown when the block is visualized
	 */
	private final ToolVisualizer tool;

	public BlockVisualizer(final ToolVisualizer tool) {
		this.tool = tool;

		VisualizerListener.register(this);
	}

	/**
	 * Visualize the block.
	 * @param location
	 * @param mode
	 */
	public final void show(final Location location, final VisualizeMode mode) {
		synchronized (LOCK) {
			Valid.checkNotNull(location, "Location == null");
			Valid.checkNotNull(location.getWorld(), "Location.World == null");

			tool.setCalledLocation(location);

			final VisualizedBlock v = new VisualizedBlock(location.getBlock(), tool.getMask()) {

				@Override
				public String getBlockName(final Block block) {
					tool.setCalledLocation(block.getLocation());

					return BlockVisualizer.this.getBlockName(block);
				}
			};

			v.visualize(mode);
			stored.override(location, v);
		}
	}

	/**
	 * Stop visualizing of the block.
	 *
	 * @param block the block
	 */
	public final void hide(final Location location) {
		synchronized (LOCK) {
			if (!stored.contains(location))
				return;

			final VisualizedBlock v = stored.remove(location);

			// Workaround for shutdown of plugins:
			if (!SimplePlugin.getInstance().isEnabled())
				v.hide();

			else
				Common.runLater(() -> v.hide());
		}
	}

	/**
	 * Get if the block that this tool holds, is stored (it has been put
	 * to the map by the {@link #show(Block, VisualizeMode)} method?)
	 *
	 * @param block the block
	 * @return whether or not the block is visualized
	 */
	public final boolean isStored(final Block block) {
		synchronized (LOCK) {
			Valid.checkNotNull(block, "Null block!");

			return stored.contains(block.getLocation());
		}
	}

	/**
	 * Update all stored blocks to a new state.
	 *
	 * @param mode the new mode
	 */
	public final void updateStored(final VisualizeMode mode) {
		synchronized (LOCK) {
			for (final VisualizedBlock v : stored.values())
				v.visualize(mode);
		}
	}

	/**
	 * Get the blocks's name above it for the specified block.
	 *
	 * @param block the block
	 * @return the block name
	 */
	protected abstract String getBlockName(Block block);

	/**
	 * A method called when the block is removed
	 *
	 * @param player the player
	 * @param block the block
	 */
	protected abstract void onRemove(Player player, Block block);

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + tool.getMask() + "}";
	}
}
