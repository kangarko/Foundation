package org.mineacademy.fo.menu.button;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.conversation.SimpleConversation;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * A button that runs a server conversation
 */
public final class ButtonConversation extends Button {

	/**
	 * The server conversation to launch, or null if {@link SimplePrompt} is set
	 */
	private final SimpleConversation conversation;

	/**
	 * The prompt to show, null if {@link #conversation} is set
	 */
	private final SimplePrompt prompt;

	/**
	 * The item representing this button
	 */
	@Getter
	private final ItemStack item;

	/**
	 * Convenience shortcut for {@link ItemCreator#from(CompMaterial, String, String...)}
	 *
	 * @param convo
	 * @param material
	 * @param title
	 * @param lore
	 */
	public ButtonConversation(final SimpleConversation convo, final CompMaterial material, final String title, final String... lore) {
		this(convo, ItemCreator.from(material, title, lore));
	}

	/**
	 * Create a new button that starts a server conversation when clicked
	 *
	 * @param convo
	 * @param item
	 */
	public ButtonConversation(final SimpleConversation convo, final ItemCreator item) {
		this(convo, null, item.make());
	}

	/**
	 * Convenience shortcut for {@link ItemCreator#from(CompMaterial, String, String...)}
	 *
	 * @param prompt
	 * @param material
	 * @param title
	 * @param lore
	 */
	public ButtonConversation(final SimplePrompt prompt, final CompMaterial material, final String title, final String... lore) {
		this(prompt, ItemCreator.from(material, title, lore));
	}

	/**
	 * Create a new conversation from a single prompt
	 *
	 * @param prompt
	 * @param item
	 */
	public ButtonConversation(final SimplePrompt prompt, final ItemCreator item) {
		this(null, prompt, item.hideTags(true).make());
	}

	private ButtonConversation(final SimpleConversation conversation, final SimplePrompt prompt, final ItemStack item) {
		this.conversation = conversation;
		this.prompt = prompt;
		this.item = item;
	}

	@Override
	public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
		ValidCore.checkBoolean(this.conversation != null || this.prompt != null, "Conversation and prompt cannot be null!");

		if (this.conversation != null) {
			this.conversation.setMenuToReturnTo(menu);

			this.conversation.start(player);

		} else
			this.prompt.show(player);

	}
}