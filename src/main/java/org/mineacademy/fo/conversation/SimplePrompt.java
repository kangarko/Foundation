package org.mineacademy.fo.conversation;

import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.Menu;

/**
 * Represents one question for the player during a server conversation
 */
public abstract class SimplePrompt extends ValidatingPrompt {

	/**
	 * Open the players menu back if any?
	 */
	private boolean openMenu = true;

	protected SimplePrompt() {
	}

	/**
	 * Create a new prompt, show we open players menu back if he has any?
	 *
	 * @param openMenu
	 */
	protected SimplePrompt(boolean openMenu) {
		this.openMenu = openMenu;
	}

	/**
	 * Return the prefix before tell messages
	 *
	 * @param ctx
	 * @return
	 */
	protected String getCustomPrefix() {
		return null;
	}

	/**
	 * Return the question, implemented in own way using colors
	 *
	 * @param
	 * @return
	 */
	@Override
	public final String getPromptText(ConversationContext ctx) {
		return Common.colorize(getPrompt(ctx));
	}

	/**
	 * Return the question to the user in this prompt
	 *
	 * @param ctx
	 * @return
	 */
	protected abstract String getPrompt(ConversationContext ctx);

	/**
	 * Checks if the input from the user was valid, if it was, we can continue to the next prompt
	 *
	 * @param context
	 * @param input
	 *
	 * @return
	 */
	@Override
	protected boolean isInputValid(ConversationContext context, String input) {
		return true;
	}

	/**
	 * Return the failed error message when {@link #isInputValid(ConversationContext, String)} returns false
	 */
	@Override
	protected String getFailedValidationText(ConversationContext context, String invalidInput) {
		return null;
	}

	/**
	 * Converts the {@link ConversationContext} into a {@link Player}
	 * or throws an error if it is not a player
	 *
	 * @param ctx
	 * @return
	 */
	protected Player getPlayer(ConversationContext ctx) {
		Valid.checkBoolean(ctx.getForWhom() instanceof Player, "Conversable is not a player but: " + ctx.getForWhom());

		return (Player) ctx.getForWhom();
	}

	/**
	 * Send the player (in case any) the given message
	 *
	 * @param ctx
	 * @param message
	 */
	protected void tell(ConversationContext ctx, String message) {
		tell(getPlayer(ctx), message);
	}

	/**
	 * Sends the message to the player
	 *
	 * @param conversable
	 * @param message
	 */
	protected void tell(Conversable conversable, String message) {
		Common.tellConversing(conversable, (getCustomPrefix() != null ? getCustomPrefix() : "") + message);
	}

	/**
	 * Called when the whole conversation is over. This is called before {@link SimpleConversation#onConversationEnd(ConversationAbandonedEvent)}
	 *
	 * @param conversation
	 * @param event
	 */
	public void onConversationEnd(SimpleConversation conversation, ConversationAbandonedEvent event) {
	}

	// Do not allow superclasses to modify this since we hve isInputValid here
	@Override
	public final Prompt acceptInput(ConversationContext context, String input) {
		if (isInputValid(context, input))
			return acceptValidatedInput(context, input);

		else {
			final String failPrompt = getFailedValidationText(context, input);

			if (failPrompt != null)
				tell(context, "&c" + failPrompt);

			// Redisplay this prompt to the user to re-collect input
			return this;
		}
	}

	/**
	 * Shows this prompt as a conversation to the player
	 *
	 * NB: Do not call this as a means to showing this prompt DURING AN EXISTING
	 * conversation as it will fail! Use {@link #acceptValidatedInput(ConversationContext, String)} instead
	 * to show the next prompt
	 *
	 * @param player
	 */
	public final SimpleConversation show(Player player) {
		Valid.checkBoolean(!player.isConversing(), "Player " + player.getName() + " is already conversing! Show them their next prompt in acceptValidatedInput() in " + getClass().getSimpleName() + " instead!");

		final SimpleConversation conversation = new SimpleConversation() {

			@Override
			protected Prompt getFirstPrompt() {
				return SimplePrompt.this;
			}

			@Override
			protected ConversationPrefix getPrefix() {
				final String prefix = SimplePrompt.this.getCustomPrefix();

				return prefix != null ? new SimplePrefix(prefix) : super.getPrefix();
			}
		};

		if (openMenu) {
			final Menu menu = Menu.getMenu(player);

			if (menu != null)
				conversation.setMenuToReturnTo(menu);
		}

		conversation.start(player);

		return conversation;
	}
}
