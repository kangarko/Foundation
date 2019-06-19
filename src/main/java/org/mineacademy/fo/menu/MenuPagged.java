package org.mineacademy.fo.menu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.PageManager;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompSound;

import lombok.Getter;
import lombok.val;

/**
 * An advanced menu listing items with automatic page support
 *
 * @param <T> the item that each page consists of
 */
public abstract class MenuPagged<T> extends MenuStandard {

	/**
	 * The pages by the page number, containing a list of items
	 */
	@Getter
	private final Map<Integer, List<T>> pages;

	/**
	 * The current page
	 */
	@Getter
	private int currentPage = 1;

	/**
	 * The next button automatically generated
	 */
	private Button nextButton;

	/**
	 * The "go to previous page" button automatically generated
	 */
	private Button prevButton;

	/**
	 * Create a new paged menu
	 *
	 * @param cellSize size of the menu, a multiple of 9 (keep in mind we already add 1 row there)
	 * @param parent the parent menu
	 * @param pages the pages
	 */
	protected MenuPagged(int cellSize, Menu parent, Iterable<T> pages) {
		this(cellSize, parent, false, pages);
	}

	/**
	 * Create a new paged menu
	 *
	 * @param cellSize size of the menu, a multiple of 9 (keep in mind we already add 1 row there)
	 * @param parent the parent menu
	 * @param returnMakesNewInstance should we create new instance of parent when returning back
	 * @param pages the pages the pages
	 */
	protected MenuPagged(int cellSize, Menu parent, boolean returnMakesNewInstance, Iterable<T> pages) {
		super(parent, returnMakesNewInstance);

		this.currentPage = 1;
		this.pages = PageManager.populate(cellSize, pages);

		setSize(9 + cellSize);
		setTitleAndButtons();
	}

	// Render the title and buttons
	private final void setTitleAndButtons() {
		final boolean hasPages = pages.size() > 1;

		{ // Set title
			final String title = getTitlePrefix() + (hasPages ? " &8" + currentPage + "/" + pages.size() : "");

			if (getViewer() != null)
				ReflectionUtil.updateInventoryTitle(getViewer(), title);
			else
				setTitle(title);
		}

		{ // Set buttons
			this.prevButton = hasPages ? new Button() {
				final boolean canGo = currentPage > 1;

				@Override
				public void onClickedInMenu(Player pl, Menu menu, ClickType click) {
					if (canGo) {
						MenuPagged.this.currentPage = MathUtil.range(currentPage - 1, 1, pages.size());

						updatePage();
					}
				}

				@Override
				public ItemStack getItem() {
					final int str = currentPage - 1;

					return ItemCreator.of(CompMaterial.fromLegacy("INK_SACK", canGo ? 10 : 8)).name(str == 0 ? "&7First Page" : "&8<< &fPage " + str).build().make();
				}
			} : Button.makeEmpty();

			this.nextButton = hasPages ? new Button() {
				final boolean canGo = currentPage < pages.size();

				@Override
				public void onClickedInMenu(Player pl, Menu menu, ClickType click) {
					if (canGo) {
						MenuPagged.this.currentPage = MathUtil.range(currentPage + 1, 1, pages.size());

						updatePage();
					}
				}

				@Override
				public ItemStack getItem() {
					final boolean last = currentPage == pages.size();

					return ItemCreator.of(CompMaterial.fromLegacy("INK_SACK", canGo ? 10 : 8)).name(last ? "&7Last Page" : "Page " + (currentPage + 1) + " &8>>").build().make();
				}
			} : Button.makeEmpty();
		}
	}

	// Reinits the menu and plays the anvil sound
	private final void updatePage() {
		setTitleAndButtons();
		redraw();
		registerButtons();

		CompSound.ANVIL_LAND.play(getViewer(), 1, 1);
	}

	/**
	 * Since we override the default menu title due to page number,
	 * you can return the prefix title here.
	 *
	 * Example:
	 * "Available classes" and we will add the pages so it becomes:
	 * "Available classes 1/4"
	 *
	 * @return the title prefix
	 */
	protected abstract String getTitlePrefix();

	/**
	 * Return the {@link ItemStack} representation of an item on a certain page
	 *
	 * Use {@link ItemCreator} for easy creation.
	 *
	 * @param item the given object, for example Arena
	 * @return the itemstack, for example diamond sword having arena name
	 */
	protected abstract ItemStack convertToItemStack(T item);

	/**
	 * Called automatically when an item is clicked
	 *
	 * @param player the player who clicked
	 * @param item the clicked item
	 * @param click the click type
	 */
	protected abstract void onPageClick(Player player, T item, ClickType click);

	/**
	 * Utility: Shall we send update packet when the menu is clicked?
	 *
	 * @return true by default
	 */
	protected boolean updateButtonOnClick() {
		return true;
	}

	/**
	 * Automatically get the correct item from the actual page, including prev/next buttons
	 *
	 * @param slot the slot
	 * @return the item, or null
	 */
	@Override
	public ItemStack getItemAt(int slot) {
		if (slot < getPageItems().size()) {
			final T object = getPageItems().get(slot);

			if (object != null)
				return convertToItemStack(object);
		}

		if (slot == getSize() - 6)
			return prevButton.getItem();

		if (slot == getSize() - 4)
			return nextButton.getItem();

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onMenuClick(Player pl, int slot, InventoryAction action, ClickType click, ItemStack cursor, ItemStack clicked, boolean cancelled) {
		if (slot < getPageItems().size()) {
			final T obj = getPageItems().get(slot);

			if (obj != null) {
				val prevType = pl.getOpenInventory().getType();
				onPageClick(pl, obj, click);

				if (updateButtonOnClick() && prevType == pl.getOpenInventory().getType())
					pl.getOpenInventory().getTopInventory().setItem(slot, getItemAt(slot));
			}
		}
	}

	// Do not allow override
	@Override
	public final void onButtonClick(Player pl, int slot, InventoryAction action, ClickType click, Button button) {
		super.onButtonClick(pl, slot, action, click, button);
	}

	// Do not allow override
	@Override
	public final void onMenuClick(Player pl, int slot, ItemStack clicked) {
		throw new FoException("Simplest click unsupported");
	}

	// Get all items in a page
	private final List<T> getPageItems() {
		Validate.isTrue(pages.containsKey(currentPage - 1), "The menu has only " + pages.size() + " pages, not " + currentPage + "!");

		return pages.get(currentPage - 1);
	}
}
