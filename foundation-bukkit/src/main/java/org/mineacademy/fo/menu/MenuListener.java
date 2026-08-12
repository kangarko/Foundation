package org.mineacademy.fo.menu;

import java.util.EnumSet;
import java.util.Set;

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
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

/**
 * The bukkit listener responsible for menus to function.
 */
public final class MenuListener implements Listener {

	/**
	 * The actions we hand over to the menu as a button or a menu click. Every other action is only
	 * prevented, never handled, so that no exotic click type can fire a button by accident.
	 */
	private static final Set<InventoryAction> HANDLED_ACTIONS = EnumSet.of(
			InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_SOME, InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_ONE,
			InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ONE,
			InventoryAction.SWAP_WITH_CURSOR, InventoryAction.CLONE_STACK, InventoryAction.MOVE_TO_OTHER_INVENTORY);

	/**
	 * Create a new menu listener
	 */
	public MenuListener() {
		try {
			Class.forName("org.bukkit.event.player.PlayerSwapHandItemsEvent");

			Platform.registerEvents(new PlayerSwapHandItemsListener());
		} catch (final Throwable t) {
			// Legacy MC
		}

		try {
			Class.forName("org.bukkit.event.inventory.InventoryDragEvent");

			Platform.registerEvents(new InventoryDragEventListener());
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

		if (menu == null)
			return;

		final int slot = event.getSlot();
		final ItemStack slotItem = event.getCurrentItem();
		final ItemStack cursor = event.getCursor();
		final Inventory clickedInv = Remain.getClickedInventory(event);

		final InventoryAction action = event.getAction();
		final MenuClickLocation whereClicked = clickedInv != null ? clickedInv.getType() == InventoryType.CHEST ? MenuClickLocation.MENU : MenuClickLocation.PLAYER_INVENTORY : MenuClickLocation.OUTSIDE;

		// Double clicking sweeps matching items out of every slot of both inventories at once, it is
		// not bound to the clicked slot so no per-slot rule can ever authorize it
		final boolean allowed = action != InventoryAction.COLLECT_TO_CURSOR && isAllowed(menu, whereClicked, slot, slotItem, cursor, action);

		if (whereClicked == MenuClickLocation.MENU && slotItem != null && HANDLED_ACTIONS.contains(action))
			try {
				Button button = menu.getButton(slot);

				if (button == null)
					button = menu.getButton(slotItem);

				if (button != null)
					menu.onButtonClick(player, slot, action, event.getClick(), button);
				else
					menu.onMenuClick(player, slot, action, event.getClick(), cursor, slotItem, !allowed);

			} catch (final Throwable t) {
				Common.tell(player, Lang.component("menu-error"));
				player.closeInventory();

				CommonCore.error(t, "Error clicking in menu " + menu);
			}

		// Prevent everything the menu did not explicitly allow, regardless of the action. Filtering
		// by action left holes such as number key swaps or double clicks moving items in and out
		if (!allowed) {
			event.setResult(Result.DENY);

			player.updateInventory();
		}

		// Spigot bug
		if (whereClicked != MenuClickLocation.PLAYER_INVENTORY && player.getGameMode() == GameMode.CREATIVE && event.getClick().toString().equals("SWAP_OFFHAND"))
			player.getInventory().setItemInOffHand(null);
	}

	/*
	 * Ask the menu if it permits the interaction, preventing it when the menu's own rule errors out
	 * so that a broken rule can never open a menu up instead of locking it down.
	 */
	static boolean isAllowed(final Menu menu, final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor, final InventoryAction action) {
		try {
			return menu.isActionAllowed(location, slot, clicked, cursor, action);

		} catch (final Throwable t) {
			CommonCore.error(t, "Error asking " + menu + " if " + action + " is allowed in " + location + " slot " + slot + ", preventing it");

			return false;
		}
	}
}

class PlayerSwapHandItemsListener implements Listener {

	/**
	 * Prevent swapping items when menu is opened to avoid duplication.
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSwapItems(final PlayerSwapHandItemsEvent event) {
		if (Menu.getMenu(event.getPlayer()) != null)
			event.setCancelled(true);
	}
}

class InventoryDragEventListener implements Listener {

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
		final InventoryType inventoryType = Remain.invokeInventoryViewMethod(event, "getType");

		if (menu == null || inventoryType != InventoryType.CHEST)
			return;

		final Inventory topInventory = Remain.invokeInventoryViewMethod(event, "getTopInventory");
		final int size = topInventory.getSize();
		final ItemStack cursor = CommonCore.getOrDefault(event.getCursor(), event.getOldCursor());

		// Dragging must obey the same rules as clicking, otherwise it becomes a way around them
		for (final int rawSlot : event.getRawSlots()) {
			final boolean inMenu = rawSlot < size;

			if (!MenuListener.isAllowed(menu, inMenu ? MenuClickLocation.MENU : MenuClickLocation.PLAYER_INVENTORY, inMenu ? rawSlot : rawSlot - size, event.getNewItems().get(rawSlot), cursor, InventoryAction.PLACE_SOME)) {
				event.setCancelled(true);

				return;
			}
		}
	}
}