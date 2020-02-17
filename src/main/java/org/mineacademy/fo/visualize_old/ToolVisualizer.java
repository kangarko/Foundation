package org.mineacademy.fo.visualize_old;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.model.BlockClick;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 *  @deprecated use classes in the new "visual" package
 */
@Deprecated
public abstract class ToolVisualizer extends Tool {

	@Getter
	private final BlockVisualizer visualizer;

	@Deprecated // unsafe
	@Getter
	@Setter
	private Location calledLocation;

	@Getter
	@Setter
	private VisualizeMode defaultMode = VisualizeMode.GLOW;

	protected ToolVisualizer() {
		this.visualizer = new BlockVisualizer(this) {

			@Override
			public String getBlockName(final Block block) {
				return ToolVisualizer.this.getBlockTitle(block);
			}

			@Override
			public void onRemove(final Player player, final Block block) {
				ToolVisualizer.this.onRemove(player, block);
			}
		};
	}

	// --------------------------------------------------------------------------------
	// Standard methods
	// --------------------------------------------------------------------------------

	@Override
	public final void onBlockClick(final PlayerInteractEvent e) {
		final Player player = e.getPlayer();

		if (!e.hasBlock() || e.getAction().toString().contains("AIR")) {
			handleAirClick(player, e.getItem(), e.getAction() == Action.LEFT_CLICK_AIR ? ClickType.LEFT : ClickType.RIGHT);
			return;
		}

		if (e.isCancelled())
			return;

		if (!BlockUtil.canSetup(e.getClickedBlock(), e.getAction()) && e.getClickedBlock().getType() != Material.BARRIER)
			return;

		final Block block = e.getClickedBlock();

		if (!canVisualize(block, player) || VisualizerListener.isBlockTakenByOthers(block, visualizer))
			return;

		handleDataLoad(player, block);

		if (!visualizer.isStored(block)) {
			handleBlockSelect(player, block, BlockClick.fromAction(e.getAction()));

			visualizer.show(block.getLocation(), getDefaultMode());
			Common.tell(player, makeActionMessage("&2set"));

		} else
			onRemove(player, block);
	}

	public final void onRemove(final Player player, final Block block) {
		handleDataLoad(player, block);

		visualizer.hide(block.getLocation());
		handleBlockBreak(player, block);

		Common.tell(player, makeRemoveActionMessage());
	}

	// Workaround for concurrency issues
	private final StrictList<Player> cache = new StrictList<>();

	@Override
	public void onHotbarFocused(@NonNull final Player pl) {
		if (cache.contains(pl))
			return;

		handleDataLoad(pl, null);

		cache.add(pl);
		visualizer.updateStored(VisualizeMode.GLOW);
		cache.remove(pl);
	}

	@Override
	public void onHotbarDefocused(@NonNull final Player pl) {
		if (cache.contains(pl))
			return;

		handleDataLoad(pl, null);

		cache.add(pl);
		visualizer.updateStored(VisualizeMode.MASK);
		cache.remove(pl);
	}

	@Override
	public final ItemStack getItem() {
		final CompMaterial mat = getMenuItem();
		Valid.checkNotNull(mat);

		setCalledLocation(null);

		return ItemCreator

				.of(mat)
				.name("&8> " + getColor() + "&l" + getName() + " &8<")

				.lore("&r")
				.lores(getDescription())
				.lore("&r")
				.lore("&7Break it to remove.")
				.unbreakable(true)
				.tag(new Tuple("Game", "Edit Item"))
				.build().make();
	}

	// --------------------------------------------------------------------------------
	// Protected methods
	// --------------------------------------------------------------------------------

	/**
	 * Can the player visualize the clicked block?
	 */
	protected abstract boolean canVisualize(Block block, Player player);

	protected abstract CompMaterial getMenuItem();

	public abstract CompMaterial getMask();

	protected abstract String getName();

	protected abstract ChatColor getColor();

	protected List<String> getDescription() {
		return Arrays.asList(
				getColor() + "Left click &7a block to set",
				"&7" + Common.article(getName().toLowerCase()) + "&7. ");
	}

	protected String makeRemoveActionMessage() {
		return makeActionMessage("&4removed");
	}

	protected String makeActionMessage(final String action) {
		return "&9Setup > &7" + StringUtils.capitalize(getName().toLowerCase() + " &7has been " + action + "&7.");
	}

	protected String getBlockTitle(final Block block) {
		return getColoredName();
	}

	protected final void visualize(final Location loc) {
		visualizer.show(loc, getDefaultMode());
	}

	protected final void visualizeMask(final Location loc) {
		visualizer.show(loc, VisualizeMode.MASK);
	}

	protected final void visualizeGlow(final Location loc) {
		visualizer.show(loc, VisualizeMode.MASK);
	}

	protected final void hide(final Location loc) {
		visualizer.hide(loc);
	}

	// --------------------------------------------------------------------------------
	// Handle
	// --------------------------------------------------------------------------------

	protected void handleAirClick(final Player pl, final ItemStack item, final ClickType click) {
	}

	protected abstract void handleDataLoad(Player pl, Block block);

	protected abstract void handleBlockSelect(Player pl, Block block, BlockClick click);

	protected abstract void handleBlockBreak(Player pl, Block block);

	// --------------------------------------------------------------------------------
	// Final
	// --------------------------------------------------------------------------------

	protected final String getColoredName() {
		return getColor() + getName();
	}

	@Override
	public final boolean autoCancel() {
		return true;
	}

	@Override
	public final boolean ignoreCancelled() {
		return false;
	}

	// --------------------------------------------------------------------------------
	// Block manipulation
	// --------------------------------------------------------------------------------

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + getMask() + "}";
	}
}
