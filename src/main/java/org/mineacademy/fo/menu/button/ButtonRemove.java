package org.mineacademy.fo.menu.button;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Represents a standardized remove button that opens the remove confirmation dialog.
 *
 * Typically we use this to remove an arena, class, upgrade etc.
 */
@RequiredArgsConstructor
public class ButtonRemove extends Button {

	/**
	 * The parent menu
	 */
	private final Menu parentMenu;

	/**
	 * The type of the object to remove, for example class, upgrade, arena
	 */
	private final String toRemoveType;

	/**
	 * The name of the object to remove, for example "Warrior" for class
	 */
	private final String toRemoveName;

	/**
	 * The action that triggers when the object is removed
	 */
	private final ButtonRemoveAction removeAction;

	/**
	 * The icon for this button
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator

				.of(CompMaterial.LAVA_BUCKET)
				.name("&4&lRemove " + toRemoveName)

				.lores(Arrays.asList(
						"&r",
						"&7The selected " + toRemoveType + " will",
						"&7be removed permanently."))

				.flag(CompItemFlag.HIDE_ATTRIBUTES)
				.build().make();
	}

	public ItemStack getRemoveConfirmItem() {
		return ItemCreator

				.ofWool(CompColor.RED)
				.name("&6&lRemove " + toRemoveName)

				.lores(Arrays.asList(
						"&r",
						"&7Confirm that this " + toRemoveType + " will",
						"&7be removed permanently.",
						"&cCannot be undone."))

				.flag(CompItemFlag.HIDE_ATTRIBUTES)
				.build().make();
	}

	public String getMenuTitle() {
		return "&0Confirm removal";
	}

	/**
	 * Open the confirm dialog when clicked
	 */
	@Override
	public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
		new MenuDialogRemove(parentMenu, new RemoveConfirmButton()).displayTo(pl);
	}

	/**
	 * The button that when clicked, actually removes the object
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	final class RemoveConfirmButton extends Button {

		@Override
		public ItemStack getItem() {
			return getRemoveConfirmItem();
		}

		/**
		 * Remove the object using {@link ButtonRemove#removeAction}
		 */
		@Override
		public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
			player.closeInventory();
			removeAction.remove(toRemoveName);

			Common.tell(player, SimpleLocalization.Menu.ITEM_DELETED.replace("{item}", (!toRemoveType.isEmpty() ? toRemoveType + " " : "") + toRemoveName));
		}
	}

	/**
	 * Fires the action to remove the object
	 */

	@FunctionalInterface
	public interface ButtonRemoveAction {

		/**
		 * Remove the object
		 *
		 * @param object the object's name, for example "Warrior" for class
		 */
		void remove(String object);
	}

	/**
	 * A prepared menu to allow two-step object removal with a confirmation step
	 */
	final class MenuDialogRemove extends Menu {

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
		public MenuDialogRemove(final Menu parentMenu, final RemoveConfirmButton confirmButton) {
			super(parentMenu);

			this.confirmButton = confirmButton;
			returnButton = new ButtonReturnBack(parentMenu);

			setSize(9 * 3);
			setTitle(getMenuTitle());
		}

		/**
		 * Returns the proper item at the correct slot
		 *
		 * @param slot the slot
		 * @return the item or null
		 */
		@Override
		public ItemStack getItemAt(final int slot) {
			if (slot == 9 + 3)
				return confirmButton.getItem();

			if (slot == 9 + 5)
				return returnButton.getItem();

			return null;
		}

		/**
		 * Do not add twice return buttons
		 *
		 * @see org.mineacademy.fo.menu.Menu#addReturnButton()
		 */
		@Override
		protected boolean addReturnButton() {
			return false;
		}

		@Override
		protected String[] getInfo() {
			return null;
		}
	}
}