package org.mineacademy.fo.menu.model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * Represents a way to render the inventory to the player
 * using Bukkit/Spigot native methods.
 * <p>
 * This is also handy if you simply want to show
 * a certain inventory without creating the full menu.
 */
public final class InventoryDrawer {

	/**
	 * The size of the inventory.
	 */
	@Getter
	private final int size;

	/**
	 * The inventory title
	 */
	private String title;

	/**
	 * The items in this inventory
	 */
	private final ItemStack[] content;

	/**
	 * Create a new inventory drawer, see {@link #of(int, String)}
	 *
	 * @param size  the size
	 * @param title the title
	 */
	private InventoryDrawer(int size, String title) {
		this.size = size;
		this.title = title;

		this.content = new ItemStack[size];
	}

	/**
	 * Adds the item at the first empty slot starting from the 0 slot
	 * <p>
	 * If the inventory is full, we add it on the last slot replacing existing item
	 *
	 * @param item the item
	 */
	public void pushItem(ItemStack item) {
		boolean added = false;

		for (int i = 0; i < content.length; i++) {
			final ItemStack currentItem = content[i];

			if (currentItem == null) {
				content[i] = item;
				added = true;

				break;
			}
		}

		if (!added)
			content[size - 1] = item;
	}

	/**
	 * Is the current slot occupied by a non-null {@link ItemStack}?
	 *
	 * @param slot the slot
	 * @return true if the slot is occupied
	 */
	public boolean isSet(int slot) {
		return getItem(slot) != null;
	}

	/**
	 * Get an item at the slot, or null if slot overflown or item not set
	 *
	 * @param slot
	 * @return
	 */
	public ItemStack getItem(int slot) {
		return slot < content.length ? content[slot] : null;
	}

	/**
	 * Set an item at the certain slot
	 *
	 * @param slot
	 * @param item
	 */
	public void setItem(int slot, ItemStack item) {
		content[slot] = item;
	}

	/**
	 * Set the full content of this inventory
	 * <p>
	 * If the given content is shorter, all additional inventory slots are replaced with air
	 *
	 * @param newContent the new content
	 */
	public void setContent(ItemStack[] newContent) {
		for (int i = 0; i < content.length; i++)
			content[i] = i < newContent.length ? newContent[i] : new ItemStack(CompMaterial.AIR.getMaterial());
	}

	/**
	 * Set the title of this inventory drawer, not updating the inventory if it is being viewed
	 *
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Display this inventory to the player, closing older inventory if already opened
	 *
	 * @param player
	 */
	public void display(Player player) {
		final Inventory inv = this.build(player);

		player.openInventory(inv);
	}

	/**
	 * Builds the inventory
	 *
	 * @return
	 */
	public Inventory build() {
		return this.build(null);
	}

	/**
	 * Builds the inventory for the given holder
	 *
	 * @param holder
	 * @return
	 */
	public Inventory build(InventoryHolder holder) {

		// Automatically append the black color in the menu, can be overriden by colors
		final Inventory inv = Bukkit.createInventory(holder, size, Common.colorize("&0" + (title.length() > 30 ? title.substring(0, 30) : title)));

		inv.setContents(content);

		return inv;
	}

	/**
	 * Make a new inventory drawer
	 *
	 * @param size  the size
	 * @param title the title, colors will be replaced
	 * @return the inventory drawer
	 */
	public static InventoryDrawer of(int size, String title) {
		return new InventoryDrawer(size, title);
	}
}
