package org.mineacademy.fo.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

/**
 * The bukkit listener responsible for menus to function.
 */
public final class MenuListener implements Listener {

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

		if (menu != null) {
			menu.onMenuClose(player, event.getInventory());

			player.removeMetadata(FoConstants.NBT.TAG_MENU_CURRENT, SimplePlugin.getInstance());
		}
	}

	/**
	 * Handles clicking in menus
	 *
	 * @param event the event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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

			final boolean allowed = menu.isActionAllowed(whereClicked, event.getSlot(), slotItem, cursor);

			if (action.toString().contains("PICKUP") || action.toString().contains("PLACE") || action.toString().equals("SWAP_WITH_CURSOR") || action == InventoryAction.CLONE_STACK) {
				if (whereClicked == MenuClickLocation.MENU)
					try {
						final Button button = menu.getButton(slotItem);

						if (button != null)
							menu.onButtonClick(player, event.getSlot(), action, event.getClick(), button);
						else
							menu.onMenuClick(player, event.getSlot(), action, event.getClick(), cursor, slotItem, !allowed);

					} catch (final Throwable t) {
						Common.tell(player, "&cOups! There was a problem with this menu! Please contact the administrator to review the console for details.");
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
			}
		}
	}
}
