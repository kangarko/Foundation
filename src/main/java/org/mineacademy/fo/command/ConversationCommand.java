package org.mineacademy.fo.command;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.settings.SimpleLocalization;

/**
 * A ready to use sub command enabling users to send conversation
 * input in case other plugins are blocking it and we are conversing
 * with the sender.
 *
 * A conversation means that we are waiting for player to type something
 * into the chat to process it. Such as the Boss plugin asks to type a Boss
 * name the player wants to create.
 */
public final class ConversationCommand extends SimpleSubCommand {

	public ConversationCommand() {
		super("conversation|conv");

		setDescription("Reply to a server's conversation manually.");
		setUsage("<message ...>");
		setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		checkConsole();
		checkBoolean(getPlayer().isConversing(), SimpleLocalization.Commands.CONVERSATION_NOT_CONVERSING);

		getPlayer().acceptConversationInput(Common.joinRange(0, args));
	}
}