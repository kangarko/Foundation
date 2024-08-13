package org.mineacademy.fo.model;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public final class SimpleComponent extends SimpleComponentCore {

	private SimpleComponent(String text, boolean colorize) {
		super(text, colorize);
	}

	private SimpleComponent(Component component) {
		super(component);
	}

	private SimpleComponent(SerializedMap map) {
		super(map);
	}

	/**
	 * Shows the item on hover if it is not air.
	 * <p>
	 * NB: Some colors from lore may get lost as a result of Minecraft/Spigot bug.
	 *
	 * @param item
	 * @return
	 */
	public SimpleComponentCore onHover(@NonNull final ItemStack item) {
		if (CompMaterial.isAir(item.getType()))
			return this.onHover("Air");

		try {
			this.modifyLastComponent(component -> component.hoverEvent(Remain.convertItemStackToHoverEvent(item)));

		} catch (final Throwable t) {
			CommonCore.logFramed(
					"Error parsing ItemStack to simple component!",
					"Item: " + item,
					"Error: " + t.getMessage());

			t.printStackTrace();
		}

		return this;
	}

	@Override
	protected Component onBuild(Audience sender, Audience receiver, Component component) {
		return HookManager.replaceRelationPlaceholders(sender, receiver, component);
	}

	// --------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @return
	 */
	public static SimpleComponentCore empty() {
		return of(false, "");
	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @param text
	 * @return
	 */
	public static SimpleComponentCore of(final String text) {
		return of(true, text);
	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @param colorize
	 * @param text
	 * @return
	 */
	public static SimpleComponent of(boolean colorize, String text) {
		return new SimpleComponent(text, colorize);
	}

	/**
	 * Create a new interactive chat component
	 *
	 * @param component
	 * @return
	 */
	public static SimpleComponent of(Component component) {
		return new SimpleComponent(component);
	}

	/**
	 *
	 * @param json
	 * @return
	 */
	public static SimpleComponent fromJson(String json) {
		return deserialize(SerializedMap.fromJson(json));
	}

	/**
	 *
	 * @param map
	 * @return
	 */
	public static SimpleComponent deserialize(SerializedMap map) {
		return new SimpleComponent(map);
	}
}
