package org.mineacademy.fo.menu.tool;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;

/**
 * Represents the tool registry holding all registered items
 *
 * The items are added here automatically upon calling the constructor of {@link Tool}
 */
public final class ToolRegistry {

	/**
	 * The registered tools
	 */
	private static final Collection<Tool> tools = new ConcurrentLinkedQueue<>();

	/**
	 * Add a new tool to register.
	 *
	 * Called automatically.
	 *
	 * @param tool the tool
	 */
	static synchronized void register(Tool tool) {
		Validate.isTrue(!isRegistered(tool), "Tool with itemstack " + tool.getItem() + " already registered");

		tools.add(tool);
	}

	/**
	 * Checks if the tool is registered
	 *
	 * @param tool the tool
	 * @return true if the tool is registered
	 */
	static synchronized boolean isRegistered(Tool tool) {
		return getTool(tool.getItem()) != null;
	}

	/**
	 * Attempts to find a registered tool from given itemstack
	 *
	 * @param item the item
	 * @return the corresponding tool, or null
	 */
	public static Tool getTool(ItemStack item) {
		for (final Tool t : tools)
			if (t.isTool(item))
				return t;

		return null;
	}

	/**
	 * Get all tools
	 *
	 * @return the registered tools array
	 */
	public static Tool[] getTools() {
		return tools.toArray(new Tool[tools.size()]);
	}
}
