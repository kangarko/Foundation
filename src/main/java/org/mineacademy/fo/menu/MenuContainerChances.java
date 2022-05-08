package org.mineacademy.fo.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.menu.model.MenuQuantity;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * A menu that lets players put items into the container and save them.
 *
 * It also provides a way to set a "chance" for each item. For example you can
 * use this as drop chances for your drop tables for custom entities.
 *
 * You are recommended to follow a video guide on implementing this. We provide
 * a complete GUI training at mineacademy.org in our programs.
 */
public abstract class MenuContainerChances extends Menu implements MenuQuantitable {

	/**
	 * Temporary store of the edited drop chances here
	 */
	private final StrictMap<Integer, Double> editedDropChances = new StrictMap<>();

	/**
	 * The button to switch between menu modes.
	 */
	private final Button changeModeButton;

	/**
	 * The mode in which this menu is operating right now.
	 */
	@Getter
	@Setter
	private MenuQuantity quantity = MenuQuantity.ONE;

	/*
	 * The current menu mode stored here.
	 */
	@Getter(AccessLevel.PROTECTED)
	private EditMode mode = EditMode.ITEM;

	/**
	 * Create a new menu that can edit chances of the items you put inside.
	 *
	 * @param parent
	 */
	protected MenuContainerChances(Menu parent) {
		this(parent, false);
	}

	/**
	 * Create a new menu that can edit chances of the items you put inside.
	 *
	 * @param parent
	 * @param startMode
	 * @param returnMakesNewInstance
	 */
	protected MenuContainerChances(Menu parent, boolean returnMakesNewInstance) {
		super(parent, returnMakesNewInstance);

		// Default the size to 3 rows (+ 1 bottom row is added automatically)
		this.setSize(9 * 3);

		this.changeModeButton = new Button() {

			/**
			 * Change the menu mode and refresh its content.
			 */
			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				final MenuContainerChances instance = MenuContainerChances.this;

				// Call event to properly save data without us having to restart the menu completely
				instance.onMenuClose(player, player.getOpenInventory().getTopInventory());

				// Simulate mode chance in the menu
				instance.mode = MenuContainerChances.this.mode.next();
				instance.setTitle("&0Editing " + instance.mode.getKey());

				instance.restartMenu();
			}

			/**
			 * Compiles the edit mode button.
			 */
			@Override
			public ItemStack getItem() {
				final boolean chances = MenuContainerChances.this.mode == EditMode.CHANCE;

				return ItemCreator.of(
						chances ? CompMaterial.GOLD_NUGGET : CompMaterial.CHEST,
						"Editing " + MenuContainerChances.this.mode.getKey(),
						"",
						"&7Click to edit " + MenuContainerChances.this.mode.next().getKey().toLowerCase() + ".")
						.glow(chances)
						.make();
			}
		};
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuQuantitable#getQuantityButtonPosition()
	 */
	@Override
	public int getQuantityButtonPosition() {
		return this.mode == EditMode.ITEM ? -1 : MenuQuantitable.super.getQuantityButtonPosition(); // TODO was this.getSize() - 6
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting items
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @see org.mineacademy.fo.menu.MenuQuantitable#allowDecimalQuantities()
	 */
	@Override
	public boolean allowDecimalQuantities() {
		return false;
	}

	/*
	 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
	 */
	@Override
	public final ItemStack getItemAt(int slot) {
		if (slot == this.getChangeModeButtonPosition())
			return this.changeModeButton.getItem();

		final ItemStack customDrop = this.getDropAt(slot);

		if (customDrop != null) {

			if (this.mode == EditMode.ITEM || !this.canEditItem(slot))
				return customDrop;

			final double dropChance = this.mode == EditMode.ITEM ? this.getDropChance(slot) : this.editedDropChances.getOrDefault(slot, this.getDropChance(slot));
			final String level = MathUtil.formatTwoDigits(100 * dropChance) + "%";

			return this.addLevelToItem(customDrop, level);
		}

		if (slot > this.getSize() - 9)
			return MenuContainer.BOTTOM_BAR_FILLER_ITEM;

		return NO_ITEM;
	}

	/**
	 * Returns the {@link #changeModeButton} position, defaults to (getSize() - 4)
	 *
	 * @return
	 */
	protected int getChangeModeButtonPosition() {
		return this.getSize() - 2;
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuQuantitable#getLevelLoreLabel()
	 */
	@Override
	public String getLevelLoreLabel() {
		return "Drop chance";
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuQuantitable#quantitiesArePercents()
	 */
	@Override
	public final boolean quantitiesArePercents() {
		return true;
	}

	/**
	 * Return the item that should appear at the given slot,
	 * you should load items from your data file or cache here.
	 *
	 * @param slot
	 * @return
	 */
	protected abstract ItemStack getDropAt(int slot);

	/**
	 * Return the item's drop chance loaded from the disk or cache here.
	 *
	 * @param slot
	 * @return
	 */
	protected abstract double getDropChance(int slot);

	// ------------------------------------------------------------------------------------------------------------
	// Allowing clicking
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see org.mineacademy.fo.menu.Menu#isActionAllowed(org.mineacademy.fo.menu.model.MenuClickLocation, int, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack)
	 */
	@Override
	public final boolean isActionAllowed(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor, InventoryAction action) {
		if (this.mode == EditMode.CHANCE)
			return false;

		if (location != MenuClickLocation.MENU)
			return true;

		if (!this.canEditItem(location, slot, clicked, cursor, action))
			return false;

		return slot < this.getSize() - 9;
	}

	/**
	 * Return true for the slots you want players to be able to edit.
	 * By default we enable them to edit anything above the bottom bar.
	 *
	 * This is called from {@link #isActionAllowed(MenuClickLocation, int, ItemStack, ItemStack)} and
	 * by defaults forwards the call to {@link #canEditItem(int)}
	 *
	 * @param location
	 * @param slot
	 * @param clicked
	 * @param cursor
	 * @param action
	 *
	 * @return
	 */
	protected boolean canEditItem(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor, InventoryAction action) {
		return this.canEditItem(slot);
	}

	/**
	 * Return the slot numbers for which you want to allow
	 * items to get edited in your menu (if you do not want
	 * to allow editing the entire container window).
	 *
	 * If you want users to edit chances for all items except
	 * bottom bar, simply always return true here.
	 *
	 * @param slot
	 * @return
	 */
	protected boolean canEditItem(int slot) {
		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Handling clicking
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see org.mineacademy.fo.menu.Menu#onMenuClick(org.bukkit.entity.Player, int, org.bukkit.event.inventory.InventoryAction, org.bukkit.event.inventory.ClickType, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack, boolean)
	 */
	@Override
	protected final void onMenuClick(Player player, int slot, InventoryAction action, ClickType click, ItemStack cursor, ItemStack clicked, boolean cancelled) {

		if (this.mode == EditMode.CHANCE && this.canEditItem(slot) && slot < this.getSize() - 9) {

			// Prevent exploiting chances menu holding an item
			if (clicked == null)
				return;

			final double chance = this.editedDropChances.getOrDefault(slot, this.getDropChance(slot));
			final double next = this.getNextQuantityDouble(click);
			final double newChance = MathUtil.range(chance + next, 0.D, 1.D);

			// Save drop chance
			this.editedDropChances.override(slot, newChance);

			// Update item
			this.setItem(slot, this.getItemAt(slot));
		}
	}

	/*
	 * @see org.mineacademy.fo.menu.Menu#onMenuClick(org.bukkit.entity.Player, int, org.bukkit.inventory.ItemStack)
	 */
	@Override
	protected final void onMenuClick(Player player, int slot, ItemStack clicked) {
		throw new FoException("unsupported call");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Handling saving
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see org.mineacademy.fo.menu.Menu#onMenuClose(org.bukkit.entity.Player, org.bukkit.inventory.Inventory)
	 */
	@Override
	protected final void onMenuClose(Player player, Inventory inventory) {
		final StrictMap<Integer, Tuple<ItemStack, Double>> items = new StrictMap<>();

		for (int slot = 0; slot < this.getSize() - 9; slot++) {
			boolean placed = false;

			if (this.canEditItem(slot)) {
				final ItemStack item = this.mode == EditMode.ITEM ? inventory.getItem(slot) : this.getDropAt(slot);
				final Double dropChance = this.editedDropChances.getOrDefault(slot, this.getDropChance(slot));

				if (item != null && !CompMaterial.isAir(item)) {
					Valid.checkNotNull(dropChance, "Drop chances cannot be null on slot " + slot + " for " + item);

					items.put(slot, new Tuple<>(item, dropChance));
					placed = true;
				}
			}

			if (!placed)
				items.put(slot, null);
		}

		this.onMenuClose(items);
	}

	/**
	 * Called automatically when you should save all editable slots stored in the map
	 * by slot, with their items (nullable) and new drop chances)
	 *
	 * @param items
	 */
	protected abstract void onMenuClose(StrictMap<Integer, Tuple<ItemStack, Double>> items);

	// ------------------------------------------------------------------------------------------------------------
	// Decoration
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		if (this.mode == EditMode.ITEM)
			return new String[] {
					"This menu allows you to drop",
					"items to this container.",
					"",
					"Simply &2drag and drop &7items",
					"from your inventory here."
			};

		else
			return new String[] {
					"This menu allows you to edit drop",
					"chances for items in this container.",
					"",
					"&2Right or left click &7on items",
					"to adjust their drop chance."
			};
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * The menu edit mode
	 */
	@RequiredArgsConstructor
	public enum EditMode {

		/**
		 * We want to allow player to place items in the menu container,
		 * such as items a Boss should drop on death.
		 */
		ITEM("Items"),

		/**
		 * We want to allow player to edit the drop chance for each
		 * item he placed in the container, such as how likely it is
		 * for a Boss to drop each item on death.
		 */
		CHANCE("Drop Chances");

		/**
		 * The localized key.
		 */
		@Getter
		private final String key;

		/**
		 * Get the next mode to refresh the menu.
		 *
		 * @return
		 */
		private EditMode next() {
			return Common.getNext(this, EditMode.values(), true);
		}
	}
}
