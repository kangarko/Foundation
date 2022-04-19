package org.mineacademy.fo.menu;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

/**
 * The bukkit listener responsible for menus to function.
 */
public final class MenuListener implements Listener {

	/**
	 * Create a new menu listener
	 */
	public MenuListener() {
		try {
			Class.forName("org.bukkit.event.player.PlayerSwapHandItemsEvent");

			Common.registerEvents(new OffHandListener());
		} catch (final Throwable t) {
			// Legacy MC
		}

		try {
			Class.forName("org.bukkit.event.inventory.InventoryDragEvent");

			Common.registerEvents(new DragListener());
		} catch (final Throwable t) {
			// Legacy MC
		}
	}

	/**
	 * Handles closing menus
	 *
	 * @param event the event
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMenuClose(final InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player))
			return;

		final Player player = (Player) event.getPlayer();
		final Menu menu = Menu.getMenu(player);

		if (menu != null)
			menu.handleClose(event.getInventory());

	}

	/**
	 * Handles clicking in menus
	 *
	 * @param event the event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onMenuClick(final InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player))
			return;

		final Player player = (Player) event.getWhoClicked();
		final Menu menu = Menu.getMenu(player);

		if (menu != null) {
			final ItemStack slotItem = event.getCurrentItem();
			final ItemStack cursor = event.getCursor();
			final Inventory clickedInv = Remain.getClickedInventory(event);

			final InventoryAction action = event.getAction();
			final MenuClickLocation whereClicked = clickedInv != null ? clickedInv.getType() == InventoryType.CHEST ? MenuClickLocation.MENU : MenuClickLocation.PLAYER_INVENTORY : MenuClickLocation.OUTSIDE;

			final boolean allowed = menu.isActionAllowed(whereClicked, event.getSlot(), slotItem, cursor, action);

			if (action.toString().contains("PICKUP") || action.toString().contains("PLACE") || action.toString().equals("SWAP_WITH_CURSOR") || action == InventoryAction.CLONE_STACK) {
				if (whereClicked == MenuClickLocation.MENU)
					try {
						final Button button = menu.getButton(slotItem);

						if (button != null)
							menu.onButtonClick(player, event.getSlot(), action, event.getClick(), button);
						else
							menu.onMenuClick(player, event.getSlot(), action, event.getClick(), cursor, slotItem, !allowed);

					} catch (final Throwable t) {
						Common.tell(player, SimpleLocalization.Menu.ERROR);
						player.closeInventory();

						Common.error(t, "Error clicking in menu " + menu);
					}

				if (!allowed) {
					event.setResult(Result.DENY);

					player.updateInventory();
				}

			} else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || whereClicked != MenuClickLocation.PLAYER_INVENTORY) {
				event.setResult(Result.DENY);
				player.updateInventory();

				// Spigot bug
				if (player.getGameMode() == GameMode.CREATIVE && event.getClick().toString().equals("SWAP_OFFHAND"))
					player.getInventory().setItemInOffHand(null);
			}
		}
	}

	private static final class OffHandListener implements Listener {

		/**
		 * Prevent swapping items when menu is opened to avoid duplication.
		 *
		 * @param event
		 */
		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onSwapItems(PlayerSwapHandItemsEvent event) {
			if (Menu.getMenu(event.getPlayer()) != null)
				event.setCancelled(true);
		}
	}

	private static final class DragListener implements Listener {

		/**
		 * Prevents players from putting disallowed items into slots, apparently Bukkit fires a drag event if the slot
		 * is clicked rapidly. Thanks to ItsRozzaDev for help!
		 *
		 * @param event
		 */
		@EventHandler
		public void onInventoryDragTop(final InventoryDragEvent event) {
			if (!(event.getWhoClicked() instanceof Player))
				return;

			final Player player = (Player) event.getWhoClicked();
			final Menu menu = Menu.getMenu(player);

			if (menu != null && event.getView().getType() == InventoryType.CHEST) {
				final int size = event.getView().getTopInventory().getSize();

				for (final int slot : event.getRawSlots()) {
					if (slot > size)
						continue;

					final ItemStack cursor = Common.getOrDefault(event.getCursor(), event.getOldCursor());

					if (!menu.isActionAllowed(MenuClickLocation.MENU, slot, event.getNewItems().get(slot), cursor, InventoryAction.PLACE_SOME)) {
						event.setCancelled(true);

						return;
					}
				}
			}
		}
	}
}
