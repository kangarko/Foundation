package org.mineacademy.fo.menu.tool;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.conversation.CreateRegionPrompt;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.visual.VisualTool;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the tool used to create arena region for any arena
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegionTool extends VisualTool {

	/**
	 * The singular tool instance
	 */
	@Getter
	private static final Tool instance = new RegionTool();

	/**
	 * The region point that is shown above the primary/secondary block when tool is held in hands
	 */
	@Getter
	@Setter
	private static String blockName = "&f[&aRegion point&f]";

	/**
	 * The block mask you can customize
	 */
	@Getter
	@Setter
	private static CompMaterial blockMask = CompMaterial.EMERALD_BLOCK;

	/**
	 * The item material you can customize
	 */
	@Getter
	@Setter
	private static CompMaterial itemMaterial = CompMaterial.EMERALD;

	/**
	 * The item name you can customize
	 */
	@Getter
	@Setter
	private static String itemName = "Region Tool";

	@Getter
	@Setter
	private static String[] lore = {
			"",
			"Use this tool to create",
			"and edit regions.",
			"",
			"&4&l< &7Left click &7– &7Primary",
			"&4&l> &7Right click &7– &7Secondary"
	};

	/**
	 * The actual item
	 */
	private ItemStack item;

	/**
	 * @see org.mineacademy.fo.visual.VisualTool#getBlockName(org.bukkit.block.Block, org.bukkit.entity.Player)
	 */
	@Override
	protected String getBlockName(final Block block, final Player player) {
		return blockName;
	}

	/**
	 * @see org.mineacademy.fo.visual.VisualTool#getBlockMask(org.bukkit.block.Block, org.bukkit.entity.Player)
	 */
	@Override
	protected CompMaterial getBlockMask(final Block block, final Player player) {
		return blockMask;
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		if (this.item == null)
			this.item = ItemCreator.of(itemMaterial).name(itemName).lore(lore).make();

		return this.item;
	}

	/**
	 * @see org.mineacademy.fo.visual.VisualTool#handleBlockClick(org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	protected void handleBlockClick(final Player player, final ClickType click, final Block block) {
		final Location location = block.getLocation();
		final boolean primary = click == ClickType.LEFT;
		final Region region = DiskRegion.getCreatedRegion(player);

		if (primary)
			region.setPrimary(location);
		else
			region.setSecondary(location);

		final boolean whole = region.isWhole();

		if (whole && !player.isConversing())
			CreateRegionPrompt.showToOrHint(player);
		else
			Messenger.success(player, primary ? SimpleLocalization.Commands.REGION_SET_PRIMARY : SimpleLocalization.Commands.REGION_SET_SECONDARY);
	}

	/**
	 * @see org.mineacademy.fo.visual.VisualTool#getVisualizedPoints(org.bukkit.entity.Player)
	 */
	@Override
	protected List<Location> getVisualizedPoints(Player player) {
		final List<Location> blocks = new ArrayList<>();
		final Region region = DiskRegion.getCreatedRegion(player);

		if (region.getPrimary() != null)
			blocks.add(region.getPrimary());

		if (region.getSecondary() != null)
			blocks.add(region.getSecondary());

		return blocks;
	}

	/**
	 * @see org.mineacademy.fo.visual.VisualTool#getVisualizedRegion(org.bukkit.entity.Player)
	 */
	@Override
	protected VisualizedRegion getVisualizedRegion(Player player) {
		final VisualizedRegion region = DiskRegion.getCreatedRegion(player);

		return region.isWhole() ? region : null;
	}

	/**
	 * Cancel the event so that we don't destroy blocks when selecting them
	 *
	 * @see org.mineacademy.fo.menu.tool.Tool#autoCancel()
	 */
	@Override
	protected boolean autoCancel() {
		return true;
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.BlockTool#ignoreCancelled()
	 */
	@Override
	protected boolean ignoreCancelled() {
		return true;
	}
}
