package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.List;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.RemainCore;

import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

/**
 * Represents a chat message surrounded by chat-wide line on the top and bottom:
 * <p>
 * -----------------------------------
 * Hello this is a test!
 * -----------------------------------
 * <p>
 * You can also specify &lt;center&gt; in front of the text to center it.
 */
public final class BoxedMessage {

	/**
	 * The top and bottom line itself
	 */
	public static Component LINE = RemainCore.convertLegacyToAdventure("&8" + CommonCore.chatLineSmooth());

	/**
	 * All message recipients
	 */
	private final Iterable<Audience> recipients;

	/**
	 * The sender of the message
	 */
	private final Audience sender;

	/**
	 * The messages to send
	 */
	@Getter
	private final Component[] messages;

	/**
	 * Create a new boxed message from the given messages
	 * without sending it to any player
	 *
	 * @param messages
	 */
	public BoxedMessage(@NonNull Component... messages) {
		this(null, null, messages);
	}

	/**
	 * Create a new boxed message
	 *
	 * @param recipients
	 * @param sender
	 * @param messages
	 */
	private BoxedMessage(Iterable<Audience> recipients, Audience sender, @NonNull Component[] messages) {
		this.recipients = recipients == null ? null : CommonCore.toList(recipients); // Make a copy to prevent changes in the list on send
		this.sender = sender;
		this.messages = messages;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Helper methods
	// ------------------------------------------------------------------------------------------------------------

	private void launch() {
		Platform.runTask(2, () -> {
			final Component oldPrefix = CommonCore.getTellPrefix();
			CommonCore.setTellPrefix(Component.empty());

			this.sendFrame();

			CommonCore.setTellPrefix(oldPrefix);
		});
	}

	private void sendFrame() {
		this.send(LINE);

		for (int i = 0; i < this.getTopLines(); i++)
			this.send(Component.empty());

		for (final Component message : this.messages)
			this.send(message);

		for (int i = 0; i < this.getBottomLines(); i++)
			this.send(Component.empty());

		this.send(LINE);
	}

	private int getTopLines() {
		switch (this.length()) {
			case 1:
				return 2;
			case 2:
			case 3:
			case 4:
				return 1;
			default:
				return 0;
		}
	}

	private int getBottomLines() {
		switch (this.length()) {
			case 1:
			case 2:
				return 2;
			case 3:
				return 1;
			default:
				return 0;
		}
	}

	@SuppressWarnings("unused")
	private int length() {
		int length = 0;

		for (final Component message : this.messages)
			length++;

		return length;
	}

	private void send(Component message) {
		if (this.recipients == null)
			this.broadcast0(message);

		else
			this.tell0(message);
	}

	private void broadcast0(Component message) {
		if (this.sender != null)
			CommonCore.broadcast(message, this.sender);
		else
			CommonCore.broadcastTo(Platform.getOnlinePlayers(), message);
	}

	private void tell0(Component message) {
		if (this.sender != null)
			message = message.replaceText(b -> b.matchLiteral("{player}").replacement(Platform.resolveSenderName(this.sender)));

		CommonCore.broadcastTo(this.recipients, message);
	}

	@Override
	public String toString() {
		return "Boxed{" + String.join(", ", RemainCore.convertAdventureToPlain(Component.textOfChildren(this.messages)).split("\n")) + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Send this message to everyone
	 *
	 * @param messages
	 */
	public static void broadcast(String... messages) {
		final List<Component> converted = CommonCore.convert(messages, CommonCore::colorize);

		broadcast(null, converted.toArray(new Component[converted.size()]));
	}

	/**
	 * Send this message to everyone
	 *
	 * @param messages
	 */
	public static void broadcast(Component... messages) {
		broadcast(null, messages);
	}

	/**
	 * Sends this message to the all players as the sender
	 *
	 * @param sender
	 * @param messages
	 */
	public static void broadcast(Audience sender, String... messages) {
		final List<Component> converted = CommonCore.convert(messages, CommonCore::colorize);

		broadcast(sender, converted.toArray(new Component[converted.size()]));
	}

	/**
	 * Sends this message to the all players as the sender
	 *
	 * @param sender
	 * @param messages
	 */
	public static void broadcast(Audience sender, Component... messages) {
		new BoxedMessage(null, sender, messages).launch();
	}

	/**
	 * Sends the message to the recipient
	 *
	 * @param recipient
	 * @param messages
	 */
	public static void tell(Audience recipient, String... messages) {
		final List<Component> converted = CommonCore.convert(messages, CommonCore::colorize);

		tell(recipient, converted.toArray(new Component[converted.size()]));
	}

	/**
	 * Sends the message to the recipient
	 *
	 * @param recipient
	 * @param messages
	 */
	public static void tell(Audience recipient, Component... messages) {
		tell(null, Arrays.asList(recipient), messages);
	}

	/**
	 * Sends the message to the given recipients
	 *
	 * @param recipients
	 * @param messages
	 */
	public static void tell(Iterable<Audience> recipients, Component... messages) {
		tell(null, recipients, messages);
	}

	/**
	 * Sends this message to a recipient as sender
	 *
	 * @param sender
	 * @param receiver
	 * @param messages
	 */
	public static void tell(Audience sender, Audience receiver, Component... messages) {
		tell(sender, Arrays.asList(receiver), messages);
	}

	/**
	 * Sends this message to recipients as sender
	 *
	 * @param sender
	 * @param receivers
	 * @param messages
	 */
	public static void tell(Audience sender, Iterable<Audience> receivers, Component... messages) {
		new BoxedMessage(receivers, sender, messages).launch();
	}
}