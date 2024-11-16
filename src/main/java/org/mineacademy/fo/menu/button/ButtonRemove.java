package org.mineacademy.fo.menu.button;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Represents a standardized remove button that opens the remove confirmation dialog.
 * <p>
 * Typically we use this to remove an arena, class, upgrade etc.
 */
@RequiredArgsConstructor
public class ButtonRemove extends Button {

	/**
	 * The remove menu title
	 */
	@Getter
	@Setter
	private static String menuTitle = SimpleLocalization.Menu.MENU_REMOVE_TITLE;

	/**
	 * The remove button material
	 */
	@Getter
	@Setter
	private static CompMaterial material = CompMaterial.LAVA_BUCKET;

	/**
	 * The remove button item name
	 */
	@Getter
	@Setter
	private static String title = SimpleLocalization.Menu.BUTTON_REMOVE_TITLE;

	/**
	 * The remove button item lore
	 */
	@Getter
	@Setter
	private static List<String> lore = Arrays.asList(SimpleLocalization.Menu.BUTTON_REMOVE_LORE);

	/**
	 * The remove button material
	 */
	@Getter
	@Setter
	private static CompMaterial removeConfirmMaterial = CompMaterial.RED_WOOL;

	/**
	 * The remove button item name
	 */
	@Getter
	@Setter
	private static String removeConfirmTitle = SimpleLocalization.Menu.BUTTON_REMOVE_CONFIRM_TITLE;

	/**
	 * The remove button item lore
	 */
	@Getter
	@Setter
	private static List<String> removeConfirmLore = Arrays.asList(SimpleLocalization.Menu.BUTTON_REMOVE_CONFIRM_LORE);

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
	private final Runnable removeAction;

	/**
	 * The icon for this button
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator

				.of(material)
				.name(title.replace("{name}", this.toRemoveName))

				.lore(Replacer.replaceArray(lore,
						"name", this.toRemoveName,
						"type", this.toRemoveType))

				.flags(CompItemFlag.HIDE_ATTRIBUTES)
				.make();
	}

	/**
	 * The icon to confirm removal
	 *
	 * @return
	 */
	public ItemStack getRemoveConfirmItem() {
		return ItemCreator

				.of(removeConfirmMaterial)
				.name(removeConfirmTitle.replace("{name}", this.toRemoveName))

				.lore(Replacer.replaceArray(removeConfirmLore,
						"name", this.toRemoveName,
						"type", this.toRemoveType))

				.flags(CompItemFlag.HIDE_ATTRIBUTES)
				.make();
	}

	/**
	 * Open the confirm dialog when clicked
	 */
	@Override
	public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
		new MenuDialogRemove(this.parentMenu, new RemoveConfirmButton()).displayTo(pl);
	}

	/**
	 * The button that when clicked, actually removes the object
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	final class RemoveConfirmButton extends Button {

		@Override
		public ItemStack getItem() {
			return ButtonRemove.this.getRemoveConfirmItem();
		}

		/**
		 * Remove the object using {@link ButtonRemove#removeAction}
		 */
		@Override
		public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
			player.closeInventory();
			ButtonRemove.this.removeAction.run();

			Common.tell(player, SimpleLocalization.Menu.ITEM_DELETED.replace("{item}", (!ButtonRemove.this.toRemoveType.isEmpty() ? ButtonRemove.this.toRemoveType + " " : "") + ButtonRemove.this.toRemoveName));
		}
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
		 * @param parentMenu    the parent menu
		 * @param confirmButton the remove button
		 */
		public MenuDialogRemove(final Menu parentMenu, final RemoveConfirmButton confirmButton) {
			super(parentMenu);

			this.confirmButton = confirmButton;
			this.returnButton = new ButtonReturnBack(parentMenu);

			this.setSize(9 * 3);
			this.setTitle(menuTitle);
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
				return this.confirmButton.getItem();

			if (slot == 9 + 5)
				return this.returnButton.getItem();

			return NO_ITEM;
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