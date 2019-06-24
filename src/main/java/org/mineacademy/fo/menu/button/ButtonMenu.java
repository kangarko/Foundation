package org.mineacademy.fo.menu.button;

import java.util.Objects;
import java.util.concurrent.Callable;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ReflectionUtil;
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
	 *
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
	 * Create a new button that triggers another menu
	 *
	 * @param menuClass
	 * @param material
	 * @param name
	 * @param lore
	 */
	public ButtonMenu(Class<? extends Menu> menuClass, CompMaterial material, String name, String... lore) {
		this(null, () -> ReflectionUtil.instatiate(menuClass), ItemCreator.of(material, name, lore).hideTags(true).build().make());
	}

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menuLateBind
	 * @param item
	 */
	public ButtonMenu(Callable menuLateBind, ItemCreator.ItemCreatorBuilder item) {
		this(null, menuLateBind, item.hideTags(true).build().make());
	}

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menuLateBind
	 * @param item
	 */
	public ButtonMenu(Callable menuLateBind, ItemStack item) {
		this(null, menuLateBind, item);
	}

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menu
	 * @param material
	 * @param name
	 * @param lore
	 */
	public ButtonMenu(Menu menu, CompMaterial material, String name, String... lore) {
		this(menu, ItemCreator.of(material, name, lore));
	}

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menu
	 * @param item
	 */
	public ButtonMenu(Menu menu, ItemCreator.ItemCreatorBuilder item) {
		this(menu, null, item.hideTags(true).build().make());
	}

	/**
	 * Create a new button that triggers another menu
	 *
	 * @param menu
	 * @param item
	 */
	public ButtonMenu(Menu menu, ItemStack item) {
		this(menu, null, item);
	}

	// Private constructor
	private ButtonMenu(Menu menuToOpen, Callable<Menu> menuLateBind, ItemStack item) {
		this.menuToOpen = menuToOpen;
		this.menuLateBind = menuLateBind;
		this.item = item;
	}

	/**
	 * Automatically display another menu when the button is clicked
	 */
	@Override
	public void onClickedInMenu(Player pl, Menu menu, ClickType click) {
		if (menuLateBind != null)
			try {
				menuLateBind.call().displayTo(pl);
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
		else {
			Objects.requireNonNull(menuToOpen, "Report / ButtonTrigger requires either 'late bind menu' or normal menu to be set!");

			menuToOpen.displayTo(pl);
		}
	}
}