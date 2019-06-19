package org.mineacademy.fo.conversation;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * A simple conversation prefix with a static string
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class SimplePrefix implements ConversationPrefix {

	/**
	 * The conversation prefix
	 */
	private final String prefix;

	@Override
	public String getPrefix(ConversationContext context) {
		return prefix;
	}
}