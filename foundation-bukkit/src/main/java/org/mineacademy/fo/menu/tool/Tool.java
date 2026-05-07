package org.mineacademy.fo.menu.tool;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMetadata;

/**
 * Represents a tool. A tool is a simple ItemStack that is registered within the
 * plugin and fires automatic events
 */
public abstract class Tool {

	/**
	 * Per-plugin NBT key stamped on every tool item. Because the key itself
	 * includes the plugin name, two Foundation-shaded plugins with visually-identical
	 * tools end up with disjoint key sets, so {@link ItemUtil#isSimilar} rejects
	 * cross-plugin matches and only the originating plugin's listener handles the click.
	 */
	private static final String TAG_PLUGIN = Platform.getPlugin().getName() + "_FoTool";

	/**
	 * The registered tools
	 */
	private static final Collection<Tool> tools = new ConcurrentLinkedQueue<>();

	/**
	 * Cached, plugin-tagged item produced by {@link #createItem()}. Built once on
	 * first access and reused by every {@link #getItem()} / {@link #isTool(ItemStack)}
	 * / {@link #give(Player)} call.
	 */
	private ItemStack item;

	/**
	 * Add a new tool to register.
	 * <p>
	 * Called automatically.
	 *
	 * @param tool the tool
	 */
	static void register(final Tool tool) {
		ValidCore.checkBoolean(!isRegistered(tool), "Tool with itemstack " + tool.getItem() + " already registered");

		tools.add(tool);
	}

	/**
	 * Checks if the tool is registered
	 *
	 * @param tool the tool
	 * @return true if the tool is registered
	 */
	static boolean isRegistered(final Tool tool) {
		return tools.contains(tool);
	}

	/**
	 * Attempts to find a registered tool from given itemstack
	 *
	 * @param item the item
	 * @return the corresponding tool, or null
	 */
	public static Tool getTool(final ItemStack item) {
		for (final Tool tool : tools)
			if (tool.isTool(item))
				return tool;

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
			} catch (final InterruptedException ex) {
				ex.printStackTrace();
			}

			// Sync to main thread
			Platform.runTask(() -> {
				final Tool instance = Tool.this;

				if (!isRegistered(instance))
					register(instance);
			});

		}).start();
	}

	/**
	 * Evaluates the given itemstack whether it is this tool
	 *
	 * @param item the itemstack
	 * @return true if this tool is the given itemstack
	 */
	public final boolean isTool(final ItemStack item) {
		return ItemUtil.isSimilar(this.getItem(), item, this.compareByNbt());
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
	public final boolean hasTool(final Player player) {
		for (final ItemStack item : player.getInventory().getContents())
			if (this.isTool(item))
				return true;

		return false;
	}

	/**
	 * Returns this tool's item, lazily built from {@link #createItem()} and stamped
	 * with this plugin's ownership NBT tag. Always tagged — there is no untagged
	 * accessor by design.
	 *
	 * @return the tool item
	 */
	public final ItemStack getItem() {
		if (this.item == null)
			this.item = CompMetadata.setMetadata(this.createItem(), TAG_PLUGIN, Platform.getPlugin().getName());

		return this.item;
	}

	/**
	 * Subclass hook for building the visual item. Called once, then cached and
	 * tagged by {@link #getItem()}. Use {@link ItemCreator}.
	 *
	 * @return the freshly built (untagged) item
	 */
	protected abstract ItemStack createItem();

	/**
	 * Called automatically when the tool is clicked
	 *
	 * @param event the event
	 */
	protected void onBlockClick(final PlayerInteractEvent event) {
	}

	/**
	 * Called automatically when the tool is clicked on an entity
	 *
	 * @param event the event
	 */
	protected void onEntityRightClick(final PlayerInteractEntityEvent event) {
	}

	/**
	 * Called automatically when a block is placed using this tool
	 *
	 * @param event
	 */
	protected void onBlockPlace(final BlockPlaceEvent event) {
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
	 * If true we ignore all matching except type and
	 * compare by NBT tags only.
	 *
	 * @return
	 */
	public boolean compareByNbt() {
		return false;
	}

	/**
	 * Gives this tool to player is he does not have it yet
	 *
	 * @param player
	 * @return true if tool was given, false if player already has it
	 */
	public final boolean giveIfHasnt(final Player player) {
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
		player.getInventory().setItem(slot, this.getItem().clone());
	}

	/**
	 * Convenience method for quickly adding this tool into a players inventory
	 *
	 * @param player
	 */
	public final void give(final Player player) {
		player.getInventory().addItem(this.getItem().clone());
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
