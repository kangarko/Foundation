package org.mineacademy.fo.menu.model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.CompChatColor;
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
	private ItemStack[] content;

	/**
	 * Create a new inventory drawer
	 *
	 * @param size  the size
	 * @param title the title
	 */
	public InventoryDrawer(final int size, final String title) {
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
	public void pushItem(final ItemStack item) {
		boolean added = false;

		for (int i = 0; i < this.content.length; i++) {
			final ItemStack currentItem = this.content[i];

			if (currentItem == null) {
				this.content[i] = item;
				added = true;

				break;
			}
		}

		if (!added)
			this.content[this.size - 1] = item;
	}

	/**
	 * Is the current slot occupied by a non-null {@link ItemStack}?
	 *
	 * @param slot the slot
	 * @return true if the slot is occupied
	 */
	public boolean isSet(final int slot) {
		return this.getItem(slot) != null;
	}

	/**
	 * Get an item at the slot, or null if slot overflown or item not set
	 *
	 * @param slot
	 * @return
	 */
	public ItemStack getItem(final int slot) {
		return slot < this.content.length ? this.content[slot] : null;
	}

	/**
	 * Set an item at the certain slot
	 *
	 * @param slot
	 * @param item
	 */
	public void setItem(final int slot, final ItemStack item) {
		Valid.checkBoolean(slot < this.content.length, "Cannot set item to slot " + slot + " as inventore has only a size of " + this.size + "! Item: " + item);

		this.content[slot] = item;
	}

	/**
	 * Set the full content of this inventory
	 * <p>
	 * If the given content is shorter, all additional inventory slots are replaced with air
	 *
	 * @param newContent the new content
	 */
	public void setContent(final ItemStack[] newContent) {
		if (this.content.length < newContent.length)
			this.content = new ItemStack[newContent.length];

		for (int i = 0; i < this.content.length; i++)
			this.content[i] = i < newContent.length ? newContent[i] : new ItemStack(CompMaterial.AIR.getMaterial());
	}

	/**
	 * Set the title of this inventory drawer, not updating the inventory if it is being viewed
	 *
	 * @param title
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * Display this inventory to the player, closing older inventory if already opened
	 *
	 * @param player
	 */
	public void display(final Player player) {
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
	public Inventory build(final InventoryHolder holder) {
		final String localTitle = MinecraftVersion.newerThan(V.v1_8) ? this.title : (this.title.length() > 30 ? this.title.substring(0, 30) : this.title);

		// Automatically append the black color in the menu, can be overriden by colors
		final Inventory inv = Bukkit.createInventory(holder, this.size, CompChatColor.translateColorCodes("<black>" + localTitle));

		inv.setContents(this.content);

		return inv;
	}
}
