package org.mineacademy.fo.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;

/**
 * A standardized menu to display a list of tools player can toggle to get in
 * his inventory
 */
public abstract class MenuTools extends Menu {

	/**
	 * The list of tools
	 */
	private final List<ToggleableTool> tools;

	/**
	 * Make a new tools menu
	 */
	protected MenuTools() {
		this(null);
	}

	/**
	 * Make a new tools menu with parent
	 *
	 * @param parent
	 */
	protected MenuTools(final Menu parent) {
		super(parent);

		this.tools = compile0(compileTools());

		final int items = tools.size();
		final int pages = items < 9 ? 9 * 1 : items < 9 * 2 ? 9 * 2 : items < 9 * 3 ? 9 * 3 : items < 9 * 4 ? 9 * 4 : 9 * 5;

		setSize(pages);
		setTitle(SimpleLocalization.Menu.TITLE_TOOLS);
	}

	/**
	 * Attempts to automatically compile a set of tools Accepts an array containing
	 * {@link Button}, {@link ItemStack} or enter 0 for air.
	 *
	 * @return the array of items in this menu
	 */
	protected abstract Object[] compileTools();

	/**
	 * Helper method you can use directly in your {@link #compileTools()} method
	 * that will automatically scan all classes in your plugin that extend the given
	 * class and return those who contain the given field:
	 * <p>
	 * public static Tool instance = new X() (X = the class)
	 *
	 * @param extendingClass
	 * @return
	 */
	protected Object[] lookupTools(final Class<? extends Tool> extendingClass) {
		final List<Object> instances = new ArrayList<>();

		for (final Class<?> clazz : ReflectionUtil.getClasses(SimplePlugin.getInstance(), extendingClass))
			try {
				final Object instance = ReflectionUtil.getFieldContent(clazz, "instance", null);

				instances.add(instance);

			} catch (final Throwable ex) {
				// continue, unsupported tool. It must have an "instance" static
				// field with its instance
			}

		return instances.toArray();
	}

	// Compiles the given tools from makeTools()
	private final List<ToggleableTool> compile0(final Object... tools) {
		final List<ToggleableTool> list = new ArrayList<>();

		if (tools != null)
			for (final Object tool : tools)
				list.add(new ToggleableTool(tool));

		return list;
	}

	/**
	 * Returns the compileTools() at their respective positions for each
	 * slot
	 *
	 * @param slot the slot
	 * @return the tool or null
	 */
	@Override
	public final ItemStack getItemAt(final int slot) {
		return slot < tools.size() ? tools.get(slot).get(getViewer()) : null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onMenuClick(final Player pl, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack item, final boolean cancelled) {
		final ItemStack it = getItemAt(slot);
		final ToggleableTool tool = it != null ? findTool(it) : null;

		if (tool != null) {
			tool.giveOrTake(pl);

			restartMenu();
		}
	}

	// Converts the clicked item into a toggleable tool
	private final ToggleableTool findTool(final ItemStack item) {
		for (final ToggleableTool h : tools)
			if (h.equals(item))
				return h;

		return null;
	}

	@Override
	protected int getInfoButtonPosition() {
		return getSize() - 1;
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return null;
	}

	/**
	 * Compiles an automated tools menu.
	 *
	 * @param pluginToolClasses We will scan your plugin for this kind of class and
	 *                          all classes extending it will be loaded into the
	 *                          menu
	 * @param description       the menu description
	 * @return
	 */
	public static final MenuTools of(final Class<? extends Tool> pluginToolClasses, final String... description) {
		return new MenuTools() {

			@Override
			protected Object[] compileTools() {
				return lookupTools(pluginToolClasses);
			}

			@Override
			protected String[] getInfo() {
				return description;
			}
		};
	}
}

/**
 * Represents a tool that can be "toggled", meaning the player can only have 1
 * of the tool in their inventory that is either taken or given on click.
 */
final class ToggleableTool {

	/**
	 * The item representation
	 */
	private final ItemStack item;

	/**
	 * Internal flag representing if the player had the tool, since we last checked
	 */
	private boolean playerHasTool = false;

	/**
	 * Create a new tool
	 *
	 * @param unparsed the object to parse, see {@link MenuTools#compileTools()}
	 */
	ToggleableTool(final Object unparsed) {

		if (unparsed != null) {
			if (unparsed instanceof ItemStack)
				this.item = (ItemStack) unparsed;

			else if (unparsed instanceof Tool)
				this.item = ((Tool) unparsed).getItem();

			else if (unparsed instanceof Number && ((Number) unparsed).intValue() == 0)
				this.item = new ItemStack(Material.AIR);

			else if (unparsed instanceof Class && Tool.class.isAssignableFrom((Class<?>) unparsed))
				this.item = ((Tool) ReflectionUtil.invokeStatic((Class<?>) unparsed, "getInstance")).getItem();

			else
				throw new FoException("Unknown tool: " + unparsed + " (we only accept ItemStack, Tool's instance or 0 for air)");

		} else
			this.item = new ItemStack(Material.AIR);
	}

	/**
	 * Returns the itemstack automatically, different if the player has or does not
	 * have it already
	 *
	 * @param player
	 * @return the item
	 */
	ItemStack get(final Player player) {
		update(player);

		return playerHasTool ? getToolWhenHas() : getToolWhenHasnt();
	}

	private void update(final Player pl) {
		playerHasTool = pl.getOpenInventory().getBottomInventory().containsAtLeast(item, 1);
	}

	// Return the dummy placeholder tool when the player already has it
	private ItemStack getToolWhenHas() {
		return ItemCreator
				.of(item)
				.glow(true)
				.lore("", "&6You already have this item.", "&6Click to take it away.")
				.makeMenuTool();
	}

	// Return the actual working tool in case player does not have it yet
	private ItemStack getToolWhenHasnt() {
		return item;
	}

	/**
	 * Gives or takes the tool for the player depending on {@link #playerHasTool}
	 *
	 * @param player the player
	 */
	void giveOrTake(final Player player) {
		final PlayerInventory inv = player.getInventory();

		if (playerHasTool = !playerHasTool)
			inv.addItem(item);

		else
			inv.removeItem(item);
	}

	boolean equals(final ItemStack item) {
		return getToolWhenHas().isSimilar(item) || getToolWhenHasnt().isSimilar(item);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Toggleable{" + item.getType() + "}";
	}
}
