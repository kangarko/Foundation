package org.mineacademy.fo.menu;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;
import lombok.Setter;

/**
 * A simple menu allowing players to drop or take items.
 *
 * from the container. You can connect this with your file storing
 * system to save or load items edited by players in the container.
 */
public abstract class MenuContainer extends Menu {

	/**
	 * The filler item we fill the bottom bar with for safety.
	 */
	@Getter
	@Setter
	private ItemStack bottomBarFillerItem = ItemCreator.of(CompMaterial.LIGHT_GRAY_STAINED_GLASS_PANE, " ").make();

	/**
	 * Create a new menu that can edit chances of the items you put inside.
	 */
	protected MenuContainer() {
		this(null);
	}

	/**
	 * Create a new menu that can edit chances of the items you put inside.
	 *
	 * @param parent
	 */
	protected MenuContainer(Menu parent) {
		this(parent, false);
	}

	/**
	 * Create a new menu that can edit chances of the items you put inside.
	 *
	 * @param parent
	 * @param returnMakesNewInstance should we re-instatiate the parent menu when
	 *                               returning to it?
	 */
	protected MenuContainer(Menu parent, boolean returnMakesNewInstance) {
		super(parent, returnMakesNewInstance);

		// Default the size to 3 rows (+ 1 bottom row is added automatically)
		this.setSize(9 * 3);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting items
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
	 */
	@Override
	public final ItemStack getItemAt(int slot) {

		final ItemStack customDrop = this.getDropAt(slot);

		if (customDrop != null)
			return customDrop;

		if (slot > this.getSize() - 9)
			return this.bottomBarFillerItem;

		return NO_ITEM;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Allowing clicking
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see org.mineacademy.fo.menu.Menu#isActionAllowed(org.mineacademy.fo.menu.model.MenuClickLocation, int, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack)
	 */
	@Override
	public final boolean isActionAllowed(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor, final InventoryAction action) {

		if (location != MenuClickLocation.MENU && action != InventoryAction.MOVE_TO_OTHER_INVENTORY)
			return true;

		if (!this.canEditItem(location, slot, clicked, cursor, action))
			return false;

		return true;
	}

	/**
	 * Return true for the slots you want players to be able to edit.
	 * By default we enable them to edit anything above the bottom bar.
	 *
	 * This is called from {@link #isActionAllowed(MenuClickLocation, int, ItemStack, ItemStack)} and
	 * by defaults forwards the call to {@link #canEditItem(int)}
	 *
	 * Bottom row is always protected by
	 *
	 * @param location
	 * @param slot
	 * @param clicked
	 * @param cursor
	 * @param action
	 *
	 * @return
	 */
	protected boolean canEditItem(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor, InventoryAction action) {
		return this.canEditItem(slot);
	}

	/**
	 * Return the slot numbers for which you want to allow
	 * items to get edited in your menu (if you do not want
	 * to allow editing the entire container window).
	 *
	 * @param slot
	 * @return
	 */
	protected boolean canEditItem(int slot) {
		return slot <= this.getSize() - 9;
	}

	/**
	 * Return the item that should appear at the given slot,
	 * you should load items from your data file or cache here.
	 *
	 * @param slot
	 * @return
	 */
	protected abstract ItemStack getDropAt(int slot);

	// ------------------------------------------------------------------------------------------------------------
	// Handling clicking
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see org.mineacademy.fo.menu.Menu#onMenuClick(org.bukkit.entity.Player, int, org.bukkit.event.inventory.InventoryAction, org.bukkit.event.inventory.ClickType, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack, boolean)
	 */
	@Override
	protected final void onMenuClick(Player player, int slot, InventoryAction action, ClickType clickType, ItemStack cursor, ItemStack clicked, boolean cancelled) {

		if (this.canEditItem(slot) && slot < this.getSize() - 9) {

			// Call our handler
			clicked = this.onItemClick(slot, clickType, clicked);

			// Update item
			this.setItem(slot, clicked);
		}
	}

	/*
	 * @see org.mineacademy.fo.menu.Menu#onMenuClick(org.bukkit.entity.Player, int, org.bukkit.inventory.ItemStack)
	 */
	@Override
	protected final void onMenuClick(Player player, int slot, ItemStack clicked) {
		throw new FoException("unsupported call");
	}

	/**
	 * Called automatically when the given slot is clicked,
	 * you can edit the clicked item here or simply pass it through.
	 *
	 * @param slot
	 * @param clickType
	 * @param item
	 * @return
	 */
	protected ItemStack onItemClick(int slot, ClickType clickType, @Nullable ItemStack item) {
		return item;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Handling saving
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see org.mineacademy.fo.menu.Menu#onMenuClose(org.bukkit.entity.Player, org.bukkit.inventory.Inventory)
	 */
	@Override
	protected final void onMenuClose(Player player, Inventory inventory) {
		final StrictMap<Integer, ItemStack> items = new StrictMap<>();

		for (int slot = 0; slot < this.getSize() - 9; slot++)
			if (this.canEditItem(slot)) {
				final ItemStack item = inventory.getItem(slot);

				items.put(slot, item);
			}

		this.onMenuClose(items);
	}

	/**
	 * Called automatically when you should save all editable slots stored in the map
	 * by slot, with their items (nullable).
	 *
	 * @param items
	 */
	protected abstract void onMenuClose(StrictMap<Integer, ItemStack> items);

	// ------------------------------------------------------------------------------------------------------------
	// Decoration
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return new String[] {
				"This menu allows you to drop",
				"items to this container.",
				"",
				"Simply &2drag and drop &7items",
				"from your inventory here."
		};
	}
}
