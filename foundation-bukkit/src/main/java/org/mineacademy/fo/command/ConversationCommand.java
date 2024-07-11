package org.mineacademy.fo.command;

import java.util.List;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.settings.Lang;

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

	/**
	 * Create a new sub-command with the "conversation" and "convo" aliases registered in your
	 * {@link FoundationPlugin#getDefaultCommandGroup()} command group.
	 */
	public ConversationCommand() {
		this("conversation|convo");
	}

	/**
	 * Create a new sub-command with the given label registered in your
	 * {@link FoundationPlugin#getDefaultCommandGroup()} command group.
	 *
	 * @param label
	 */
	public ConversationCommand(String label) {
		super(label);

		this.setProperties();
	}

	/**
	 * Create a new sub-command with the "conversation" and "convo" aliases registered in the given command group.
	 *
	 * @param group
	 */
	public ConversationCommand(SimpleCommandGroup group) {
		this(group, "conversation|convo");
	}

	/**
	 * Create a new sub-command with the given label registered in the given command group.
	 *
	 * @param group
	 * @param label
	 */
	public ConversationCommand(SimpleCommandGroup group, String label) {
		super(group, label);

		this.setProperties();
	}

	/*
	 * Set the properties for this command
	 */
	private void setProperties() {
		this.setDescription("Reply to a server's conversation manually.");
		this.setUsage("<message ...>");
		this.setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		this.checkConsole();
		this.checkBoolean(this.getPlayer().isConversing(), Lang.component("conversation-not-conversing"));

		this.getPlayer().acceptConversationInput(CommonCore.joinRange(0, this.args));
	}

	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}