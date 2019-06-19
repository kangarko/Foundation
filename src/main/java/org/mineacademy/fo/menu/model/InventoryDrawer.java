package org.mineacademy.fo.menu.model;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;

import lombok.Getter;

/**
 * Represents a way to render the inventory to the player
 * using Bukkit/Spigot native methods.
 *
 * This is also handy if you simply want to show
 * a certain inventory without creating the full menu.
 */
public final class InventoryDrawer implements ItemSetter {

	/**
	 * The size of the inventory.
	 */
	@Getter
	private final int size;

	/**
	 * The inventory title
	 */
	private final String title;

	/**
	 * The items in this inventory
	 */
	private final ItemStack[] content;

	/**
	 * Create a new inventory drawer, see {@link #of(int, String)}
	 *
	 * @param size the size
	 * @param title the title
	 */
	private InventoryDrawer(int size, String title) {
		this.size = size;
		this.title = title;

		this.content = new ItemStack[size];
	}

	/**
	 * Adds the item at the first empty slot starting from the 0 slot
	 *
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
		return slot < content.length && content[slot] != null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setItem(int slot, ItemStack item) {
		content[slot] = item;
	}

	/**
	 * Set the full content of this inventory
	 *
	 * If the given content is shorter, all additional inventory slots are replaced with air
	 *
	 * @param newContent the new content
	 */
	public void setContent(ItemStack[] newContent) {
		for (int i = 0; i < content.length; i++)
			content[i] = i < newContent.length ? newContent[i] : new ItemStack(Material.AIR);
	}

	/**
	 * Display the inventory to the player
	 *
	 * @param player the player
	 */
	public void display(Player player) {
		final Inventory inv = Bukkit.createInventory(player, size, Common.colorize(title));
		inv.setContents(content);

		if (player.getOpenInventory() != null)
			player.closeInventory();

		player.openInventory(inv);
	}

	/**
	 * Make a new inventory drawer
	 *
	 * @param size the size
	 * @param title the title, colors will be replaced
	 * @return the inventory drawer
	 */
	public static InventoryDrawer of(int size, String title) {
		return new InventoryDrawer(size, title);
	}
}
