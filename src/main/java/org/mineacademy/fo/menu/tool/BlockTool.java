package org.mineacademy.fo.menu.tool;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * A helper class for tools that may only be used on blocks
 */
public abstract class BlockTool extends Tool {

	/**
	 * The click event involved with this tool
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private PlayerInteractEvent event;

	@Override
	protected final void onBlockClick(final PlayerInteractEvent event) {
		this.event = event;

		final Player player = event.getPlayer();
		final Block block = event.getClickedBlock();

		final Action action = event.getAction();

		if (action == Action.RIGHT_CLICK_BLOCK)
			this.onBlockClick(player, ClickType.RIGHT, block);

		else if (action == Action.LEFT_CLICK_BLOCK)
			this.onBlockClick(player, ClickType.LEFT, block);

		else if (action == Action.RIGHT_CLICK_AIR)
			this.onAirClick(player, ClickType.RIGHT);

		else if (action == Action.LEFT_CLICK_AIR)
			this.onAirClick(player, ClickType.LEFT);
	}

	/**
	 * Called automatically when a player holding this tool clicks
	 * a block. The {@link ClickType} can only be RIGHT or LEFT here.
	 *
	 * @param player
	 * @param click
	 * @param block
	 */
	protected abstract void onBlockClick(Player player, ClickType click, Block block);

	/**
	 * Called automatically when a player clicks the air
	 *
	 * @param player
	 * @param click
	 */
	protected void onAirClick(final Player player, final ClickType click) {
	}

	/**
	 * Listen for air clicking to invoike {@link #onAirClick(Player, ClickType, Block)}
	 *
	 * @see org.mineacademy.fo.menu.tool.Tool#ignoreCancelled()
	 */
	@Override
	protected boolean ignoreCancelled() {
		return false;
	}
}
