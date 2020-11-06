package org.mineacademy.fo.menu.button;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleLocalization;

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
	 * <p>
	 * Colorized automatically.
	 */
	@Setter
	private static String infoButtonTitle = SimpleLocalization.Menu.TOOLTIP_INFO;

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
	 * <p>
	 * Each description line starts with gray color by default and has colors replaced.
	 * <p>
	 * Use {@link #setInfoButtonMaterial(CompMaterial)} and {@link #setInfoButtonTitle(String)} to customize it.
	 *
	 * @param description the description of the button
	 * @return the button
	 */
	public static final DummyButton makeInfo(final String... description) {
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
	public static final DummyButton makeDummy(final ItemCreator.ItemCreatorBuilder builder) {
		return makeDummy(builder.build());
	}

	/**
	 * Creates a dummy button that does nothing when clicked
	 *
	 * @param creator the icon creator
	 * @return the buttpn
	 */
	public static final DummyButton makeDummy(final ItemCreator creator) {
		return new DummyButton(creator.makeMenuTool());
	}

	/**
	 * Creates a lazy button having the given icon, title, label (the second lore row) and the click function
	 * taking in the player who damn clicked
	 *
	 * @param icon
	 * @param title
	 * @param label
	 * @param onClickFunction
	 * @return
	 */
	public static final Button makeSimple(final CompMaterial icon, final String title, final String label, final Consumer<Player> onClickFunction) {
		return new Button() {

			@Override
			public ItemStack getItem() {
				return ItemCreator.of(icon, title, "", label).build().makeMenuTool();
			}

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				onClickFunction.accept(player);
			}
		};
	}

	/**
	 * Creates a lazy button with the given builder and action when clicked
	 *
	 * @param builder
	 * @param onClickFunction
	 * @return
	 */
	public static final Button makeSimple(ItemCreator.ItemCreatorBuilder builder, final Consumer<Player> onClickFunction) {
		return new Button() {

			@Override
			public ItemStack getItem() {
				return builder.build().makeMenuTool();
			}

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				onClickFunction.accept(player);
			}
		};
	}

	/**
	 * Creates a lazy button having the given icon, title, label (the second lore row) and the click function
	 * taking in the player and the click type
	 *
	 * @param icon
	 * @param title
	 * @param label
	 * @param onClickFunction
	 * @return
	 */
	public static final Button makeSimple(final CompMaterial icon, final String title, final String label, final BiConsumer<Player, ClickType> onClickFunction) {
		return new Button() {

			@Override
			public ItemStack getItem() {
				return ItemCreator.of(icon, title, "", label).build().makeMenuTool();
			}

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				onClickFunction.accept(player, click);
			}
		};
	}

	/**
	 * Create a button that shows the decimal prompt to the player when clicked
	 *
	 * @param question
	 * @param successAction
	 */
	public static Button makeDecimalPrompt(final ItemCreator.ItemCreatorBuilder builder, final String question, final Consumer<Double> successAction) {
		return new Button() {

			@Override
			public ItemStack getItem() {
				return builder.build().make();
			}

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				SimpleDecimalPrompt.show(player, question, successAction);
			}
		};
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "{" + getItem().getType() + "}";
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
		public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
		}
	}
}
