package org.mineacademy.fo.conversation;

import java.util.Arrays;
import java.util.List;

import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationCanceller;
import org.bukkit.conversations.ConversationContext;
import org.mineacademy.fo.Valid;

/**
 * A simple conversation canceller
 * If the players message matches any word in the list, his conversation is cancelled
 */
public final class SimpleCanceller implements ConversationCanceller {

	/**
	 * The words that trigger the conversation cancellation
	 */
	private final List<String> cancelPhrases;

	/**
	 * Create a new convo canceler based off the given strings
	 * If the players message matches any word in the list, his conversation is cancelled
	 *
	 * @param cancelPhrases
	 */
	public SimpleCanceller(String... cancelPhrases) {
		this(Arrays.asList(cancelPhrases));
	}

	/**
	 * Create a new convo canceler from the given lists
	 * If the players message matches any word in the list, his conversation is cancelled
	 *
	 * @param cancelPhrases
	 */
	public SimpleCanceller(List<String> cancelPhrases) {
		Valid.checkBoolean(!cancelPhrases.isEmpty(), "Cancel phrases are empty for conversation cancel listener!");

		this.cancelPhrases = cancelPhrases;
	}

	@Override
	public void setConversation(Conversation conversation) {
	}

	/**
	 * Listen to cancel phrases and exit if they equals
	 */
	@Override
	public boolean cancelBasedOnInput(ConversationContext context, String input) {
		for (final String phrase : this.cancelPhrases)
			if (input.equalsIgnoreCase(phrase))
				return true;

		return false;
	}

	@Override
	public ConversationCanceller clone() {
		return new SimpleCanceller(this.cancelPhrases);
	}
}