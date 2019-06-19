package org.mineacademy.fo.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.event.MenuOpenEvent;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.InventoryDrawer;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.model.OneTimeRunnable;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompSound;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * The core class of Menu. Represents a simple menu.
 *
 * We advise you to extend {@link MenuStandard} as it offers much less work for
 * you as a developer and has more features covered. Also see other menus that
 * implement this for your convenience.
 */
public abstract class Menu {

	// --------------------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------------------

	/**
	 * The default sound when switching between menues.
	 */
	@Getter
	@Setter
	private static SimpleSound sound = new SimpleSound(CompSound.NOTE_STICKS.getSound(), .4F, 1F, true);

	/**
	 * An internal metadata tag the player gets when he opens the menu
	 *
	 * Used in {@link #getMenu(Player)}
	 */
	static final String TAG_CURRENT = "KaMenu_" + SimplePlugin.getNamed();

	/**
	 * An internal metadata tag the player gets when he opens another menu
	 *
	 * Used in {@link #getPreviousMenu(Player)}
	 */
	static final String TAG_PREVIOUS = "KaMenu_Previous_" + SimplePlugin.getNamed();

	/**
	 * Returns the current menu for player
	 *
	 * @param player the player
	 * @return the menu, or null if none
	 */
	public static Menu getMenu(Player player) {
		return getMenu0(player, TAG_CURRENT);
	}

	/**
	 * Returns the previous menu for player
	 *
	 * @param player the player
	 * @return the menu, or none
	 */
	public static Menu getPreviousMenu(Player player) {
		return getMenu0(player, TAG_PREVIOUS);
	}

	// Returns the menu associated with the players metadata, or null
	private static Menu getMenu0(Player player, String tag) {
		if (player.hasMetadata(tag)) {
			final Menu menu = (Menu) player.getMetadata(tag).get(0).value();
			Objects.requireNonNull(menu, "Menu missing from " + player.getName() + "'s Menu meta!");

			return menu;
		}

		return null;
	}

	// --------------------------------------------------------------------------------
	// Actual class
	// --------------------------------------------------------------------------------

	/**
	 * Automatically registered Buttons in this menu (using reflection)
	 */
	private final List<Button> registeredButtons = new ArrayList<>();

	/**
	 * The registrator responsible for scanning the class and making buttons
	 * function
	 */
	private final OneTimeRunnable buttonRegistrator;

	/**
	 * The viewer of this menu, is null until {@link #displayTo(Player)} is called
	 */
	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.PROTECTED)
	protected Player viewer;

	/**
	 * Create a new menu
	 */
	protected Menu() {
		buttonRegistrator = new OneTimeRunnable(() -> registerButtons());
	}

	// --------------------------------------------------------------------------------
	// Reflection to make life easier
	// --------------------------------------------------------------------------------

	/**
	 * Scans the menu class this menu extends and registers buttons
	 */
	protected final void registerButtons() {
		registeredButtons.clear();

		// Register buttons explicitly given
		{
			final List<Button> buttons = getButtonsToAutoRegister();

			if (buttons != null)
				registeredButtons.addAll(buttons);
		}

		// Register buttons declared as fields
		{
			Class<?> lookup = getClass();

			do {
				for (final Field f : lookup.getDeclaredFields())
					registerButton0(f);

			} while (Menu.class.isAssignableFrom(lookup = lookup.getSuperclass()) && !Menu.class.equals(lookup));
		}
	}

	// Scans the class and register fields that extend Button class
	private final void registerButton0(Field field) {
		field.setAccessible(true);

		final Class<?> type = field.getType();

		if (Button.class.isAssignableFrom(type)) {
			final Button button = (Button) ReflectionUtil.getFieldContent(field, this);

			Objects.requireNonNull(button, "Null button field named " + field.getName() + " in " + this);
			registeredButtons.add(button);
		}

		else if (Button[].class.isAssignableFrom(type)) {
			Validate.isTrue(Modifier.isFinal(field.getModifiers()), "Report / Button field must be final: " + field);
			final Button[] buttons = (Button[]) ReflectionUtil.getFieldContent(field, this);

			Validate.isTrue(buttons != null && buttons.length > 0, "Null " + field.getName() + "[] in " + this);
			registeredButtons.addAll(Arrays.asList(buttons));
		}
	}

	/**
	 * Returns a list of buttons that should be registered manually.
	 *
	 * If you simply have Button fields in your class, these are registered
	 * automatically
	 *
	 * @return button list, null by default
	 */
	protected List<Button> getButtonsToAutoRegister() {
		return null;
	}

	/**
	 * Attempts to find a clickable registered button in this menu having the same
	 * icon as the given item stack
	 *
	 * @param fromItem the itemstack to compare to
	 * @return the buttor or null if not found
	 */
	protected final Button getButton(ItemStack fromItem) {
		buttonRegistrator.runIfHasnt();

		if (fromItem != null)
			for (final Button button : registeredButtons) {
				Objects.requireNonNull(button, "Menu button is null at " + getClass().getSimpleName());
				Objects.requireNonNull(button.getItem(), "Itemstack cannot be null at " + button.getClass().getSimpleName());

				try {
					if (button.getItem().equals(fromItem))
						return button;
				} catch (final NullPointerException ex) {
				}
			}

		return null;
	}

	/**
	 * Return a new instance of this menu
	 *
	 * You must override this in certain cases
	 *
	 * @throws if new instance could not be made, for example when the menu is
	 *         taking constructor params
	 * @return the new instance, of null
	 */
	public Menu newInstance() {
		try {
			return ReflectionUtil.instatiate(getClass());
		} catch (final Throwable t) {

			try {
				final Object parent = getClass().getMethod("getParent").invoke(getClass());

				if (parent != null)
					return ReflectionUtil.instatiate(getClass(), parent);
			} catch (final Throwable tt) {
			}
		}

		throw new FoException("Could not make new instance of menu " + getClass() + ", please implement 'newInstance'!");
	}

	// --------------------------------------------------------------------------------
	// Actual menu functions
	// --------------------------------------------------------------------------------

	/**
	 * Draws the inventory in Bukkit
	 *
	 * See {@link MenuStandard} to handle this automatically for you
	 *
	 * @return the inventory drawer
	 */
	protected abstract InventoryDrawer drawInventory();

	/**
	 * Displays this menu to the player
	 *
	 * The menu will not be displayed when the player is having server conversation
	 *
	 * @param player the player
	 */
	public final void displayTo(Player player) {
		displayTo(player, false);
	}

	/**
	 * Display this menu to the player
	 *
	 * @param player                   the player
	 * @param ignoreServerConversation display menu even if the player is having
	 *                                 server conversation?
	 */
	public final void displayTo(Player player, boolean ignoreServerConversation) {
		this.viewer = player;

		buttonRegistrator.runIfHasnt();

		// Draw the menu
		final InventoryDrawer inv = drawInventory();

		for (int i = 0; i < inv.getSize(); i++) {
			final ItemStack item = getItemAt(i);

			if (item != null && !inv.isSet(i))
				inv.setItem(i, item);
		}

		// Call event after items have been set to allow to get them
		if (!Common.callEvent(new MenuOpenEvent(this, player)))
			return;

		// Prevent menu in conversation
		if (!ignoreServerConversation && player.isConversing()) {
			player.sendRawMessage(ChatColor.RED + "Type 'exit' to quit your conversation before opening menu.");

			return;
		}

		// Play the pop sound
		sound.play(player);

		// Register previous menu if exists
		{
			final Menu previous = getMenu(player);

			if (previous != null)
				player.setMetadata(TAG_PREVIOUS, new FixedMetadataValue(SimplePlugin.getInstance(), previous));
		}

		// Register current menu
		Common.runLater(1, () -> {
			inv.display(player);

			player.setMetadata(TAG_CURRENT, new FixedMetadataValue(SimplePlugin.getInstance(), Menu.this));
		});
	}

	/**
	 * The title of this menu
	 *
	 * @return the menu title
	 */
	public abstract String getTitle();

	/**
	 * Returns the item at a certain slot
	 *
	 * @param slot the slow
	 * @return the item, or null if no icon at the given slot
	 */
	public ItemStack getItemAt(int slot) {
		return null;
	}

	/**
	 * Animate the title of this menu
	 *
	 * Automatically reverts back to the old title after 1 second
	 *
	 * @param title the title to animate
	 */
	public final void animateTitle(String title) {
		PlayerUtil.animateInvTitle(this, getViewer(), title, getTitle());
	}

	// --------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------

	/**
	 * Called automatically when the menu is clicked.
	 *
	 * By default we call the shorter {@link #onMenuClick(Player, int, ItemStack)}
	 * method.
	 *
	 * @param player    the player
	 * @param slot      the slot
	 * @param action    the action
	 * @param click     the click
	 * @param cursor    the cursor
	 * @param clicked   the item clicked
	 * @param cancelled is the event cancelled?
	 */
	protected void onMenuClick(Player player, int slot, InventoryAction action, ClickType click, ItemStack cursor, ItemStack clicked, boolean cancelled) {
		onMenuClick(player, slot, clicked);
	}

	/**
	 * Called automatically when the menu is clicked
	 *
	 * @param player  the player
	 * @param slot    the slot
	 * @param clicked the item clicked
	 */
	protected void onMenuClick(Player player, int slot, ItemStack clicked) {
	}

	/**
	 * Called automatically when a registered button is clicked
	 *
	 * By default this method parses the click into
	 * {@link Button#onClickedInMenu(Player, Menu, ClickType)}
	 *
	 * @param player the player
	 * @param slot   the slot
	 * @param action the action
	 * @param click  the click
	 * @param button the button
	 */
	protected void onButtonClick(Player player, int slot, InventoryAction action, ClickType click, Button button) {
		button.onClickedInMenu(player, this, click);
	}

	/**
	 * Called automatically when the menu is closed
	 *
	 * @param player    the player
	 * @param inventory the menu inventory that is being closed
	 */
	protected void onMenuClose(Player player, Inventory inventory) {
	}

	/**
	 * Should we prevent the click or drag?
	 *
	 * @param location the click location
	 * @param slot     the slot
	 * @param clicked  the clicked item
	 * @param cursor   the cursor
	 * @deprecated sometimes does not work correctly due to flaws in server to
	 *             client packet communication do not rely on this
	 * @return if the action is cancelled in the {@link InventoryClickEvent}, false
	 *         by default
	 */
	@Deprecated
	protected boolean isActionAllowed(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor) {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{}";
	}
}
