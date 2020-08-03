package org.mineacademy.fo.visual;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.menu.tool.BlockTool;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A class that can visualize selection of blocks in the arena
 *
 * @param <T>
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class VisualTool extends BlockTool {

	/**
	 * Handle block clicking and automatically refreshes rendering of visualized blocks
	 *
	 * @param player
	 * @param click
	 * @param block
	 */
	@Override
	protected final void onBlockClick(final Player player, final ClickType click, final Block block) {
		// Remove old blocks
		stopVisualizing(player);

		// Call the block handling, probably new blocks will appear
		handleBlockClick(player, click, block);

		// Render the new blocks
		visualize(player);
	}

	/**
	 * Handles block clicking. Any changes here will be reflected automatically in the visualization
	 *
	 * @param player
	 * @param click
	 * @param block
	 */
	protected abstract void handleBlockClick(Player player, ClickType click, Block block);

	/**
	 * @see org.mineacademy.arena.tool.ArenaTool#onAirClick(org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType)
	 */
	@Override
	protected final void onAirClick(final Player player, final ClickType click) {
		// Remove old blocks
		stopVisualizing(player);

		// Call the block handling, probably new blocks will appear
		handleAirClick(player, click);

		// Render the new blocks
		visualize(player);
	}

	/**
	 * Handles air clicking and updates visualization automatically
	 *
	 * @param player
	 * @param click
	 */
	protected void handleAirClick(final Player player, final ClickType click) {
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#onHotbarFocused(org.bukkit.entity.Player)
	 */
	@Override
	protected final void onHotbarFocused(final Player player) {
		visualize(player);
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#onHotbarDefocused(org.bukkit.entity.Player)
	 */
	@Override
	protected final void onHotbarDefocused(final Player player) {
		stopVisualizing(player);
	}

	/**
	 * Return a list of points we should render in this visualization
	 *
	 * @return
	 */
	protected abstract List<Location> getVisualizedPoints();

	/**
	 * Return a region that this tool should draw particles around
	 *
	 * @return
	 */
	protected VisualizedRegion getVisualizedRegion() {
		return null;
	}

	/**
	 * Return the name above the glowing block for the given parameters
	 *
	 * @param block
	 * @param player
	 * @return
	 */
	protected abstract String getBlockName(Block block, Player player);

	/**
	 * Return the block mask for the given parameters
	 *
	 * @param block
	 * @param player
	 * @return
	 */
	protected abstract CompMaterial getBlockMask(Block block, Player player);

	/*
	 * Visualize the region and points if exist
	 */
	private void visualize(final Player player) {
		final VisualizedRegion region = getVisualizedRegion();

		if (region != null && region.isWhole())
			if (!region.canSeeParticles(player))
				region.showParticles(player);

		for (final Location location : getVisualizedPoints()) {
			if (location == null)
				continue;

			final Block block = location.getBlock();

			if (!BlockVisualizer.isVisualized(block))
				BlockVisualizer.visualize(block, getBlockMask(block, player), getBlockName(block, player));
		}
	}

	/*
	 * Stop visualizing region and points if they were so before
	 */
	private void stopVisualizing(final Player player) {
		final VisualizedRegion region = getVisualizedRegion();

		if (region != null && region.canSeeParticles(player))
			region.hideParticles(player);

		for (final Location location : getVisualizedPoints()) {
			if (location == null)
				continue;

			final Block block = location.getBlock();

			if (BlockVisualizer.isVisualized(block))
				BlockVisualizer.stopVisualizing(block);
		}
	}
}
