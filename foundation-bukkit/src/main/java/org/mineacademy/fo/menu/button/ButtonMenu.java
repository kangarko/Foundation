package org.mineacademy.fo.menu.button;

import java.util.concurrent.Callable;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * A button that opens another menu
 */
public final class ButtonMenu extends Button {

	/**
	 * Sometimes you need to allocate data when you create the button,
	 * but these data are not yet available when you make new instance of this button
	 * <p>
	 * Use this helper to set them right before showing the button
	 */
	private final Callable<Menu> menuLateBind;

	/**
	 * The menu this button opens
	 */
	private final Menu menuToOpen;

	/**
	 * The icon of this button
	 */
	@Getter
	private final ItemStack item;

	/**
	 * Create a new instanceof using {@link Menu#newInstance()} when showing the menu?
	 */
	private final boolean newInstance;

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menuClass
	 * @param material
	 * @param name
	 * @param lore
	 */
	public ButtonMenu(final Class<? extends Menu> menuClass, final CompMaterial material, final String name, final String... lore) {
		this(null, () -> ReflectionUtil.instantiate(menuClass), ItemCreator.of(material, name, lore).hideTags(true).make(), false);
	}

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menuLateBind
	 * @param item
	 */
	public ButtonMenu(final Callable<Menu> menuLateBind, final ItemCreator item) {
		this(null, menuLateBind, item.hideTags(true).make(), false);
	}

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menuLateBind
	 * @param item
	 */
	public ButtonMenu(final Callable<Menu> menuLateBind, final ItemStack item) {
		this(null, menuLateBind, item, false);
	}

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menu
	 * @param material
	 * @param name
	 * @param lore
	 */
	public ButtonMenu(final Menu menu, final CompMaterial material, final String name, final String... lore) {
		this(menu, ItemCreator.of(material, name, lore));
	}

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menu
	 * @param item
	 */
	public ButtonMenu(final Menu menu, final ItemCreator item) {
		this(menu, null, item.hideTags(true).make(), false);
	}

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menu
	 * @param item
	 */
	public ButtonMenu(final Menu menu, final ItemStack item) {
		this(menu, null, item, false);
	}

	public ButtonMenu(final Menu menu, final ItemStack item, final boolean newInstance) {
		this(menu, null, item, newInstance);
	}

	// Private constructor
	private ButtonMenu(final Menu menuToOpen, final Callable<Menu> menuLateBind, final ItemStack item, final boolean newInstance) {
		this.menuToOpen = menuToOpen;
		this.menuLateBind = menuLateBind;
		this.item = item;
		this.newInstance = newInstance;
	}

	/**
	 * Automatically display another menu when the button is clicked
	 */
	@Override
	public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
		if (this.menuLateBind != null) {
			Menu menuToOpen = null;

			try {
				menuToOpen = this.menuLateBind.call();
			} catch (final Exception ex) {
				ex.printStackTrace();

				return;
			}

			if (this.newInstance)
				menuToOpen = menuToOpen.newInstance();

			menuToOpen.displayTo(pl);

		} else {
			Valid.checkNotNull(this.menuToOpen, "Report / ButtonTrigger requires either 'late bind menu' or normal menu to be set!");

			if (this.newInstance)
				this.menuToOpen.newInstance().displayTo(pl);
			else
				this.menuToOpen.displayTo(pl);
		}
	}
}