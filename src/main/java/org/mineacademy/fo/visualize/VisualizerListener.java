package org.mineacademy.fo.visualize;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.menu.tool.ToolRegistry;
import org.mineacademy.fo.remain.Remain;

public final class VisualizerListener implements Listener {

	// -------------------------------------------------------------------------------
	// Static registration of different visualizers
	// -------------------------------------------------------------------------------

	private static final StrictList<BlockVisualizer> registered = new StrictList<>();

	public static void register(BlockVisualizer v) {
		registered.add(v);
	}

	public static boolean isBlockTakenByOthers(final Block block, BlockVisualizer whoAsks) {
		for (final BlockVisualizer other : registered)
			if (other != whoAsks && other.isStored(block))
				return true;

		return false;
	}

	// -------------------------------------------------------------------------------
	// Automatic listeners
	// -------------------------------------------------------------------------------

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		final BlockVisualizer v = findVisualizer(e.getBlock());

		if (v != null) {
			v.onRemove(e.getPlayer(), e.getBlock());

			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onMiddleClick(InventoryClickEvent e) {
		if (e.getAction() != InventoryAction.PLACE_ALL || e.getView().getType() != InventoryType.CREATIVE || e.getClick() != ClickType.CREATIVE || e.getCursor() == null)
			return;

		final Block block = Remain.getTargetBlock(e.getWhoClicked(), 5);

		if (findVisualizer(block) != null)
			e.setCancelled(true);
	}

	public static final class HeldListener implements Listener {

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onHeltItem(PlayerItemHeldEvent e) {
			final Player pl = e.getPlayer();

			final Tool curr = ToolRegistry.getTool(pl.getInventory().getItem(e.getNewSlot()));
			final Tool prev = ToolRegistry.getTool(pl.getInventory().getItem(e.getPreviousSlot()));

			// Player has attained focus
			if (curr != null) {

				if (prev != null) {

					// Not really
					if (prev.equals(curr))
						return;

					prev.onHotbarDefocused(pl);
				}

				curr.onHotbarFocused(pl);
			}
			// Player lost focus
			else if (prev != null)
				prev.onHotbarDefocused(pl);
		}
	}

	// -------------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------------

	private BlockVisualizer findVisualizer(Block block) {
		return block != null && canVisualize(block) ? findVisualizer0(block) : null;
	}

	private BlockVisualizer findVisualizer0(Block block) {
		for (final BlockVisualizer v : registered)
			if (v.isStored(block))
				return v;

		return null;
	}

	private boolean canVisualize(Block block) {
		return BlockUtil.isForBlockSelection(block.getType());
	}
}
