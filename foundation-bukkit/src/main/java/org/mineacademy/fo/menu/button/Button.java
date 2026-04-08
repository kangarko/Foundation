package org.mineacademy.fo.menu.button;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

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
	 * The function that creates an info button with the given description
	 */
	@Setter
	private static Function<String[], Button> infoButtonCreator = description -> {
		final List<String> lores = new ArrayList<>();
		lores.add(" ");

		for (final String line : description)
			lores.add(line);

		return makeDummy(ItemCreator.fromMaterial(infoButtonMaterial).name(Lang.legacy("menu-button-info-name")).hideTags(true).lore(lores));
	};

	/**
	 * The slot of this button in the menu
	 */
	@Getter
	private int slot = -1;

	/**
	 * Create a new button with the given slot
	 *
	 * @param slot
	 */
	public Button(final int slot) {
		this.slot = slot;
	}

	/**
	 * Create a new button with no slot.
	 */
	public Button() {
	}

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
	 * @param description the description of the button
	 * @return the button
	 */
	public static final Button makeInfo(final String... description) {
		return infoButtonCreator.apply(description);
	}

	/**
	 * Create a new empty button (air)
	 *
	 * @return a new dummy air button
	 */
	public static final DummyButton makeEmpty() {
		return makeDummy(ItemCreator.fromMaterial(CompMaterial.AIR));
	}

	/**
	 * Creates a dummy button that does nothing when clicked
	 *
	 * @param material
	 * @param title
	 * @param lore
	 * @return
	 */
	public static final DummyButton makeDummy(final CompMaterial material, final String title, final String... lore) {
		return makeDummy(ItemCreator.fromMaterial(material).name(title).lore(lore));
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
				return ItemCreator.fromMaterial(icon).name(title).lore("").lore(label.split("\n")).makeMenuTool();
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
	public static final Button makeSimple(final ItemCreator builder, final Consumer<Player> onClickFunction) {
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
				return ItemCreator.from(icon, title, "", label).makeMenuTool();
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
	public static final Button makeBoolean(final ItemCreator creator, final Supplier<Boolean> getter, final Consumer<Boolean> setter) {
		final String menuTitle = creator.getName().toLowerCase();

		return new Button() {

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
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

				meta.setLore(Variables.builder().placeholder("status", has ? "§aEnabled" : "§cDisabled").replaceLegacyList(meta.getLore()));

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
	public static Button makeIntegerPrompt(final ItemCreator item, final String question, final RangedValue minMaxRange, final Supplier<Object> getter, final Consumer<Integer> setter) {
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
	public static Button makeIntegerPrompt(final ItemCreator item, final String question, final String menuTitle, final RangedValue minMaxRange, final Supplier<Object> getter, final Consumer<Integer> setter) {
		return new Button() {

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				new SimplePrompt() {

					@Override
					protected String getPrompt(final ConversationContext ctx) {
						return question.replace("{current}", getter.get().toString());
					}

					@Override
					protected boolean isInputValid(final ConversationContext context, final String input) {
						return ValidCore.isInteger(input) && ValidCore.isInRange(Integer.parseInt(input), minMaxRange.getMinLong(), minMaxRange.getMaxLong());
					}

					@Override
					protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
						return "Invalid input '" + invalidInput + "'! Enter a whole number from " + minMaxRange.getMinLong() + " to " + minMaxRange.getMaxLong() + ".";
					}

					@Override
					protected String getMenuAnimatedTitle() {
						return menuTitle != null ? "&9" + menuTitle.substring(0, 1).toUpperCase() + menuTitle.substring(1) + " set to " + getter.get() + "!" : null;
					}

					@Override
					protected Prompt acceptValidatedInput(final ConversationContext context, final String input) {
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
	public static Button makeDecimalPrompt(final ItemCreator item, final String question, final RangedValue minMaxRange, final Consumer<Double> setter) {
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
	public static Button makeDecimalPrompt(final ItemCreator item, final String question, final RangedValue minMaxRange, final Supplier<Object> getter, final Consumer<Double> setter) {
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
	public static Button makeDecimalPrompt(final ItemCreator item, final String question, final String menuTitle, final RangedValue minMaxRange, final Supplier<Object> getter, final Consumer<Double> setter) {
		return new Button() {

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				new SimplePrompt() {

					@Override
					protected String getPrompt(final ConversationContext ctx) {
						return question.replace("{current}", getter != null ? getter.get().toString() : "");
					}

					@Override
					protected boolean isInputValid(final ConversationContext context, final String input) {
						return ValidCore.isDecimal(input) && ValidCore.isInRange(Double.parseDouble(input), minMaxRange.getMinDouble(), minMaxRange.getMaxDouble());
					}

					@Override
					protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
						return "Invalid input '" + invalidInput + "'! Enter a whole number from " + minMaxRange.getMinDouble() + " to " + minMaxRange.getMaxDouble() + ".";
					}

					@Override
					protected String getMenuAnimatedTitle() {
						return menuTitle != null ? "&9" + menuTitle.substring(0, 1).toUpperCase() + menuTitle.substring(1) + " set to " + getter.get() + "!" : null;
					}

					@Override
					protected Prompt acceptValidatedInput(final ConversationContext context, final String input) {
						setter.accept(Double.parseDouble(input));

						return END_OF_CONVERSATION;
					}

				}.show(player);
			}

			@Override
			public ItemStack getItem() {
				final ItemStack itemstack = item.make();
				final ItemMeta meta = itemstack.getItemMeta();

				meta.setLore(Variables.builder().placeholder("current", getter != null ? getter.get().toString() : "").replaceLegacyList(meta.getLore()));
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
	public static Button makeStringPrompt(final ItemCreator creator, final String question, final Consumer<String> onPromptFinish) {
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
	public static Button makeStringPrompt(final ItemCreator creator, final String question, final String menuTitle, final Consumer<String> onPromptFinish) {
		return new Button() {

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				new SimpleStringPrompt(question) {

					@Override
					protected String getMenuAnimatedTitle() {
						return menuTitle;
					}

					@Override
					protected void onValidatedInput(final ConversationContext context, final String input) {
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
