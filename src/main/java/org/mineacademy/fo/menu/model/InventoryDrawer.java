package org.mineacademy.fo.menu.model;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompMaterial;

import java.lang.reflect.Method;

/**
 * Represents a way to render the inventory to the player
 * using Bukkit/Spigot native methods.
 * <p>
 * This is also handy if you simply want to show
 * a certain inventory without creating the full menu.
 */
public final class InventoryDrawer {

	/**
	 * The size of the inventory.
	 */
	@Getter
	private final int size;

	/**
	 * The inventory title
	 */
	private String title;

	/**
	 * The items in this inventory
	 */
	private final ItemStack[] content;

	/**
	 * Create a new inventory drawer, see {@link #of(int, String)}
	 *
	 * @param size  the size
	 * @param title the title
	 */
	private InventoryDrawer(int size, String title) {
		this.size = size;
		this.title = title;

		this.content = new ItemStack[size];
	}

	/**
	 * Adds the item at the first empty slot starting from the 0 slot
	 * <p>
	 * If the inventory is full, we add it on the last slot replacing existing item
	 *
	 * @param item the item
	 */
	public void pushItem(ItemStack item) {
		boolean added = false;

		for (int i = 0; i < content.length; i++) {
			final ItemStack currentItem = content[i];

			if (currentItem == null) {
				content[i] = item;
				added = true;

				break;
			}
		}

		if (!added)
			content[size - 1] = item;
	}

	/**
	 * Is the current slot occupied by a non-null {@link ItemStack}?
	 *
	 * @param slot the slot
	 * @return true if the slot is occupied
	 */
	public boolean isSet(int slot) {
		return getItem(slot) != null;
	}

	/**
	 * Get an item at the slot, or null if slot overflown or item not set
	 *
	 * @param slot
	 * @return
	 */
	public ItemStack getItem(int slot) {
		return slot < content.length ? content[slot] : null;
	}

	/**
	 * Set an item at the certain slot
	 *
	 * @param slot
	 * @param item
	 */
	public void setItem(int slot, ItemStack item) {
		content[slot] = item;
	}

	/**
	 * Set the full content of this inventory
	 * <p>
	 * If the given content is shorter, all additional inventory slots are replaced with air
	 *
	 * @param newContent the new content
	 */
	public void setContent(ItemStack[] newContent) {
		for (int i = 0; i < content.length; i++)
			content[i] = i < newContent.length ? newContent[i] : new ItemStack(CompMaterial.AIR.getMaterial());
	}

	/**
	 * Set the title of this inventory drawer, not updating the inventory if it is being viewed
	 *
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Display the inventory to the player, closing older inventory if already opened, this
	 * method will attempt to display the inventory in a nicer manner. For the sake of compatibility with
	 * existing code / existing behaviour this method will not log the error if one has occurred when attempting to do a nice display.
	 * Invoking this method is the equivalent of calling:
	 * <pre>
	 *   display(player, true, false)
	 * </pre>
	 *
	 * @param player the player
	 * @see #display(Player, boolean, boolean)
	 */
	public void display(Player player) {
		display(player, true, false);
	}

	/**
	 * Display the inventory to the player, closing older inventory if already opened.
	 * If the previous inventory a player has opened is a {@link org.mineacademy.fo.menu.Menu} this
	 * method will attempt to display the menu without the "close inventory" animation.
	 * @param player the player
	 * @param attemptNiceDisplay Whether a reflection based attempt for nicer handoff which stops the cursor from resetting / inventory close animation from running.
	 * @param logFailure Whether to log console with the error when trying to display the inventory nicely.
	 * @return Returns whether the attempt to display the menu nicely was successful.
	 */
	public boolean display(Player player, boolean attemptNiceDisplay, boolean logFailure) {
		// Automatically append the black color in the menu, can be overridden by colors
		final Inventory inv = Bukkit.createInventory(player, size, Common.colorize("&0" + title));

		inv.setContents(content);

		// Before opening make sure we close his old inventory if exist,
		// but only if the inventory is NOT a menu. If it is a menu, we can overwrite the contents,
		// as they will be re-rendered upon calling Menu#displayTo again. This will prevent the annoying
		// mouse position reset that happens when you move from inventory to inventory.
		boolean success = true;
		if (!player.hasMetadata("Ka_Menu"))
			player.closeInventory();
		else if (attemptNiceDisplay)
			try {
				final Method getHandle = ReflectionUtil.getMethod(ReflectionUtil.getOBCClass("CraftPlayer"), "getHandle");
				Valid.checkNotNull(getHandle, "Failed to find OBC CraftPlayer#getHandle method!");
				assert getHandle != null;
				final Class<?> entityHumanClass = ReflectionUtil.getNMSClass("EntityHuman");
				final Object entityHumanObject = getHandle.invoke(player); // We assume all {org.bukkit.Player} instances are the OBC variants (no custom players),
				final Method invCloseHandler = ReflectionUtil.getOBCClass("event.CraftEventFactory").getMethod("handleInventoryCloseEvent", entityHumanClass);
				ReflectionUtil.invokeStatic(invCloseHandler, entityHumanObject);
			} catch (ReflectiveOperationException ex) {
				if (logFailure)
					Common.error(ex, "Could not use internal CraftBukkit method to handle menu closing!");
				player.closeInventory(); // Close the inventory as we would without this workaround
				success = false;
			}
		player.openInventory(inv);
		return success;
	}

	/**
	 * Make a new inventory drawer
	 *
	 * @param size  the size
	 * @param title the title, colors will be replaced
	 * @return the inventory drawer
	 */
	public static InventoryDrawer of(int size, String title) {
		return new InventoryDrawer(size, title);
	}
}
