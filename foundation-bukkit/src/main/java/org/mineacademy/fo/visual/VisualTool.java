package org.mineacademy.fo.visual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.util.Vector;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.tool.BlockTool;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * A class that can visualize selection of blocks in the arena
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class VisualTool extends BlockTool {

	/**
	 * Stores a map of currently visualized blocks.
	 */
	private static final Map<Location, Object /*Old Minecraft compatibility.*/> visualizedBlocks = new HashMap<>();

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
		this.stopVisualizing(player);

		// Call the block handling, probably new blocks will appear
		this.handleBlockClick(player, click, block);

		// Render the new blocks
		this.visualize(player);
	}

	/**
	 * Handles block clicking. Any changes here will be reflected automatically in the visualization
	 * You need to override this method if you want to save the selected region or block clicked!
	 *
	 * @param player
	 * @param click
	 * @param block
	 */
	protected void handleBlockClick(Player player, ClickType click, Block block) {
		final boolean isPrimary = click == ClickType.LEFT;
		final Location location = block.getLocation();

		final Region region = this.getVisualizedRegion(player);

		if (region != null) {

			// If you place primary location over a secondary location point, remove secondary
			if (!isPrimary && region.hasPrimary() && region.isPrimary(location))
				region.setPrimary(null);

			// ...and vice versa
			if (isPrimary && region.hasSecondary() && region.isSecondary(location))
				region.setSecondary(null);

			final boolean removed = !region.toggleLocation(location, click);
			Messenger.success(player, (isPrimary ? "&cPrimary" : "&6Secondary") + " &7location has been " + (removed ? "&cremoved" : "&2set") + "&7.");
		}
	}

	/**
	 * @see org.mineacademy.arena.tool.ArenaTool#onAirClick(org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType)
	 */
	@Override
	protected final void onAirClick(final Player player, final ClickType click) {
		// Remove old blocks
		this.stopVisualizing(player);

		// Call the block handling, probably new blocks will appear
		this.handleAirClick(player, click);

		// Render the new blocks
		this.visualize(player);
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
	protected void onHotbarFocused(final Player player) {
		this.visualize(player);
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#onHotbarDefocused(org.bukkit.entity.Player)
	 */
	@Override
	protected void onHotbarDefocused(final Player player) {
		this.stopVisualizing(player);
	}

	/**
	 * Return a list of points or a single point we should render in this visualization
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
	 * Visualize the region and points if exist.
	 */
	private void visualize(@NonNull final Player player) {
		final VisualizedRegion region = this.getVisualizedRegion(player);

		if (region != null && region.isWhole())
			if (!region.canSeeParticles(player))
				region.showParticles(player);

		for (final Location location : this.getVisualizedPoints(player)) {
			if (location == null)
				continue;

			final Block block = location.getBlock();

			if (!isVisualized(block))
				visualize(block, this.getBlockMask(block, player), this.getBlockName(block, player));
		}
	}

	/*
	 * Stop visualizing region and points if they were so before
	 */
	private void stopVisualizing(@NonNull final Player player) {
		final VisualizedRegion region = this.getVisualizedRegion(player);

		if (region != null && region.canSeeParticles(player))
			region.hideParticles(player);

		for (final Location location : this.getVisualizedPoints(player)) {
			if (location == null)
				continue;

			final Block block = location.getBlock();

			if (isVisualized(block))
				stopVisualizing(block);
		}
	}

	/*
	 * Starts visualizing the block at the given location.
	 */
	private static void visualize(@NonNull final Block block, final CompMaterial mask, final String blockName) {
		Valid.checkBoolean(!isVisualized(block), "Block at " + block.getLocation() + " already visualized");
		final Location location = block.getLocation();

		final FallingBlock falling = spawnFallingBlock(location, mask, blockName);

		// Also send the block change packet to barrier (fixes lightning glitches)
		for (final Player player : block.getWorld().getPlayers())
			Remain.sendBlockChange(2, player, location, MinecraftVersion.olderThan(V.v1_9) ? mask : CompMaterial.BARRIER);

		visualizedBlocks.put(location, falling == null ? false : falling);
	}

	/*
	 * Spawns a customized falling block at the given location.
	 */
	private static FallingBlock spawnFallingBlock(final Location location, final CompMaterial mask, final String blockName) {
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

	/*
	 * Stops visualizing the block at the given location.
	 */
	private static void stopVisualizing(@NonNull final Block block) {
		Valid.checkBoolean(isVisualized(block), "Block at " + block.getLocation() + " not visualized");

		final Object fallingBlock = visualizedBlocks.remove(block.getLocation());

		// Mark the entity for removal on the next tick
		if (fallingBlock instanceof FallingBlock)
			((FallingBlock) fallingBlock).remove();

		// Then restore the client's block back to normal
		for (final Player player : block.getWorld().getPlayers())
			Remain.sendBlockChange(1, player, block);
	}

	/*
	 * Return true if the given block is currently being visualized.
	 */
	private static boolean isVisualized(@NonNull final Block block) {
		return visualizedBlocks.containsKey(block.getLocation());
	}
}
