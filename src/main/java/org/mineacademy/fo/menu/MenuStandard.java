package org.mineacademy.fo.menu;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonReturnBack;
import org.mineacademy.fo.menu.button.Button.DummyButton;
import org.mineacademy.fo.menu.model.InventoryDrawer;
import org.mineacademy.fo.menu.model.ItemSetter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Menu aims to create a standard in creating beautiful and rich GUIs.
 *
 * This is the recommended standard menu class for creating menus automatically
 * having a parent menu, return button and an info button explaining the purpose
 * of the menu to the user.
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
public abstract class MenuStandard extends Menu {

	/**
	 * The size of the menu
	 */
	private Integer size = 9 * 3;

	/**
	 * The inventory title of the menu
	 */
	private String title;

	/**
	 * Parent menu
	 */
	private final Menu parent;

	/**
	 * The return button to the previous menu, null if none
	 */
	private final Button returnButton;

	/**
	 * Create a new menu with parent menu
	 *
	 * @param parent the parent menu
	 */
	protected MenuStandard(Menu parent) {
		this(parent, false);
	}

	/**
	 * Create a new menu with parent menu
	 *
	 * @param parent                 the parent
	 * @param returnMakesNewInstance should we re-instatiate the parent menu when
	 *                               returning to it?
	 */
	protected MenuStandard(Menu parent, boolean returnMakesNewInstance) {
		this.parent = parent;
		this.returnButton = parent != null ? new ButtonReturnBack(parent, returnMakesNewInstance) : Button.makeEmpty();
	}

	/**
	 * Automatically draw the inventory for the viewer
	 */
	@Override
	protected final InventoryDrawer drawInventory() {
		Objects.requireNonNull(size, "Size not set in " + this);
		Objects.requireNonNull(title, "Title not set in " + this);

		final InventoryDrawer inv = InventoryDrawer.of(size, title);

		drawBottomBar(inv);
		onDraw(inv);

		return inv;
	}

	// Draws the bottom bar containing info and return button
	private final void drawBottomBar(ItemSetter inv) {
		if (getInfo() != null)
			inv.setItem(getInfoButtonPosition(), Button.makeInfo(getInfo()).getItem());

		if (addReturnButton() && !(returnButton instanceof DummyButton))
			inv.setItem(getReturnButtonPosition(), returnButton.getItem());
	}

	/**
	 * Redraws and refreshes all buttons
	 */
	public final void restartMenu() {
		restartMenu(null);
	}

	/**
	 * Redraws and re-register all buttons while sending a title animation to the
	 * player
	 *
	 * @param animatedTitle the animated title
	 */
	public final void restartMenu(String animatedTitle) {
		registerButtons();
		redraw();

		if (animatedTitle != null)
			animateTitle(animatedTitle);
	}

	/**
	 * Redraws the bottom bar and updates inventory
	 */
	protected final void redraw() {
		final Inventory inv = getViewer().getOpenInventory().getTopInventory();
		Validate.isTrue(inv.getType() == InventoryType.CHEST, getViewer().getName() + "'s inventory closed in the meanwhile (now == " + inv.getType() + ").");

		for (int i = 0; i < size; i++) {
			final ItemStack item = getItemAt(i);

			Validate.isTrue(i < inv.getSize(), "Item (" + (item != null ? item.getType() : "null") + ") position (" + i + ") > inv size (" + inv.getSize() + ")");
			inv.setItem(i, item);
		}

		drawBottomBar((slot, item) -> inv.setItem(slot, item));
		getViewer().updateInventory();
	}

	/**
	 * Called automatically after the menu has been drawed
	 *
	 * Override for custom last-minute modifications
	 *
	 * @param drawer the drawer
	 */
	protected void onDraw(InventoryDrawer drawer) {
	}

	/**
	 * Get the information about this menu.
	 *
	 * Used to create an info bottom in bottom left corner, see
	 * {@link Button#makeInfo(String...)}
	 *
	 * @return the description of this menu, or null
	 */
	protected abstract String[] getInfo();

	/**
	 * Get the info button position
	 *
	 * @return the slot which info buttons is located on
	 */
	protected int getInfoButtonPosition() {
		return size - 9;
	}

	/**
	 * Should we automatically add the return button to the bottom left corner?
	 *
	 * @return true if the return button should be added, true by default
	 */
	protected boolean addReturnButton() {
		return true;
	}

	/**
	 * Get the return button position
	 *
	 * @return the slot which return buttons is located on
	 */
	protected int getReturnButtonPosition() {
		return size - 1;
	}

	/**
	 * Sets the title of this inventory, adding black color at the front
	 * automatically
	 *
	 * @param title the new title
	 */
	protected final void setTitle(String title) {
		this.title = "&0" + title;
	}

	/**
	 * Calculates the center slot of this menu
	 *
	 * @deprecated not exact
	 * @return the estimated center slot
	 */
	@Deprecated
	protected final int getCenterSlot() {
		int pos = Arrays.asList(13, 22, 31).contains(pos = size / 2) ? pos : pos - 5;

		return pos;
	}
}
