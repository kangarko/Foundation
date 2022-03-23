package org.mineacademy.fo.visual;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.tool.BlockTool;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * A class that can visualize selection of blocks in the arena
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
	protected void handleBlockClick(Player player, ClickType click, Block block) {
		final boolean isPrimary = click == ClickType.LEFT;
		final Location location = block.getLocation();

		final Region region = this.getVisualizedRegion(player);
		Valid.checkNotNull(region, "Got null region on block clicking for player " + player.getName());

		// If you place primary location over a secondary location point, remove secondary
		if (!isPrimary && region.hasPrimary() && region.isPrimary(location))
			region.setPrimary(null);

		// ...and vice versa
		if (isPrimary && region.hasSecondary() && region.isSecondary(location))
			region.setSecondary(null);

		final boolean removed = !region.toggleLocation(location, click);
		Messenger.success(player, (isPrimary ? "&cPrimary" : "&6Secondary") + " &7location has been " + (removed ? "&cremoved" : "&2set") + "&7.");
	}

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
	 * @param player
	 *
	 * @return
	 */
	protected List<Location> getVisualizedPoints(Player player) {
		final Region region = this.getVisualizedRegion(player);
		final List<Location> points = new ArrayList<>();

		if (region != null) {
			if (region.hasPrimary())
				points.add(region.getPrimary());

			if (region.hasSecondary())
				points.add(region.getSecondary());
		}

		return points;
	}

	/**
	 * Return a region that this tool should draw particles around
	 *
	 * @param player
	 *
	 * @return
	 */
	protected VisualizedRegion getVisualizedRegion(Player player) {
		return null;
	}

	/**
	 * Return the name above the glowing block for the given parameters
	 *
	 * @param block
	 * @param player
	 * @return
	 */
	protected String getBlockName(Block block, Player player) {
		final Region region = this.getVisualizedRegion(player);
		String name = "&7Point";

		if (region != null) {
			final Location location = block.getLocation();

			name = region.isPrimary(location) ? "&cPrimary" : region.isSecondary(location) ? "&6Secondary" : name;
		}

		return "&8[" + name + "&8]";
	}

	/**
	 * Return the block mask for the given parameters
	 *
	 * @param block
	 * @param player
	 * @return
	 */
	protected abstract CompMaterial getBlockMask(Block block, Player player);

	/**
	 * Returns an example lore you can apply to item
	 *
	 * @return
	 */
	protected final String[] getItemLore() {
		return new String[] {
				"",
				"&6&l<- &7(left) Primary",
				"Secondary (right) &6&l->",
				"",
				"Click a block to set."
		};
	}

	@Override
	protected boolean autoCancel() {
		return true; // Cancel the event so that we don't destroy blocks when selecting them
	}

	/*
	 * Visualize the region and points if exist
	 */
	private void visualize(@NonNull final Player player) {
		final VisualizedRegion region = getVisualizedRegion(player);

		if (region != null && region.isWhole())
			if (!region.canSeeParticles(player))
				region.showParticles(player);

		for (final Location location : getVisualizedPoints(player)) {
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
	private void stopVisualizing(@NonNull final Player player) {
		final VisualizedRegion region = getVisualizedRegion(player);

		if (region != null && region.canSeeParticles(player))
			region.hideParticles(player);

		for (final Location location : getVisualizedPoints(player)) {
			if (location == null)
				continue;

			final Block block = location.getBlock();

			if (BlockVisualizer.isVisualized(block))
				BlockVisualizer.stopVisualizing(block);
		}
	}
}
