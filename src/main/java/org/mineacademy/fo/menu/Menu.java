package org.mineacademy.fo.menu;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.*;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.event.MenuCloseEvent;
import org.mineacademy.fo.event.MenuOpenEvent;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.Button.DummyButton;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.InventoryDrawer;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.settings.SimpleLocalization;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The core class of Menu. Represents a simple menu.
 *
 * <p>
 * This is the recommended menu class for all plugins having menus. It offers
 * having a parent menu, a return button and an info button explaining the
 * purpose of the menu to the user.
 *
 * <p>
 * HOW TO GET STARTED: Place final {@link Button} fields in your menu class and
 * make a instatiate when in constructor. Those will be registered as clickable
 * automatically. To render them, override {@link #getItemAt(int)} and make them
 * return at your desired positions.
 *
 * @deprecated use {@link AdvancedMenu}
 */
@Deprecated
public abstract class Menu {

	// --------------------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------------------

	/**
	 * The default sound when switching between menus.
	 */
	@Getter
	@Setter
	private static SimpleSound sound = new SimpleSound(CompSound.NOTE_STICKS.getSound(), .4F);

	/**
	 * Should we animate menu titles?
	 */
	@Getter
	@Setter
	private static boolean titleAnimationEnabled = true;

	/**
	 * The default duration of the new animated title before
	 * it is reverted back to the old one
	 * <p>
	 * Used in {@link PlayerUtil#updateInventoryTitle(Menu, Player, String, String, int)}
	 */
	@Setter
	private static int titleAnimationDurationTicks = 20;

	/**
	 * A placeholder to represent that no item should be displayed/returned
	 */
	protected static final ItemStack NO_ITEM = null;

	// --------------------------------------------------------------------------------
	// Actual class
	// --------------------------------------------------------------------------------

	/**
	 * Automatically registered Buttons in this menu (using reflection)
	 */
	private final Map<Button, Position> registeredButtons = new HashMap<>();

	/**
	 * The registrator responsible for scanning the class and making buttons
	 * function
	 */
	private boolean buttonsRegistered = false;

	/**
	 * Parent menu
	 */
	private final Menu parent;

	// --------------------------------------------------------------------------------
	// Other constructors
	// --------------------------------------------------------------------------------

	/**
	 * The inventory title of the menu, colors & are supported
	 */
	private String title = "&0Menu";

	/**
	 * The size of the menu
	 */
	private Integer size = 9 * 3;

	/**
	 * The description of the menu
	 */
	private final String[] info = null;

	/**
	 * The viewer of this menu, is null until {@link #displayTo(Player)} is called
	 */
	private Player viewer;

	/**
	 * Debug option to render empty spaces as glass panel having the slot id visible
	 */
	private boolean slotNumbersVisible;

	/**
	 * A one way boolean indicating this menu has been opened at least once
	 */
	private boolean opened = false;

	/**
	 * A one way boolean set to true in {@link #handleClose(Inventory)}
	 */
	private boolean closed = false;

	/**
	 * Special case button only registered if this menu is {@link MenuQuantitable}
	 */
	@Nullable
	private final Button quantityButton;

	/**
	 * Create a new menu without parent menu with the size of 9*3
	 *
	 * <p>
	 * You are encouraged to change the size and title of this menu in your
	 * constructor by calling {@link #setTitle(String)} and
	 * {@link #setSize(Integer)}
	 *
	 * <p>
	 * NB: The {@link #getViewer()} of this menu is yet null!
	 */
	protected Menu() {
		this(null);
	}

	/**
	 * Create a new menu with parent menu with the size of 9*3
	 *
	 * <p>
	 * You are encouraged to change the size and title of this menu in your
	 * constructor by calling {@link #setTitle(String)} and
	 * {@link #setSize(Integer)}
	 *
	 * <p>
	 * NB: The {@link #getViewer()} of this menu is yet null!
	 *
	 * @param parent the parent menu
	 */
	protected Menu(final Menu parent) {
		this(parent, false);
	}

	/**
	 * Create a new menu with parent menu with the size of 9*3
	 *
	 * <p>
	 * You are encouraged to change the size and title of this menu in your
	 * constructor by calling {@link #setTitle(String)} and
	 * {@link #setSize(Integer)}
	 *
	 * <p>
	 * NB: The {@link #getViewer()} of this menu is yet null!
	 *
	 * @param parent                 the parent
	 * @param returnMakesNewInstance should we re-instatiate the parent menu when
	 *                               returning to it?
	 */
	protected Menu(final Menu parent, final boolean returnMakesNewInstance) {
		this.parent = parent;
		this.quantityButton = this instanceof MenuQuantitable ? ((MenuQuantitable) this).getQuantityButton(this) : Button.makeEmpty();
	}

	/**
	 * Returns the current menu for player
	 *
	 * @param player the player
	 * @return the menu, or null if none
	 */
	public static final Menu getMenu(final Player player) {
		return getMenu0(player, FoConstants.NBT.TAG_MENU_CURRENT);
	}

	/**
	 * Returns the previous menu for player
	 *
	 * @param player the player
	 * @return the menu, or none
	 */
	@Deprecated
	public static final Menu getPreviousMenu(final Player player) {
		return getMenu0(player, FoConstants.NBT.TAG_MENU_PREVIOUS);
	}

	/**
	 * Returns the last closed menu, null if does not exist.
	 *
	 * @param player
	 * @return
	 */
	@Nullable
	public static final Menu getLastClosedMenu(final Player player) {
		if (player.hasMetadata(FoConstants.NBT.TAG_MENU_LAST_CLOSED)) {
			final Menu menu = (Menu) player.getMetadata(FoConstants.NBT.TAG_MENU_LAST_CLOSED).get(0).value();

			return menu;
		}

		return null;
	}

	// Returns the menu associated with the players metadata, or null
	private static Menu getMenu0(final Player player, final String tag) {
		if (player.hasMetadata(tag)) {
			final Menu menu = (Menu) player.getMetadata(tag).get(0).value();
			Valid.checkNotNull(menu, "Menu missing from " + player.getName() + "'s metadata '" + tag + "' tag!");

			return menu;
		}

		return null;
	}

	// --------------------------------------------------------------------------------
	// Reflection to make life easier
	// --------------------------------------------------------------------------------

	/**
	 * Scans the menu class this menu extends and registers buttons
	 */
	final void registerButtons() {
		this.registeredButtons.clear();

		// Register buttons explicitly given
		{
			final List<Button> buttons = this.getButtonsToAutoRegister();

			if (buttons != null) {
				final Map<Button, Position> buttonsRemapped = new HashMap<>();

				for (final Button button : buttons)
					buttonsRemapped.put(button, null);

				this.registeredButtons.putAll(buttonsRemapped);
			}
		}

		// Register buttons declared as fields
		{
			Class<?> lookup = this.getClass();

			do
				for (final Field f : lookup.getDeclaredFields())
					this.registerButton0(f);
			while (Menu.class.isAssignableFrom(lookup = lookup.getSuperclass()));
		}
	}

	// Scans the class and register fields that extend Button class
	private void registerButton0(final Field field) {
		field.setAccessible(true);

		final Class<?> type = field.getType();

		if (Button.class.isAssignableFrom(type)) {
			final Button button = (Button) ReflectionUtil.getFieldContent(field, this);

			Valid.checkNotNull(button, "Null button field named " + field.getName() + " in " + this);
			final Position position = field.getAnnotation(Position.class);

			if (!(button instanceof DummyButton))
				this.registeredButtons.put(button, position);

		} else if (Button[].class.isAssignableFrom(type))
			throw new FoException("Button[] is no longer supported in menu for " + this.getClass());
	}

	/*
	 * Utility method to register buttons if they yet have not been registered
	 *
	 * This method will only register them once until the server is reset
	 */
	private void registerButtonsIfHasnt() {
		if (!this.buttonsRegistered) {
			this.registerButtons();

			this.buttonsRegistered = true;
		}
	}

	/**
	 * Returns a list of buttons that should be registered manually.
	 *
	 * NOTICE: Button fields in your class are registered automatically, do not add
	 * them here
	 *
	 * @return button list, null by default
	 *
	 * @deprecated use {@link AdvancedMenu#addButton} to add an automatically registered button.
	 */
	@Deprecated
	protected List<Button> getButtonsToAutoRegister() {
		return null;
	}

	/**
	 * Attempts to find a clickable registered button in this menu having the same
	 * icon as the given item stack
	 *
	 * @param fromItem the itemstack to compare to
	 * @return the button or null if not found
	 */
	@Deprecated
	protected final Button getButton(final ItemStack fromItem) {
		this.registerButtonsIfHasnt();

		if (fromItem != null)
			// TODO rewrite to use cache instead of for loop so that two buttons that are the same won't collide
			for (final Button button : this.registeredButtons.keySet()) {
				Valid.checkNotNull(button, "Menu button is null at " + this.getClass().getSimpleName());
				Valid.checkNotNull(button.getItem(), "Menu " + this.getTitle() + " contained button " + button + " with empty item!");

				if (ItemUtil.isSimilar(fromItem, button.getItem()))
					return button;
			}

		return null;
	}

	/**
	 * Return a new instance of this menu
	 *
	 * <p>
	 * You must override this in certain cases
	 *
	 * @return the new instance, of null
	 * @throws FoException if new instance could not be made, for example when the menu is
	 *            taking constructor params
	 */
	public Menu newInstance() {
		try {
			return ReflectionUtil.instantiate(this.getClass());
		} catch (final Throwable t) {
			try {
				final Object parent = this.getClass().getMethod("getParent").invoke(this.getClass());

				if (parent != null)
					return ReflectionUtil.instantiate(getClass(), parent);
			} catch (final Throwable ignored) {
			}

			t.printStackTrace();
		}

		throw new FoException("Could not instantiate menu of " + this.getClass() + ", override the method 'newInstance()' or ensure you have a public constructor which takes only one parameter ");
	}

	// --------------------------------------------------------------------------------
	// Rendering the menu
	// --------------------------------------------------------------------------------

	/**
	 * Display this menu to the player
	 *
	 * @param player the player
	 *
	 * @deprecated use {@link AdvancedMenu#display()} to display the menu.
	 */
	@Deprecated
	public final void displayTo(final Player player) {
		Valid.checkNotNull(this.size, "Size not set in " + this + " (call setSize in your constructor)");
		Valid.checkNotNull(this.title, "Title not set in " + this + " (call setTitle in your constructor)");

		if (MinecraftVersion.olderThan(V.v1_5)) {
			final String error = "Displaying menus require Minecraft 1.5.2 or greater.";

			if (Messenger.ENABLED)
				Messenger.error(player, error);
			else
				Common.tell(player, error);

			return;
		}

		this.viewer = player;
		this.registerButtonsIfHasnt();

		// Draw the menu
		final InventoryDrawer drawer = InventoryDrawer.of(size, title);

		// Set items defined by classes upstream
		for (int i = 0; i < drawer.getSize(); i++) {
			final ItemStack item = getItemAt(i);

			if (item != null && !drawer.isSet(i))
				drawer.setItem(i, item);
		}

		// Allow last minute modifications
		this.onDisplay(drawer);

		// Render empty slots as slot numbers if enabled
		this.debugSlotNumbers(drawer);

		// Call event after items have been set to allow to get them
		if (!Common.callEvent(new MenuOpenEvent(this, drawer, player)))
			return;

		// Prevent menu in conversation
		if (player.isConversing()) {
			player.sendRawMessage(Common.colorize(SimpleLocalization.Menu.CANNOT_OPEN_DURING_CONVERSATION));

			return;
		}

		// Play the pop sound
		sound.play(player);

		// Register previous menu if exists
		{
			final Menu previous = getMenu(player);

			if (previous != null)
				player.setMetadata(FoConstants.NBT.TAG_MENU_PREVIOUS, new FixedMetadataValue(SimplePlugin.getInstance(), previous));
		}

		// Register current menu
		Common.runLater(1, () -> {
			try {
				drawer.display(player);

			} catch (final Throwable t) {
				Common.error(t, "Error opening menu " + Menu.this);

				return;
			}

			player.setMetadata(FoConstants.NBT.TAG_MENU_CURRENT, new FixedMetadataValue(SimplePlugin.getInstance(), Menu.this));

			this.opened = true;
		});
	}

	/**
	 * Sets all empty slots to light gray pane or adds a slot number to existing
	 * items lores if {@link #slotNumbersVisible} is true
	 */
	private void debugSlotNumbers(final InventoryDrawer drawer) {
		if (this.slotNumbersVisible)
			for (int slot = 0; slot < drawer.getSize(); slot++) {
				final ItemStack item = drawer.getItem(slot);

				if (item == null)
					drawer.setItem(slot, ItemCreator.of(CompMaterial.LIGHT_GRAY_STAINED_GLASS_PANE, "Slot " + slot).make());
			}
	}

	/**
	 * Called automatically before the menu is displayed but after all items have
	 * been drawed
	 *
	 * <p>
	 * Override for custom last-minute modifications
	 *
	 * @param drawer the drawer
	 */
	protected void onDisplay(final InventoryDrawer drawer) {
	}

	/**
	 * Redraws and refreshes all buttons
	 */
	public final void restartMenu() {
		this.restartMenu(null);
	}

	/**
	 * Redraws and re-register all buttons while sending a title animation to the
	 * player
	 *
	 * @param animatedTitle the animated title
	 */
	public final void restartMenu(final String animatedTitle) {

		final Inventory inventory = this.getViewer().getOpenInventory().getTopInventory();
		Valid.checkBoolean(inventory.getType() == InventoryType.CHEST, this.getViewer().getName() + "'s inventory closed in the meanwhile (now == " + inventory.getType() + ").");

		this.registerButtons();

		// Call before calling getItemAt
		this.onRestart();

		this.getViewer().updateInventory();

		if (animatedTitle != null)
			this.animateTitle(animatedTitle);
	}

	void onRestart() {}

	/**
	 * Redraws the bottom bar and updates inventory
	 */
	protected final void redraw() {
		final Inventory inv = getViewer().getOpenInventory().getTopInventory();
		Valid.checkBoolean(inv.getType() == InventoryType.CHEST, getViewer().getName() + "'s inventory closed in the meanwhile (now == " + inv.getType() + ").");

		for (int i = 0; i < size; i++) {
			final ItemStack item = getItemAt(i);

			Valid.checkBoolean(i < inv.getSize(), "Item (" + (item != null ? item.getType() : "null") + ") position (" + i + ") > inv size (" + inv.getSize() + ")");
			inv.setItem(i, item);
		}

		getViewer().updateInventory();
	}

	// --------------------------------------------------------------------------------
	// Convenience messenger functions
	// --------------------------------------------------------------------------------

	/**
	 * Send a message to the {@link #getViewer()}
	 */
	public void tell(String... messages) {
		Common.tell(this.viewer, messages);
	}

	/**
	 * Send a message to the {@link #getViewer()}
	 */
	public void tellInfo(String message) {
		Messenger.info(this.viewer, message);
	}

	/**
	 * Send a message to the {@link #getViewer()}
	 */
	public void tellSuccess(String message) {
		Messenger.success(this.viewer, message);
	}

	/**
	 * Send a message to the {@link #getViewer()}
	 */
	public void tellWarn(String message) {
		Messenger.warn(this.viewer, message);
	}

	/**
	 * Send a message to the {@link #getViewer()}
	 */
	public void tellError(String message) {
		Messenger.error(this.viewer, message);
	}

	/**
	 * Send a message to the {@link #getViewer()}
	 */
	public void tellQuestion(String message) {
		Messenger.question(this.viewer, message);
	}

	/**
	 * Send a message to the {@link #getViewer()}
	 */
	public void tellAnnounce(String message) {
		Messenger.announce(this.viewer, message);
	}

	// --------------------------------------------------------------------------------
	// Animations
	// --------------------------------------------------------------------------------

	/**
	 * Animate the title of this menu
	 *
	 * <p>
	 * Automatically reverts back to the old title after 1 second
	 *
	 * @param title the title to animate
	 */
	public final void animateTitle(final String title) {
		if (titleAnimationEnabled)
			PlayerUtil.updateInventoryTitle(this, this.getViewer(), title, this.getTitle(), titleAnimationDurationTicks);
	}

	/**
	 * Start a repetitive task with the given period in ticks on the main thread,
	 * that is automatically stopped if the viewer no longer sees this menu.
	 *
	 * Can impose a performance penalty. Use cancel() to cancel.
	 *
	 * @param periodTicks
	 * @param task
	 */
	protected final void animate(int periodTicks, MenuRunnable task) {
		Common.runTimer(2, periodTicks, this.wrapAnimation(task));
	}

	/**
	 * Start a repetitive task with the given period in ticks ASYNC,
	 * that is automatically stopped if the viewer no longer sees this menu.
	 *
	 * Use cancel() to cancel.
	 *
	 * @param periodTicks
	 * @param task
	 */
	protected final void animateAsync(int periodTicks, MenuRunnable task) {
		Common.runTimerAsync(2, periodTicks, this.wrapAnimation(task));
	}

	/*
	 * Helper method to create a bukkit runnable
	 */
	private BukkitRunnable wrapAnimation(MenuRunnable task) {
		return new BukkitRunnable() {

			@Override
			public void run() {

				if (Menu.this.closed) {
					this.cancel();

					return;
				}

				try {
					task.run();

				} catch (final EventHandledException ex) {
					this.cancel();
				}
			}
		};
	}

	/**
	 * A special wrapper for animating menus
	 */
	@FunctionalInterface
	public interface MenuRunnable extends Runnable {

		/**
		 * Cancel the menu animation
		 */
		default void cancel() {
			throw new EventHandledException();
		}
	}

	// --------------------------------------------------------------------------------
	// Menu functions
	// --------------------------------------------------------------------------------

	/**
	 * Returns the item at a certain slot
	 *
	 * @param slot the slow
	 * @return the item, or null if no icon at the given slot (default)
	 */
	public ItemStack getItemAt(final int slot) {
		return NO_ITEM;
	}

	// --------------------------------------------------------------------------------
	// Menu functions
	// --------------------------------------------------------------------------------

	/**
	 * Calculates the center slot of this menu
	 *
	 * <p>
	 * Credits to Gober at
	 * <a href="https://www.spigotmc.org/threads/get-the-center-slot-of-a-menu.379586/">https://www.spigotmc.org/threads/get-the-center-slot-of-a-menu.379586/</a>
	 *
	 * @return the estimated center slot
	 */
	protected final int getCenterSlot() {
		final int pos = this.size / 2;

		return this.size % 2 == 1 ? pos : pos - 5;
	}

	/**
	 * Return the middle slot in the last menu row (in the hotbar)
	 *
	 * @return
	 */
	protected final int getBottomCenterSlot() {
		return this.size - 5;
	}

	/**
	 * Should we prevent the click or drag?
	 *
	 * @param location the click location
	 * @param slot     the slot
	 * @param clicked  the clicked item
	 * @param cursor   the cursor
	 * @param action   the inventory action
	 *
	 * @return if the action is cancelled in the {@link InventoryClickEvent}, false
	 * by default
	 */
	protected boolean isActionAllowed(final MenuClickLocation location, final int slot, @Nullable final ItemStack clicked, @Nullable final ItemStack cursor, InventoryAction action) {
		return false;
	}

	/**
	 * The title of this menu
	 *
	 * @return the menu title
	 */
	public final String getTitle() {
		return this.title;
	}

	/**
	 * Sets the title of this inventory, this change is reflected
	 * when this menu is already displayed to a given player.
	 *
	 * @param title the new title
	 */
	protected final void setTitle(final String title) {
		this.title = title;

		if (this.viewer != null && this.opened)
			PlayerUtil.updateInventoryTitle(this.viewer, title);
	}

	/**
	 * Return the parent menu or null
	 *
	 * @deprecated use {@link AdvancedMenu#getParentMenu()} instead.
	 */
	@Deprecated
	public final Menu getParent() {
		return this.parent;
	}

	/**
	 * Get the size of this menu
	 */
	public final Integer getSize() {
		return this.size;
	}

	/**
	 * Sets the size of this menu (without updating the player container - if you
	 * want to update it call {@link #restartMenu()})
	 */
	protected final void setSize(final Integer size) {
		this.size = size;
	}

	/**
	 * Set the menu's description
	 *
	 * <p>
	 * Used to create an info bottom in bottom left corner, see
	 * {@link Button#makeInfo(String...)}
	 *
	 * return info the info to set
	 */
	protected String[] getInfo() {
		return null;
	}

	/**
	 * Get the viewer that this instance of this menu is associated with
	 *
	 * @return the viewer of this instance, or null
	 */
	protected final Player getViewer() {
		return this.viewer;
	}

	/**
	 * Sets the viewer for this instance of this menu
	 */
	protected final void setViewer(@NonNull final Player viewer) {
		this.viewer = viewer;
	}

	/**
	 * Return the top opened inventory if viewer exists
	 */
	protected final Inventory getInventory() {
		Valid.checkNotNull(this.viewer, "Cannot get inventory when there is no viewer!");

		final Inventory topInventory = this.viewer.getOpenInventory().getTopInventory();
		Valid.checkNotNull(topInventory, "Top inventory is null!");

		return topInventory;
	}

	/**
	 * Get the open inventory content to match the array length, cloning items
	 * preventing ID mismatch in yaml files
	 */
	protected final ItemStack[] getContent(final int from, final int to) {
		final ItemStack[] content = this.getInventory().getContents();
		final ItemStack[] copy = new ItemStack[content.length];

		for (int i = from; i < copy.length; i++) {
			final ItemStack item = content[i];

			copy[i] = item != null ? item.clone() : null;
		}

		return Arrays.copyOfRange(copy, from, to);
	}

	/**
	 * Updates a slot in this menu
	 */
	protected final void setItem(int slot, ItemStack item) {
		final Inventory inventory = this.getInventory();

		inventory.setItem(slot, item);
	}

	/**
	 * If you wonder what slot numbers does each empty slot in your menu has then
	 * set this to true in your constructor
	 *
	 * <p>
	 * Only takes change when used in constructor or before calling
	 * {@link #displayTo(Player)} and cannot be updated in {@link #restartMenu()}
	 */
	protected final void setSlotNumbersVisible() {
		this.slotNumbersVisible = true;
	}

	/**
	 * Return if the given player is still viewing this menu, we compare
	 * the menu class of the menu the player is viewing and return true if both equal.
	 */
	public final boolean isViewing(Player player) {
		final Menu menu = Menu.getMenu(player);

		return menu != null && menu.getClass().getName().equals(this.getClass().getName());
	}

	// --------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------

	/**
	 * Called automatically when the menu is clicked.
	 *
	 * <p>
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
	protected void onMenuClick(final Player player, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack clicked, final boolean cancelled) {
		this.onMenuClick(player, slot, clicked);
	}

	/**
	 * Called automatically when the menu is clicked
	 *
	 * @param player  the player
	 * @param slot    the slot
	 * @param clicked the item clicked
	 */
	protected void onMenuClick(final Player player, final int slot, final ItemStack clicked) {
	}

	/**
	 * Called automatically when a registered button is clicked
	 *
	 * <p>
	 * By default this method parses the click into
	 * {@link Button#onClickedInMenu(Player, AdvancedMenu, ClickType)}
	 *
	 * @param player the player
	 * @param slot   the slot
	 * @param action the action
	 * @param click  the click
	 * @param button the button
	 */
	protected void onButtonClick(final Player player, final int slot, final InventoryAction action, final ClickType click, final Button button) {
		button.onClickedInMenu(player, (AdvancedMenu) this, click);
	}

	/**
	 * Handles the menu close, this does not close the inventory, only cleans up internally,
	 * do not use.
	 *
	 * @deprecated internal use only
	 * @param inventory
	 */
	@Deprecated
	public final void handleClose(Inventory inventory) {
		this.viewer.removeMetadata(FoConstants.NBT.TAG_MENU_CURRENT, SimplePlugin.getInstance());
		this.viewer.setMetadata(FoConstants.NBT.TAG_MENU_LAST_CLOSED, new FixedMetadataValue(SimplePlugin.getInstance(), this));
		this.closed = true;

		this.onMenuClose(this.viewer, inventory);

		// End by calling API
		Common.callEvent(new MenuCloseEvent(this, inventory, this.viewer));
	}

	/**
	 * Called automatically when the menu is closed
	 *
	 * @param player    the player
	 * @param inventory the menu inventory that is being closed
	 */
	protected void onMenuClose(final Player player, final Inventory inventory) {
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{}";
	}
}
