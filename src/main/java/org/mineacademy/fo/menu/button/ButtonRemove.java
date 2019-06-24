package org.mineacademy.fo.menu.button;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompDye;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Represents a standardized remove button that open the remove confirmation dialog.
 *
 * Typically we use this to remove an arena, class, upgrade etc.
 */
@RequiredArgsConstructor
public final class ButtonRemove extends Button {

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

	/**
	 * Open the confirm dialog when clicked
	 */
	@Override
	public void onClickedInMenu(Player pl, Menu menu, ClickType click) {
		new MenuDialogRemove(parentMenu, new RemoveConfirmButton()).displayTo(pl);
	}

	/**
	 * The button that when clicked, actually removes the object
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	final class RemoveConfirmButton extends Button {

		@Override
		public ItemStack getItem() {
			return ItemCreator

					.ofWool(CompDye.RED)
					.name("&6&lRemove " + toRemoveName)

					.lores(Arrays.asList(
							"&r",
							"&7Confirm that this " + toRemoveType + " will",
							"&7be removed permanently.",
							"&cCannot be undone."))

					.flag(CompItemFlag.HIDE_ATTRIBUTES)
					.build().make();
		}

		/**
		 * Remove the object using {@link ButtonRemove#removeAction}
		 */
		@Override
		public void onClickedInMenu(Player player, Menu menu, ClickType click) {
			player.closeInventory();
			removeAction.remove(toRemoveName);

			Common.tell(player, SimpleLocalization.Menu.ITEM_DELETED.replace("{item}", (!toRemoveType.isEmpty() ? toRemoveType + " " : "") + toRemoveName));
		}
	}

	/**
	 * Fires the action to remove the object
	 */
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
		public MenuDialogRemove(Menu parentMenu, RemoveConfirmButton confirmButton) {
			super(parentMenu);

			this.confirmButton = confirmButton;
			this.returnButton = new ButtonReturnBack(parentMenu);

			setSize(9 * 3);
			setTitle("&0Confirm removal");
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

		@Override
		protected String[] getInfo() {
			return null;
		}
	}
}