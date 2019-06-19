package org.mineacademy.fo.menu;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonReturnBack;
import org.mineacademy.fo.menu.button.ButtonRemove.RemoveConfirmButton;
import org.mineacademy.fo.menu.model.InventoryDrawer;

/**
 * A prepared menu to allow two-step object removal with a confirmation step
 */
public final class MenuDialogRemove extends Menu {

	/**
	 * The confirmation button that triggers the removal
	 */
	private final Button confirmButton;

	/**
	 * The return button
	 */
	private final Button returnButton;

	/**
	 * Create a new confirmation remove dialog
	 *
	 * @param parentMenu the parent menu
	 * @param confirmButton the remove button
	 */
	public MenuDialogRemove(Menu parentMenu, RemoveConfirmButton confirmButton) {
		this.confirmButton = confirmButton;
		this.returnButton = new ButtonReturnBack(parentMenu);
	}

	/**
	 * Returns the proper item at the correct slot
	 *
	 * @param slot the slot
	 * @return the item or null
	 */
	@Override
	public ItemStack getItemAt(int slot) {
		if (slot == 9 + 3)
			return confirmButton.getItem();

		if (slot == 9 + 5)
			return returnButton.getItem();

		return null;
	}

	/**
	 * Draws the inventory, 3 rows
	 *
	 * @return the inventory drawer
	 */
	@Override
	protected InventoryDrawer drawInventory() {
		return InventoryDrawer.of(9 * 3, getTitle());
	}

	/**
	 * Get the title for this menu
	 *
	 * @return "Confirm removal"
	 */
	@Override
	public String getTitle() {
		return "&0Confirm removal";
	}
}
