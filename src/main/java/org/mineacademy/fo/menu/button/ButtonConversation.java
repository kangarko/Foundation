package org.mineacademy.fo.menu.button;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleConversation;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.menu.AdvancedMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

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
	 * Convenience shortcut for {@link ItemCreator#of(CompMaterial, String, String...)}
	 *
	 * @param convo
	 * @param material
	 * @param title
	 * @param lore
	 */
	public ButtonConversation(SimpleConversation convo, CompMaterial material, String title, String... lore) {
		this(convo, ItemCreator.of(material, title, lore));
	}

	/**
	 * Create a new button that starts a server conversation when clicked
	 *
	 * @param convo
	 * @param item
	 */
	public ButtonConversation(SimpleConversation convo, ItemCreator.ItemCreatorBuilder item) {
		this(convo, null, item.hideTags(true).build().make());
	}

	/**
	 * Create a new button that starts a server conversation when clicked
	 *
	 * @param convo
	 * @param item
	 */
	public ButtonConversation(SimpleConversation convo, ItemCreator item) {
		this(convo, null, item.make());
	}

	/**
	 * Convenience shortcut for {@link ItemCreator#of(CompMaterial, String, String...)}
	 *
	 * @param prompt
	 * @param material
	 * @param title
	 * @param lore
	 */
	public ButtonConversation(SimplePrompt prompt, CompMaterial material, String title, String... lore) {
		this(prompt, ItemCreator.of(material, title, lore));
	}

	/**
	 * Create a new conversation from a single prompt
	 *
	 * @param prompt
	 * @param item
	 */
	public ButtonConversation(SimplePrompt prompt, ItemCreator item) {
		this(null, prompt, item.make());
	}

	/**
	 * Create a new conversation from a single prompt
	 *
	 * @param prompt
	 * @param item
	 */
	public ButtonConversation(SimplePrompt prompt, ItemCreator.ItemCreatorBuilder item) {
		this(null, prompt, item.hideTags(true).build().make());
	}

	private ButtonConversation(SimpleConversation conversation, SimplePrompt prompt, ItemStack item) {
		this.conversation = conversation;
		this.prompt = prompt;
		this.item = item;
	}

	@Override
	public void onClickedInMenu(Player player, AdvancedMenu menu, ClickType click) {
		Valid.checkBoolean(conversation != null || prompt != null, "Conversation and prompt cannot be null!");

		if (conversation != null) {
			conversation.setMenuToReturnTo(menu);

			conversation.start(player);

		} else
			prompt.show(player);

	}
}