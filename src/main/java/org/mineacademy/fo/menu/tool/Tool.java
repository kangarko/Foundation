package org.mineacademy.fo.menu.tool;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.menu.model.ItemCreator;

/**
 * Represents a tool. A tool is a simple ItemStack that is registered within the
 * plugin and fires automatic events
 */
public abstract class Tool {

	/**
	 * Create a new tool
	 */
	protected Tool() {

		// A hacky way of automatically registering it AFTER the parent constructor, assuming all went okay
		new Thread() {

			@Override
			public void run() {

				try {
					Thread.sleep(3);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}

				final Tool instance = Tool.this;

				if (!ToolRegistry.isRegistered(instance))
					ToolRegistry.register(instance);
			}
		}.start();
	}

	/**
	 * Evaluates the given itemstack whether it is this tool
	 *
	 * @param item the itemstack
	 * @return true if this tool is the given itemstack
	 */
	public boolean isTool(ItemStack item) {
		return ItemUtil.isSimilar(getItem(), item);
	}

	/**
	 * Get the tool item
	 *
	 * TIP: Use {@link ItemCreator}
	 *
	 * @return the tool item
	 */
	public abstract ItemStack getItem();

	/**
	 * Called automatically when the tool is clicked
	 *
	 * @param event the event
	 */
	protected abstract void onBlockClick(PlayerInteractEvent event);

	/**
	 * Called when the player swap items in their hotbar and the new slot matches
	 * this tool.
	 *
	 * @param player the player
	 */
	protected void onHotbarFocused(Player player) {
	}

	/**
	 * Called when the player the tool is out of focus at hotbar
	 *
	 * @param player the player
	 */
	protected void onHotbarDefocused(Player player) {
	}

	/**
	 * Should we fire {@link #onBlockClick(PlayerInteractEvent)} even on cancelled
	 * events?
	 *
	 * True by default. Set to false if you want to catch clicking air.
	 *
	 * @return true if we should ignore the click event if it was cancelled
	 */
	protected boolean ignoreCancelled() {
		return true;
	}

	/**
	 * A convenience method, should we automatically cancel the
	 * {@link PlayerInteractEvent} ?
	 *
	 * @return true if the interact event should be cancelled automatically false by
	 *         default
	 */
	protected boolean autoCancel() {
		return false;
	}

	/**
	 * Convenience method for quickly setting this tool to a specific slot of players inventory
	 *
	 * @param player
	 */
	public final void give(Player player, int slot) {
		player.getInventory().setItem(slot, getItem());
	}

	/**
	 * Convenience method for quickly adding this tool into a players inventory
	 *
	 * @param player
	 */
	public final void give(Player player) {
		player.getInventory().addItem(getItem());
	}

	/**
	 * Returns true if the compared object is a tool with the same {@link #getItem()}
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public final boolean equals(Object obj) {
		return obj instanceof Tool && ((Tool) obj).getItem().equals(getItem());
	}
}
