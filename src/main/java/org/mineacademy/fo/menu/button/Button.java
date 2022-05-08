package org.mineacademy.fo.menu.button;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.Replacer;
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

		return makeDummy(ItemCreator.of(infoButtonMaterial).name(infoButtonTitle).hideTags(true).lore(lores));
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
	 * @param material
	 * @param title
	 * @param lore
	 * @return
	 */
	public static final DummyButton makeDummy(final CompMaterial material, String title, String... lore) {
		return makeDummy(ItemCreator.of(material).name(title).lore(lore));
	}

	/**
	 * Creates a dummy button that does nothing when clicked
	 *
	 * @param creator the icon creator
	 * @return the button
	 */
	public static final DummyButton makeDummy(final ItemCreator creator) {
		return makeDummy(creator.makeMenuTool());
	}

	/**
	 * Creates a dummy button that does nothing when clicked
	 *
	 * @param item the item
	 * @return the button
	 */
	public static final DummyButton makeDummy(final ItemStack item) {
		return new DummyButton(item);
	}

	/**
	 * Creates a lazy button having the given icon, title, label (the second lore row) and the click function
	 * taking in the player who damn clicked
	 *
	 * IMPORTANT: Changing the icon won't work when calling {@link Menu#restartMenu()}, you must create
	 * an anonymous {@link Button} class for that to work.
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
				return ItemCreator.of(icon).name(title).lore("").lore(label.split("\n")).makeMenuTool();
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
	 * IMPORTANT: Changing the icon won't work when calling {@link Menu#restartMenu()}, you must create
	 * an anonymous {@link Button} class for that to work.
	 *
	 * @param builder
	 * @param onClickFunction
	 * @return
	 */
	public static final Button makeSimple(ItemCreator builder, final Consumer<Player> onClickFunction) {
		return new Button() {

			@Override
			public ItemStack getItem() {
				return builder.makeMenuTool();
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
	 * IMPORTANT: Changing the icon won't work when calling {@link Menu#restartMenu()}, you must create
	 * an anonymous {@link Button} class for that to work.
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
				return ItemCreator.of(icon, title, "", label).makeMenuTool();
			}

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				onClickFunction.accept(player, click);
			}
		};
	}

	/**
	 * Creates a functional button that will toggle on/off state, typically
	 * used to toggle a file setting, such as a Boss dropping items or not, etc.
	 *
	 * @param creator
	 * @param getter
	 * @param setter
	 * @return
	 */
	public static final Button makeBoolean(ItemCreator creator, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		final String menuTitle = creator.getName().toLowerCase();

		return new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				final boolean has = getter.get();

				setter.accept(!has);

				final Menu newMenu = menu.newInstance();

				newMenu.displayTo(player);
				newMenu.restartMenu((has ? "&4Disabled" : "&2Enabled") + " " + menuTitle + "!");
			}

			@Override
			public ItemStack getItem() {
				final boolean has = getter.get();
				final ItemStack item = creator.glow(has).make();
				final ItemMeta meta = item.getItemMeta();

				meta.setLore(Replacer.replaceArray(meta.getLore(), "status", has ? "&aEnabled" : "&cDisabled"));
				item.setItemMeta(meta);

				return item;
			}
		};
	}

	/**
	 * A convenience method for creating integer prompts
	 *
	 * @param item
	 * @param question
	 * @param minMaxRange
	 * @param getter
	 * @param setter
	 * @return
	 */
	public static Button makeIntegerPrompt(ItemCreator item, String question, RangedValue minMaxRange, Supplier<Object> getter, Consumer<Integer> setter) {
		return makeIntegerPrompt(item, question, null, minMaxRange, getter, setter);
	}

	/**
	 * A convenience method for creating integer prompts
	 *
	 * @param item
	 * @param question
	 * @param menuTitle
	 * @param minMaxRange
	 * @param getter
	 * @param setter
	 * @return
	 */
	public static Button makeIntegerPrompt(ItemCreator item, String question, String menuTitle, RangedValue minMaxRange, Supplier<Object> getter, Consumer<Integer> setter) {
		return new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				new SimplePrompt() {

					@Override
					protected String getPrompt(ConversationContext ctx) {
						return question.replace("{current}", getter.get().toString());
					}

					@Override
					protected boolean isInputValid(ConversationContext context, String input) {
						return Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), minMaxRange.getMinLong(), minMaxRange.getMaxLong());
					}

					@Override
					protected String getFailedValidationText(ConversationContext context, String invalidInput) {
						return "Invalid input '" + invalidInput + "'! Enter a whole number from " + minMaxRange.getMinLong() + " to " + minMaxRange.getMaxLong() + ".";
					}

					@Override
					protected String getMenuAnimatedTitle() {
						return menuTitle != null ? "&9" + menuTitle.substring(0, 1).toUpperCase() + menuTitle.substring(1) + " set to " + getter.get() + "!" : null;
					}

					@Override
					protected Prompt acceptValidatedInput(ConversationContext context, String input) {
						setter.accept(Integer.parseInt(input));

						return END_OF_CONVERSATION;
					}

				}.show(player);
			}

			@Override
			public ItemStack getItem() {
				return item.make();
			}
		};
	}

	/**
	 * A convenience method for creating decimal prompts
	 *
	 * @param item
	 * @param question
	 * @param minMaxRange
	 * @param setter
	 * @return
	 */
	public static Button makeDecimalPrompt(ItemCreator item, String question, RangedValue minMaxRange, Consumer<Double> setter) {
		return makeDecimalPrompt(item, question, minMaxRange, null, setter);
	}

	/**
	 * A convenience method for creating decimal prompts
	 *
	 * @param item
	 * @param question
	 * @param minMaxRange
	 * @param getter
	 * @param setter
	 * @return
	 */
	public static Button makeDecimalPrompt(ItemCreator item, String question, RangedValue minMaxRange, Supplier<Object> getter, Consumer<Double> setter) {
		return makeDecimalPrompt(item, question, null, minMaxRange, getter, setter);
	}

	/**
	 * A convenience method for creating decimal prompts
	 *
	 * @param item
	 * @param question
	 * @param menuTitle
	 * @param minMaxRange
	 * @param getter
	 * @param setter
	 * @return
	 */
	public static Button makeDecimalPrompt(ItemCreator item, String question, String menuTitle, RangedValue minMaxRange, @Nullable Supplier<Object> getter, Consumer<Double> setter) {
		return new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				new SimplePrompt() {

					@Override
					protected String getPrompt(ConversationContext ctx) {
						return question.replace("{current}", getter != null ? getter.get().toString() : "");
					}

					@Override
					protected boolean isInputValid(ConversationContext context, String input) {
						return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), minMaxRange.getMinDouble(), minMaxRange.getMaxDouble());
					}

					@Override
					protected String getFailedValidationText(ConversationContext context, String invalidInput) {
						return "Invalid input '" + invalidInput + "'! Enter a whole number from " + minMaxRange.getMinDouble() + " to " + minMaxRange.getMaxDouble() + ".";
					}

					@Override
					protected String getMenuAnimatedTitle() {
						return menuTitle != null ? "&9" + menuTitle.substring(0, 1).toUpperCase() + menuTitle.substring(1) + " set to " + getter.get() + "!" : null;
					}

					@Override
					protected Prompt acceptValidatedInput(ConversationContext context, String input) {
						setter.accept(Double.parseDouble(input));

						return END_OF_CONVERSATION;
					}

				}.show(player);
			}

			@Override
			public ItemStack getItem() {
				final ItemStack itemstack = item.make();
				final ItemMeta meta = itemstack.getItemMeta();

				meta.setLore(Replacer.replaceArray(meta.getLore(), "current", getter != null ? getter.get().toString() : ""));
				itemstack.setItemMeta(meta);

				return itemstack;
			}
		};
	}

	/**
	 * A convenience method for creating string prompts
	 *
	 * @param creator
	 * @param question
	 * @param onPromptFinish
	 * @return
	 */
	public static Button makeStringPrompt(ItemCreator creator, String question, Consumer<String> onPromptFinish) {
		return makeStringPrompt(creator, question, null, onPromptFinish);
	}

	/**
	 * A convenience method for creating string prompts
	 *
	 * @param creator
	 * @param question
	 * @param menuTitle
	 * @param onPromptFinish
	 * @return
	 */
	public static Button makeStringPrompt(ItemCreator creator, String question, @Nullable String menuTitle, Consumer<String> onPromptFinish) {
		return new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				new SimpleStringPrompt(question) {

					@Override
					protected String getMenuAnimatedTitle() {
						return menuTitle;
					}

					@Override
					protected void onValidatedInput(ConversationContext context, String input) {
						onPromptFinish.accept(input);
					}

				}.show(player);
			}

			@Override
			public ItemStack getItem() {
				return creator.make();
			}
		};
	}

	@Override
	public final String toString() {
		final ItemStack item = this.getItem();

		return this.getClass().getSimpleName() + "{" + (item != null ? item.getType() : "null") + "}";
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
