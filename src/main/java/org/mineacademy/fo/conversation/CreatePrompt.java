package org.mineacademy.fo.conversation;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Valid;

/**
 * Represents a simple prompt used in menus to create new objects.
 *
 * @param <T>
 */
public abstract class CreatePrompt<T> extends SimplePrompt {

	/**
	 * A valid name pattern.
	 */
	private static final Pattern ENGLISH_ONLY_PATTERN = Pattern.compile("^[a-zA-Z0-9_ ]+$");

	/**
	 * What type of object are we creating? Such as "region, "Boss" etc. This is NOT the actual name.
	 */
	private final String objectName;

	/**
	 * The name of the Boss, the name of the region etc.
	 */
	private String name;

	/**
	 * Create a new prompt
	 *
	 * @param objectName
	 */
	protected CreatePrompt(String objectName) {
		super(false);

		this.objectName = objectName;
	}

	/* ------------------------------------------------------------------------------- */
	/* Core functions */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Create object from the given name.
	 *
	 * @param name
	 * @return
	 */
	protected abstract T create(String name);

	/**
	 * Return the existing objects name by name, used to prevent duplicate objects
	 *
	 * @param name
	 * @return
	 */
	protected abstract String findByName(String name);

	/**
	 * Display menu to player
	 *
	 * @param player
	 * @param createdItem
	 */
	protected abstract void onCreateFinish(Player player, T createdItem);

	/**
	 * Allow spaces in names?
	 *
	 * @return
	 */
	protected boolean allowSpaces() {
		return false;
	}

	/* ------------------------------------------------------------------------------- */
	/* Final methods */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see org.mineacademy.fo.conversation.SimplePrompt#getPrompt(org.bukkit.conversations.ConversationContext)
	 */
	@Override
	protected final String getPrompt(ConversationContext context) {
		return "Please type your " + this.objectName + " name to chat to create it. Use English only alphabet.";
	}

	/**
	 * @see org.mineacademy.fo.conversation.SimplePrompt#isInputValid(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected final boolean isInputValid(ConversationContext context, String input) {
		if (input.contains(" ") && !this.allowSpaces())
			return false;

		return ENGLISH_ONLY_PATTERN.matcher(input).matches() && input.length() >= 3 && input.length() <= 24 && this.findByName(input) == null;
	}

	/**
	 * @see org.mineacademy.fo.conversation.SimplePrompt#getFailedValidationText(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected final String getFailedValidationText(ConversationContext context, String invalidInput) {

		@Nullable
		final String existing = this.findByName(invalidInput);
		final String name = ChatUtil.capitalize(this.objectName);

		if (existing != null)
			return name + " named '" + existing + "' already exists!";

		if (invalidInput.length() < 3)
			return name + " name must be at least 3 letters long!";

		if (invalidInput.length() > 24)
			return name + " name cannot be longer than 24 letters!";

		return name + " name contains invalid letters! Use English only alphabet without spaces.";
	}

	/**
	 * @see org.bukkit.conversations.ValidatingPrompt#acceptValidatedInput(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected final Prompt acceptValidatedInput(ConversationContext context, String input) {
		this.name = input;

		return END_OF_CONVERSATION;
	}

	/**
	 * @see org.mineacademy.fo.conversation.SimplePrompt#onConversationEnd(org.mineacademy.fo.conversation.SimpleConversation, org.bukkit.conversations.ConversationAbandonedEvent)
	 */
	@Override
	public final void onConversationEnd(SimpleConversation conversation, ConversationAbandonedEvent event) {
		if (event.gracefulExit()) {
			Valid.checkNotNull(this.name, "Prompt failed to carry " + this.objectName + " name");

			this.onCreateFinish(this.getPlayer(event.getContext()), this.create(this.name));
		}
	}
}
