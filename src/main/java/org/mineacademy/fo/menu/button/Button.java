package org.mineacademy.fo.menu.button;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Represents a clickable button in menu
 */
public abstract class Button {

	/**
	 * The material representing info button, see {@link #makeInfo(String...)}
	 */
	@Setter
	private static CompMaterial infoButtonMaterial = CompMaterial.NETHER_STAR;

	/**
	 * The title of the info button, see {@link #makeInfo(String...)}
	 *
	 * Colorized automatically.
	 */
	@Setter
	private static String infoButtonTitle = "&fMenu Information";

	// ----------------------------------------------------------------
	// Button functions
	// ----------------------------------------------------------------

	/**
	 * Called automatically from the button is clicked
	 *
	 * @param player
	 * @param menu
	 * @param click
	 */
	public abstract void onClickedInMenu(Player player, Menu menu, ClickType click);

	/**
	 * The item representing this button. Tip: Use {@link ItemCreator} to create it.
	 *
	 * @return the item for this button
	 */
	public abstract ItemStack getItem();

	// ----------------------------------------------------------------
	// Static methods
	// ----------------------------------------------------------------

	/**
	 * Creates a new Nether Star button has no action on clicking
	 * and it is used purely to display informative text.
	 *
	 * Each description line starts with gray color by default and has colors replaced.
	 *
	 * Use {@link #setInfoButtonMaterial(CompMaterial)} and {@link #setInfoButtonTitle(String)} to customize it.
	 *
	 * @param description the description of the button
	 * @return the button
	 */
	public static final DummyButton makeInfo(String... description) {
		final List<String> lores = new ArrayList<>();
		lores.add(" ");

		for (final String line : description)
			lores.add("&7" + line);

		return makeDummy(ItemCreator.of(infoButtonMaterial).name(infoButtonTitle).hideTags(true).lores(lores));
	}

	/**
	 * Create a new empty button (air)
	 *
	 * @return a new dummy air button
	 */
	public static final DummyButton makeEmpty() {
		return makeDummy(ItemCreator.of(CompMaterial.AIR));
	}

	/**
	 * Creates a dummy button that does nothing when clicked
	 *
	 * @param builder the icon builder
	 * @return the button
	 */
	public static final DummyButton makeDummy(ItemCreator.ItemCreatorBuilder builder) {
		return makeDummy(builder.build());
	}

	/**
	 * Creates a dummy button that does nothing when clicked
	 *
	 * @param creator the icon creator
	 * @return the buttpn
	 */
	public static final DummyButton makeDummy(ItemCreator creator) {
		return new DummyButton(creator.makeMenuTool());
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "{" + getItem().getType() + "}";
	}

	@FunctionalInterface
	public interface MenuLateBind {
		Menu getMenu();
	}

	// ----------------------------------------------------------------
	// Helper classes methods
	// ----------------------------------------------------------------

	/**
	 * The button that doesn't do anything when clicked.
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class DummyButton extends Button {

		/**
		 * The icon for this button
		 */
		@Getter
		private final ItemStack item;

		/**
		 * Do nothing when clicked
		 */
		@Override
		public void onClickedInMenu(Player player, Menu menu, ClickType click) {
		}
	}
}
