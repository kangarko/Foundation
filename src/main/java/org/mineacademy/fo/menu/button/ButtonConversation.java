package org.mineacademy.fo.menu.button;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.conversation.SimpleConversation;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A button that runs a server conversation
 */
@RequiredArgsConstructor
public final class ButtonConversation extends Button {

	/**
	 * The server conversation to launch
	 */
	private final SimpleConversation convo;

	/**
	 * The item representing this button
	 */
	@Getter
	private final ItemStack item;

	/**
	 * Create a new button that starts a server conversation when clicked
	 *
	 * @param convo
	 * @param item
	 */
	public ButtonConversation(SimpleConversation convo, ItemCreator.ItemCreatorBuilder item) {
		this(convo, item.hideTags(true).build().make());
	}

	/**
	 * Create a new button that starts a server conversation when clicked
	 *
	 * @param convo
	 * @param item
	 */
	public ButtonConversation(SimpleConversation convo, ItemCreator item) {
		this(convo, item.make());
	}

	@Override
	public void onClickedInMenu(Player pl, Menu menu, ClickType click) {
		convo.start(pl);
	}
}