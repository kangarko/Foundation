package org.mineacademy.fo.menu.tool;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.Valid;

/**
 * Represents a tool. A tool is a simple ItemStack that is registered within the
 * plugin and fires automatic events
 */
public abstract class Tool {

	/**
	 * The registered tools
	 */
	private static final Collection<Tool> tools = new ConcurrentLinkedQueue<>();

	/**
	 * Add a new tool to register.
	 * <p>
	 * Called automatically.
	 *
	 * @param tool the tool
	 */
	static synchronized void register(Tool tool) {
		Valid.checkBoolean(!isRegistered(tool), "Tool with itemstack " + tool.getItem() + " already registered");

		tools.add(tool);
	}

	/**
	 * Checks if the tool is registered
	 *
	 * @param tool the tool
	 * @return true if the tool is registered
	 */
	static synchronized boolean isRegistered(Tool tool) {
		return getTool(tool.getItem()) != null;
	}

	/**
	 * Attempts to find a registered tool from given itemstack
	 *
	 * @param item the item
	 * @return the corresponding tool, or null
	 */
	public static Tool getTool(ItemStack item) {
		for (final Tool t : tools)
			if (t.isTool(item))
				return t;

		return null;
	}

	/**
	 * Get all tools
	 *
	 * @return the registered tools array
	 */
	public static Tool[] getTools() {
		return tools.toArray(new Tool[tools.size()]);
	}

	// -------------------------------------------------------------------------------------------
	// Main class implementation
	// -------------------------------------------------------------------------------------------

	/**
	 * Create a new tool
	 */
	protected Tool() {

		// A hacky way of automatically registering it AFTER the parent constructor, assuming all went okay
		new Thread(() -> {

			try {
				Thread.sleep(3);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}

			final Tool instance = Tool.this;

			if (!isRegistered(instance))
				register(instance);
		}).start();
	}

	/**
	 * Evaluates the given itemstack whether it is this tool
	 *
	 * @param item the itemstack
	 * @return true if this tool is the given itemstack
	 */
	public final boolean isTool(final ItemStack item) {
		return ItemUtil.isSimilar(this.getItem(), item);
	}

	/**
	 * Return true if the given player holds this tool in his main hand
	 *
	 * @param player
	 * @return
	 */
	public final boolean hasToolInHand(final Player player) {
		return this.isTool(player.getItemInHand());
	}

	/**
	 * Return true if the player already contains this tool
	 *
	 * @param player
	 * @return
	 */
	public final boolean hasTool(Player player) {
		for (final ItemStack item : player.getInventory().getContents())
			if (this.isTool(item))
				return true;

		return false;
	}

	/**
	 * Get the tool item
	 * <p>
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
	protected void onBlockClick(PlayerInteractEvent event) {
	}

	/**
	 * Called automatically when a block is placed using this tool
	 *
	 * @param event
	 */
	protected void onBlockPlace(BlockPlaceEvent event) {
	}

	/**
	 * Called when the player swap items in their hotbar and the new slot matches
	 * this tool.
	 *
	 * @param player the player
	 */
	protected void onHotbarFocused(final Player player) {
	}

	/**
	 * Called when the player the tool is out of focus at hotbar
	 *
	 * @param player the player
	 */
	protected void onHotbarDefocused(final Player player) {
	}

	/**
	 * Should we fire {@link #onBlockClick(PlayerInteractEvent)} even on cancelled
	 * events?
	 * <p>
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
	 * default
	 */
	protected boolean autoCancel() {
		return false;
	}

	/**
	 * Gives this tool to player is he does not have it yet
	 *
	 * @param player
	 * @return true if tool was given, false if player already has it
	 */
	public final boolean giveIfHasnt(Player player) {
		if (this.hasTool(player))
			return false;

		this.give(player);
		return true;
	}

	/**
	 * Convenience method for quickly setting this tool to a specific slot of players inventory
	 *
	 * @param player
	 * @param slot
	 */
	public final void give(final Player player, final int slot) {
		player.getInventory().setItem(slot, this.getItem());
	}

	/**
	 * Convenience method for quickly adding this tool into a players inventory
	 *
	 * @param player
	 */
	public final void give(final Player player) {
		player.getInventory().addItem(this.getItem());
	}

	/**
	 * Returns true if the compared object is a tool with the same {@link #getItem()}
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public final boolean equals(final Object obj) {
		return obj instanceof Tool && ((Tool) obj).getItem().equals(this.getItem());
	}
}
