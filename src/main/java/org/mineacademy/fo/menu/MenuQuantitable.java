package org.mineacademy.fo.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuQuantity;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.NonNull;

/**
 * Advanced menu concept allowing to change quality of an item by more than 1 on
 * a single click.
 * <p>
 * For example: You want to chance the spawn percentage from 1% to 100% so you
 * set the editing quantity to 20 and you only need to click the item 5 times
 * instead of 99 times.
 * <p>
 * We added this as an interface so you can extend all other kinds of menus
 */
public interface MenuQuantitable {

	/**
	 * Get the current quantity of editing
	 *
	 * @return the quantity edit
	 */
	@NonNull
	MenuQuantity getQuantity();

	/**
	 * Set a new quantity of editing
	 *
	 * @param newQuantity the new quantity
	 */
	void setQuantity(@NonNull MenuQuantity newQuantity);

	/**
	 * Should we allow editing quantities below 1% such as 0.5%?
	 * Do not use for itemstack amounts.
	 *
	 * @return
	 */
	default boolean allowDecimalQuantities() {
		return false;
	}

	/**
	 * Get the next edit quantity from click
	 *
	 * @param clickType the click type
	 * @return the next quantity (higher or lower depending on the click)
	 */
	default double getNextQuantity(ClickType clickType) {
		return clickType == ClickType.LEFT ? -+getQuantity().getAmountDouble() : getQuantity().getAmountDouble();
	}

	/**
	 * Get the button that is responsible for setting the quantity edit
	 * Implemented by default.
	 *
	 * @param menu the menu
	 * @return the button that is responsible for setting the quantity edit
	 */
	default Button getEditQuantityButton(Menu menu) {
		return new Button() {

			@Override
			public final void onClickedInMenu(Player player, Menu clickedMenu, ClickType clickType) {
				final MenuQuantity nextQuantity = clickType == ClickType.LEFT ? getQuantity().previous(allowDecimalQuantities()) : getQuantity().next(allowDecimalQuantities());
				Valid.checkNotNull(nextQuantity, "Next quantity cannot be null. Current: " + getQuantity() + " Click: " + clickType);

				setQuantity(nextQuantity);

				menu.drawBottomAndSetSlots();
				menu.animateTitle("&9Editing quantity set to " + getCurrentEditAmountPercent());
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator
						.of(
								CompMaterial.STRING,
								"Edit Quantity: &7" + getCurrentEditAmountPercent(),
								"",
								"&8< &7Left click to decrease",
								"&8> &7Right click to increase")
						.build().make();
			}
		};
	}

	/**
	 * A utility method to get the chance quantity we are changing with each click,
	 * formatted with % sign appended.
	 *
	 * @return
	 */
	default String getCurrentEditAmountPercent() {
		final double percent = getQuantity().getAmountPercent();

		return (allowDecimalQuantities() ? MathUtil.formatTwoDigits(percent) : String.valueOf((int) percent)) + "%";
	}
}
