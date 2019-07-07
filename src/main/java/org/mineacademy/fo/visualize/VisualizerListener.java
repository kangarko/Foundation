package org.mineacademy.fo.visualize;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.collection.StrictList;
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
		final BlockVisualizer visualizer = findVisualizer(e.getBlock());

		if (visualizer != null) {
			visualizer.onRemove(e.getPlayer(), e.getBlock());

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

	// -------------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------------

	private BlockVisualizer findVisualizer(Block block) {
		return block != null && canVisualize(block) ? findVisualizer0(block) : null;
	}

	private BlockVisualizer findVisualizer0(Block block) {
		for (final BlockVisualizer visualizer : registered)
			if (visualizer.isStored(block))
				return visualizer;

		return null;
	}

	private boolean canVisualize(Block block) {
		return BlockUtil.isForBlockSelection(block.getType());
	}
}
