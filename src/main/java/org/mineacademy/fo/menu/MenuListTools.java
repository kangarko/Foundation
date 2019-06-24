package org.mineacademy.fo.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.model.SimpleEnchant;
import org.mineacademy.fo.remain.CompItemFlag;

/**
 * A standardized menu to display a list of tools
 * player can toggle to get in his inventory
 */
public abstract class MenuListTools extends Menu {

	/**
	 * The list of tools
	 */
	private final List<ToggleableTool> tools;

	/**
	 * Make a new tools menu
	 */
	protected MenuListTools() {
		this(null);
	}

	/**
	 * Make a new tools menu with parent
	 *
	 * @param parent
	 */
	protected MenuListTools(Menu parent) {
		super(parent);

		setSize(9 * 1);
		setTitle("Tools Menu");

		this.tools = compile0(makeTools());
	}

	/**
	 * Attempts to automatically compile a set of tools
	 * Accepts an array containing {@link Button}, {@link ItemStack}
	 * or enter 0 for air.
	 *
	 * @deprecated unclear
	 * @return the array of items in this menu
	 */
	@Deprecated
	protected abstract Object[] makeTools();

	// Compiles the given tools from makeTools()
	private final List<ToggleableTool> compile0(Object... tools) {
		final List<ToggleableTool> list = new ArrayList<>();

		if (tools != null)
			for (final Object tool : tools)
				list.add(new ToggleableTool(tool));

		return list;
	}

	/**
	 * Returns the {@link #makeTools()} at their respective positions for each slot
	 *
	 * @param slot the slot
	 * @return the tool or null
	 */
	@Override
	public final ItemStack getItemAt(int slot) {
		return slot < tools.size() ? tools.get(slot).get(getViewer()) : null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onMenuClick(Player pl, int slot, InventoryAction action, ClickType click, ItemStack cursor, ItemStack item, boolean cancelled) {
		final ItemStack it = getItemAt(slot);
		final ToggleableTool tool = it != null ? findTool(it) : null;

		if (tool != null) {
			tool.giveOrTake(pl);

			redraw();
		}
	}

	// Converts the clicked item into a toggleable tool
	private final ToggleableTool findTool(ItemStack item) {
		for (final ToggleableTool h : tools)
			if (h.equals(item))
				return h;

		return null;
	}

	@Override
	protected int getInfoButtonPosition() {
		return 9 - 1;
	}
}

/**
 * Represents a tool that can be "toggled",
 * meaning the player can only have 1 of the tool
 * in their inventory that is either taken or given
 * on click.
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
	 * @param unparsed the object to parse, see {@link MenuListTools#makeTools()}
	 */
	ToggleableTool(Object unparsed) {
		if (unparsed != null) {
			if (unparsed instanceof ItemStack)
				this.item = (ItemStack) unparsed;

			else if (unparsed instanceof Tool)
				this.item = ((Tool) unparsed).getItem();

			else if (unparsed instanceof Number)
				this.item = new ItemStack(Material.AIR);

			else
				throw new FoException("Unknown HaveableItem: " + unparsed);

		} else
			this.item = new ItemStack(Material.AIR);
	}

	/**
	 * Returns the itemstack automatically, different if the player has or does not have it already
	 *
	 * @param player
	 * @return the item
	 */
	ItemStack get(Player player) {
		update(player);

		return playerHasTool ? getToolWhenHas() : getToolWhenHasnt();
	}

	private void update(Player pl) {
		playerHasTool = pl.getOpenInventory().getBottomInventory().containsAtLeast(item, 1);
	}

	// Return the dummy placeholder tool when the player already has it
	private ItemStack getToolWhenHas() {
		return ItemCreator
				.of(item)
				.enchant(new SimpleEnchant(Enchantment.ARROW_INFINITE, 1))
				.flag(CompItemFlag.HIDE_ENCHANTS)
				.lores(Arrays.asList("", "&cYou already have this item.", "&7Click to take it away."))
				.build().make();
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
	void giveOrTake(Player player) {
		final PlayerInventory inv = player.getInventory();

		if (playerHasTool = !playerHasTool)
			inv.addItem(item);

		else
			inv.removeItem(item);
	}

	boolean equals(ItemStack item) {
		return getToolWhenHas().isSimilar(item) || getToolWhenHasnt().isSimilar(item);
	}
}
